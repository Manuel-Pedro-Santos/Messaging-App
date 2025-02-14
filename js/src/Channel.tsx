import * as React from "react";
import { useReducer, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";

interface Channel {
    id: number;
    name: string;
    type: string;
    controls?: string;
}

interface ChannelInput {
    name: string;
    type: string;
    controls?: string;
}

type State = {
    channels: Channel[];
    channel: Channel | null;
    newChannel: ChannelInput;
    username: string;
    searchName: string;
    isCreating: boolean;
    searchingByName: boolean;
    userChannelIds: number[];
};

type Action =
    | { type: "SET_CHANNELS"; channels: Channel[] }
    | { type: "SET_CHANNEL"; channel: Channel | null }
    | { type: "SET_USERNAME"; username: string }
    | { type: "SET_SEARCH_NAME"; searchName: string }
    | { type: "SET_NEW_CHANNEL"; newChannel: Partial<ChannelInput> }
    | { type: "SET_CREATING"; isCreating: boolean }
    | { type: "SET_SEARCHING_BY_NAME"; searchingByName: boolean }
    | { type: "SET_USER_CHANNEL_IDS"; userChannelIds: number[] };

const initialState: State = {
    channels: [],
    channel: null,
    newChannel: { name: "", type: "SINGLE", controls: "PUBLIC" },
    username: "",
    searchName: "",
    isCreating: false,
    searchingByName: false,
    userChannelIds: [],
};

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "SET_CHANNELS":
            return { ...state, channels: action.channels };
        case "SET_CHANNEL":
            return { ...state, channel: action.channel };
        case "SET_USERNAME":
            return { ...state, username: action.username };
        case "SET_SEARCH_NAME":
            return { ...state, searchName: action.searchName };
        case "SET_NEW_CHANNEL":
            return { ...state, newChannel: { ...state.newChannel, ...action.newChannel } };
        case "SET_CREATING":
            return { ...state, isCreating: action.isCreating };
        case "SET_SEARCHING_BY_NAME":
            return { ...state, searchingByName: action.searchingByName };
        case "SET_USER_CHANNEL_IDS":
            return { ...state, userChannelIds: action.userChannelIds };
    }
}

export function Channel() {
    const [state, dispatch] = useReducer(reducer, initialState);
    const { userID, token, setGameId } = useAuth();
    const navigate = useNavigate();

    // Fetch authenticated user's name
    useEffect(() => {
        const fetchUsername = async () => {
            try {
                const userResponse = await fetch("http://localhost:8081/api/me", {
                    method: "GET",
                    headers: {
                        "Authorization": `Bearer ${token}`,
                        "Content-Type": "application/json",
                    },
                });

                if (!userResponse.ok) throw new Error("Failed to fetch user details");
                const data = await userResponse.json();
                dispatch({ type: "SET_USERNAME", username: data.name });
            } catch (error) {
                console.error("Error fetching username:", error);
            }
        };

        fetchUsername();
    }, [token]);

    const fetchChannelsByName = async (name: string) => {
        try {
            dispatch({ type: "SET_SEARCHING_BY_NAME", searchingByName: true }); // Indicando que está buscando por nome
            const response = await fetch(`http://localhost:8081/api/channels/${name}`, {
                method: "GET",
                headers: { "Authorization": `Bearer ${token}` },
            });
            const data = await response.json();
            dispatch({ type: "SET_CHANNELS", channels: Array.isArray(data) ? data : [] });
        } catch (error) {
            console.error("Error fetching channels by name:", error);
            dispatch({ type: "SET_CHANNELS", channels: [] });
        }
    };

    const fetchUserChannels = async () => {
        try {
            dispatch({ type: "SET_SEARCHING_BY_NAME", searchingByName: false });
            const response = await fetch(`http://localhost:8081/api/channels/user/${userID}`, {
                method: "GET",
                headers: { "Authorization": `Bearer ${token}` },
            });
            const data = await response.json();
            const userChannelIds = Array.isArray(data) ? data.map((channel: Channel) => channel.id) : [];
            dispatch({ type: "SET_CHANNELS", channels: Array.isArray(data) ? data : [] });
            dispatch({ type: "SET_USER_CHANNEL_IDS", userChannelIds }); // Atualiza os IDs dos canais do usuário
        } catch (error) {
            console.error("Error fetching user channels:", error);
            dispatch({ type: "SET_CHANNELS", channels: [] });
            dispatch({ type: "SET_USER_CHANNEL_IDS", userChannelIds: [] }); // Reseta a lista em caso de erro
        }
    };

    const joinChannel = async (channelId: number) => {
        try {
            const response = await fetch(`http://localhost:8081/api/channels/join/${channelId}`, {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`,
                },
            });
            console.log(response)
            if (!response.ok) throw new Error("Failed to join channel");
            const data = await response.text();
            console.log("Successfully joined channel:", data);
            fetchUserChannels(); // Atualiza os canais do usuário
        } catch (error) {
            console.error("Error joining channel:", error);
        }
    };


    // Create a new channel
    const createChannel = async () => {
        if (state.isCreating) return;
        dispatch({ type: "SET_CREATING", isCreating: true });

        try {
            const response = await fetch("http://localhost:8081/api/channels/create", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`,
                },
                body: JSON.stringify(state.newChannel),
            });
            const data = await response.json();
            console.log("Channel created with ID:", data);
            fetchUserChannels(); // Refresh the channel list
        } catch (error) {
            console.error("Error creating channel:", error);
        } finally {
            dispatch({ type: "SET_CREATING", isCreating: false });
        }
    };

    return (
        <div className="container mt-5">
            <div className="mb-3">
                <h1>Search Channels</h1>
                <h3>Search by User</h3>
                <button onClick={fetchUserChannels} className="btn btn-primary ms-1">
                    Channels for <strong>{state.username}</strong>
                </button>
                <h3>Search by Name</h3>
                <input
                    type="text"
                    placeholder="Channel Name"
                    value={state.searchName}
                    onChange={(e) => dispatch({ type: "SET_SEARCH_NAME", searchName: e.target.value })}
                    className="form-control mt-2"
                />
                <button onClick={() => fetchChannelsByName(state.searchName)} className="btn btn-primary mt-2">
                    Fetch Channels by Name
                </button>
            </div>
            {state.channels.length === 0 ? (
                <p>No channels found</p>
            ) : (
                <ul className="list-group mb-3">
                    {state.channels.map((channel) => (
                        <li key={channel.id} className="list-group-item d-flex justify-content-between align-items-center">
                            <div>
                                <Link to={`/channels/${channel.id}`} onClick={() => setGameId(channel.id)}>
                                    {channel.name}
                                </Link>
                            </div>
                            {state.searchingByName &&
                                channel.type === "GROUP" &&
                                channel.controls === "PUBLIC" &&
                                !state.userChannelIds.includes(channel.id) && ( // Verifica se o usuário já se juntou
                                    <button
                                        onClick={() => joinChannel(channel.id)}
                                        className="btn btn-primary"
                                    >
                                        Join
                                    </button>
                                )}
                        </li>
                    ))}
                </ul>
            )}
            <div className="mb-3">
                <h2>Create Channel</h2>
                <input
                    type="text"
                    placeholder="Channel Name"
                    value={state.newChannel.name}
                    onChange={(e) => dispatch({ type: "SET_NEW_CHANNEL", newChannel: { name: e.target.value } })}
                    className="form-control mb-2"
                />
                <select
                    value={state.newChannel.type}
                    onChange={(e) => dispatch({ type: "SET_NEW_CHANNEL", newChannel: { type: e.target.value } })}
                    className="form-control mb-2"
                >
                    <option value="SINGLE">Single</option>
                    <option value="GROUP">Group</option>
                </select>
                {state.newChannel.type === "GROUP" && (
                    <select
                        value={state.newChannel.controls}
                        onChange={(e) => dispatch({ type: "SET_NEW_CHANNEL", newChannel: { controls: e.target.value } })}
                        className="form-control mb-2"
                    >
                        <option value="PUBLIC">Public</option>
                        <option value="PRIVATE">Private</option>
                    </select>
                )}
                <button onClick={createChannel} className="btn btn-success" disabled={state.isCreating}>
                    {state.isCreating ? "Creating..." : "Create Channel"}
                </button>
            </div>
            <Link to="/">
                <button className="btn btn-primary mt-4">Go Back to Main Menu</button>
            </Link>
        </div>
    );
}
