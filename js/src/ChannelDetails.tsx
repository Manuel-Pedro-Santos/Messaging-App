import * as React from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";


interface Channel {
    id: number;
    owner: string;
    name: string;
    type: string;
    controls?: string;
    users?: string[];
}

interface Message {
    id: number;
    text: string;
    sender: string;
    timestamp: string;
}

type State = {
    channel: Channel | null;
    //isUserInChannel: boolean;
    searchName: string;
    foundUserId: number | null;
    ownerId: number | null;
    messages: Message[];
    newMessage: string;
};

type Action =
    | { type: "SET_CHANNEL"; payload: Channel }
    | { type: "SET_IS_USER_IN_CHANNEL"; payload: boolean }
    | { type: "SET_SEARCH_NAME"; payload: string }
    | { type: "SET_FOUND_USER_ID"; payload: number | null }
    | { type: "SET_OWNER_ID"; payload: number | null }
    | { type: "SET_MESSAGES"; payload: Message[] }
    | { type: "ADD_MESSAGE"; payload: Message }
    | { type: "SET_NEW_MESSAGE"; payload: string };

const initialState: State = {
    channel: null,
    searchName: "",
    foundUserId: null,
    ownerId: null,
    messages: [],
    newMessage: "",
};

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "SET_CHANNEL":
            return { ...state, channel: action.payload };
        case "SET_SEARCH_NAME":
            return { ...state, searchName: action.payload };
        case "SET_FOUND_USER_ID":
            return { ...state, foundUserId: action.payload };
        case "SET_OWNER_ID":
            return { ...state, ownerId: action.payload };
        case "SET_MESSAGES":
            return { ...state, messages: action.payload };
        case "ADD_MESSAGE":
            return { ...state, messages: [...state.messages, action.payload] };
        case "SET_NEW_MESSAGE":
            return { ...state, newMessage: action.payload };
        default:
            throw new Error(`Unhandled action type: ${(action as any).type}`);
    }
 }




export function ChannelDetails() {
    const { id } = useParams<{ id: string }>();
    const { token, userID, gameId } = useAuth();
    const [state, dispatch] = React.useReducer(reducer, initialState);
    //const { channel, isUserInChannel, searchName, foundUserId, ownerId, messages, newMessage } = state;
    const { channel, searchName, foundUserId, ownerId, messages, newMessage } = state;
    const [invitationType, setInvitationType] = React.useState("READ_WRITE");
    const navigate = useNavigate();

    React.useEffect(() => {
        const fetchChannelById = async () => {
            try {
                const response = await fetch(`http://localhost:8081/api/channels/find/${gameId}`, {
                    method: "GET",
                    headers: { Authorization: `Bearer ${token}` },
                });
                const data = await response.json();
                console.log("Channel data:", data);
                dispatch({ type: "SET_CHANNEL", payload: data });

                if (data.owner && typeof data.owner === "object") {
                    await fetchOwnerByName(data.owner.username);
                }

                fetchMessages(data.id);
            } catch (error) {
                console.error("Error fetching channel by ID:", error);
            }
        };

        const fetchMessages = async (channelId: number) => {
            try {
                const response = await fetch(`http://localhost:8081/api/messages/${channelId}`, {
                    method: "GET",
                    headers: { Authorization: `Bearer ${token}` },
                });

                const data = await response.json();
                console.log("Messages data:", data);

                const formattedMessages = Array.isArray(data)
                    ? data.map((message) => ({
                        id: message.id,
                        text: message.text,
                        sender: message.user?.username || "Unknown",
                        timestamp: message.dateCreated,
                    }))
                    : [];
                dispatch({ type: "SET_MESSAGES", payload: formattedMessages });
            } catch (error) {
                console.error("Error fetching messages:", error);
                dispatch({ type: "SET_MESSAGES", payload: [] });
            }
        };

        const fetchOwnerByName = async (ownerName: string) => {
            try {
                const encodedName = encodeURIComponent(ownerName);
                const response = await fetch(`http://localhost:8081/api/users/search?name=${encodedName}`, {
                    method: "GET",
                    headers: { Authorization: `Bearer ${token}` },
                });
                const data = await response.json();
                if (data && data.id) {
                    dispatch({ type: "SET_OWNER_ID", payload: data.id });
                } else {
                    console.error("Owner not found");
                }
            } catch (error) {
                console.error("Error fetching owner by name:", error);
            }
        };

        fetchChannelById();

        const eventSource = new EventSource(`http://localhost:8081/api/channels/${id}/listen`);

        eventSource.onmessage = (event) => {
            try {
                const parsedData = JSON.parse(event.data);
                const formattedMessage = {
                    id: parsedData.id,
                    text: parsedData.message.text,
                    sender: parsedData.message.user.username || "Unknown",
                    timestamp: parsedData.message.dateCreated,
                };
                dispatch({ type: "ADD_MESSAGE", payload: formattedMessage });

                if (Notification.permission === "granted") {
                    new Notification(`${formattedMessage.sender}: ${formattedMessage.text}`, {
                        body: new Date(formattedMessage.timestamp).toLocaleString(),
                    });
                }
            } catch (error) {
                console.error("Error parsing message data:", error);
            }
        };

        return () => {
            eventSource.close();
        };

    }, [id, token, userID, gameId]);

    const leaveChannel = async () => {
        // Verifique se o usuário é o proprietário do canal
        if (isOwner) {
            alert("You cannot leave the channel as you are the owner.");
            return;
        }

        // Verifique se o usuário está no canal
        const userInChannelResponse = await fetch(`http://localhost:8081/api/channels/${id}/${userID}`, {
            method: "GET",
            headers: { Authorization: `Bearer ${token}` },
        });

        const userInChannel = await userInChannelResponse.json();

        // Se o usuário não estiver no canal, exiba a mensagem de erro
        if (!userInChannel) {
            alert("You are not in this channel, can't leave.");
            return;
        }

        try {
            const response = await fetch(`http://localhost:8081/api/channels/leaveChannel/${id}/${userID}`, {
                method: "GET",
                headers: { Authorization: `Bearer ${token}` },
            });

            if (response.ok) {
                alert("You have successfully left the channel.");
                navigate("/");
            } else {
                const errorMessage = await response.text();
                alert("Failed to leave the channel: " + errorMessage);
            }
        } catch (error) {
            console.error("Error leaving channel:", error);
            alert("An error occurred while trying to leave the channel.");
        }
    };


    const searchUserByName = async () => {
        try {
            const response = await fetch(`http://localhost:8081/api/users/search?name=${searchName}`, {
                method: "GET",
                headers: { Authorization: `Bearer ${token}` },
            });
            const data = await response.json();
            if (data && data.id) {
                dispatch({ type: "SET_FOUND_USER_ID", payload: data.id });
            } else {
                alert("User not found");
            }
        } catch (error) {
            console.error("Error searching for user:", error);
        }
    };

    const sendInvitation = async () => {
        if (foundUserId === null) return;

        try {
            const userInChannelResponse = await fetch(`http://localhost:8081/api/channels/${id}/${foundUserId}`, {
                method: "GET",
                headers: { Authorization: `Bearer ${token}` },
            });

            if (userInChannelResponse.ok) {
                const isUserInChannel = await userInChannelResponse.json();
                if (isUserInChannel) {
                    alert("User is already in the channel.");
                    return;
                }

                const response = await fetch("http://localhost:8081/api/invitations/send", {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${token}`,
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({
                        channelId: id,
                        guestId: foundUserId,
                        invitationType: invitationType,
                    }),
                });

                if (response.ok) {
                    alert("Invitation sent successfully");
                } else {
                    const error = await response.json();
                    console.error("Failed to send invitation:", error);
                    alert("Failed to send invitation: " + error.title);
                }
            } else {
                alert("Failed to check if user is in the channel.");
            }
        } catch (error) {
            console.error("Error sending invitation:", error);
        }
    };

    const handleSendMessage = async () => {
        if (!newMessage.trim()) return;

        try {
            const isPublicChannel = channel?.controls === "PUBLIC";

            const userInChannelResponse = await fetch(`http://localhost:8081/api/channels/${id}/${foundUserId}`, {
                method: "GET",
                headers: { Authorization: `Bearer ${token}` },
            });

            const userInChannel = await userInChannelResponse.json();

            if (isPublicChannel || userInChannel) {
                const response = await fetch("http://localhost:8081/api/messages", {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${token}`,
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({ channelId: id, text: newMessage }),
                });

                if (response.ok) {
                    dispatch({ type: "SET_NEW_MESSAGE", payload: "" });
                } else {
                    console.error("Failed to send message");
                }
            } else {
                alert("You are not allowed to send messages to this private channel.");
            }
        } catch (error) {
            console.error("Error sending message:", error);
        }
    };

    if (!channel) {
        return <div>Loading...</div>;
    }

    const isOwner = ownerId === userID;
    console.log("isOwner:", isOwner, "ownerId:", ownerId, "userID:", userID);

    const isUserInChannel = channel?.users?.includes(userID.toString()); // Converte userID para string
    console.log(isUserInChannel)

    return (
        <div className="container mt-5">
            <h1>Channel Details</h1>
            <p><strong>Name:</strong> {channel.name}</p>
            <p><strong>Type:</strong> {channel.type}</p>
            {channel.controls && <p><strong>Visibility:</strong> {channel.controls}</p>}
            {channel.users && (
                <div>
                    <h2>Users</h2>
                    <ul>
                        {channel.users.map((user, index) => (
                            <li key={index}>{user}</li>
                        ))}
                    </ul>
                </div>
            )}

            {/* Messages Section */}
            <div className="mt-4">
                <h2>Messages</h2>
                <div style={{ maxHeight: "300px", overflowY: "auto", border: "1px solid #ccc", padding: "10px" }}>
                    {messages.map((message, index) => (
                        <div key={`${message.id}-${index}`} style={{ marginBottom: "10px" }}>
                            <strong>{message.sender}:</strong> {message.text}
                            <br />
                            <small>{new Date(message.timestamp).toLocaleString()}</small>
                        </div>
                    ))}
                </div>
                <div className="mt-3">
                    <textarea
                        value={newMessage}
                        onChange={(e) => dispatch({ type: "SET_NEW_MESSAGE", payload: e.target.value })}
                        placeholder="Type your message..."
                        className="form-control"
                    ></textarea>
                    <button onClick={handleSendMessage} className="btn btn-primary mt-2">
                        Send Message
                    </button>
                </div>
            </div>

            {/* Invite Section */}
            {isOwner && (
                <div className="mt-3">
                    <input
                        type="text"
                        placeholder="Search user by name"
                        value={searchName}
                        onChange={(e) => dispatch({ type: "SET_SEARCH_NAME", payload: e.target.value })}
                        className="form-control mb-2"
                    />
                    <button onClick={searchUserByName} className="btn btn-secondary mb-2">
                        Search User
                    </button>
                    {foundUserId && (
                        <>
                            <select
                                value={invitationType}
                                onChange={(e) => setInvitationType(e.target.value)}
                                className="form-control mb-2"
                            >
                                <option value="READ_ONLY">Read Only</option>
                                <option value="READ_WRITE">Read and Write</option>
                            </select>
                            <button onClick={sendInvitation} className="btn btn-secondary">
                                Send Invitation
                            </button>
                        </>
                    )}
                </div>
            )}
            {/* Leave Channel Button */}
            {!isOwner && (
                     <button onClick={leaveChannel} className="btn btn-danger mt-4">
                         Leave Channel
                     </button>
                 )}

                 <button onClick={() => navigate("/")} className="btn btn-secondary mt-4">
                     Go Back
                 </button>
            </div>
    );
}