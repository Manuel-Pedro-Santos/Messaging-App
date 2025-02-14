import * as React from "react";
import { useAuth } from "./AuthContext";
import { useNavigate } from "react-router-dom";


interface RawInvitation {
    id: number;
    channel: {
        name: string;
        owner: {
            username: string;
        };
    };
    guest: {
        username: string;
    };
}

interface Invitation {
    id: number;
    channelName: string;
    senderName: string;
}

interface NotificationsState {
    invitations: Invitation[];
    loading: boolean;
    error: string | null;
}

const initialState: NotificationsState = {
    invitations: [],
    loading: true,
    error: null,
};



type InvitationAction =
    | { type: "SET_INVITATIONS"; payload: Invitation[] }
    | { type: "SET_LOADING"; payload: boolean }
    | { type: "SET_ERROR"; payload: string | null }
    | { type: "REMOVE_INVITATION"; payload: number };

function reducer(state: NotificationsState, action: InvitationAction): NotificationsState {
    switch (action.type) {
        case "SET_INVITATIONS":
            return { ...state, invitations: action.payload, loading: false };
        case "SET_LOADING":
            return { ...state, loading: action.payload };
        case "SET_ERROR":
            return { ...state, error: action.payload, loading: false };
        case "REMOVE_INVITATION":
            return { ...state, invitations: state.invitations.filter((inv) => inv.id !== action.payload) };
        default:
            return state;
    }
}
export function Invitations() {
    const { token, userID } = useAuth();
    const [state, dispatch] = React.useReducer(reducer, initialState);
    const { invitations, loading, error } = state;
    const navigate = useNavigate();

    React.useEffect(() => {
        const fetchInvitations = async () => {
            try {
                console.log("Fetching invitations...");
                const response = await fetch(`http://localhost:8081/api/invitations/${userID}`, {
                    method: "GET",
                    headers: { Authorization: `Bearer ${token}` },
                });

                console.log("Response status:", response.status);
                if (response.ok) {
                    const rawData: RawInvitation[] = await response.json();
                    console.log("Raw invitations data:", rawData);

                    const transformedData: Invitation[] = rawData.map((raw) => ({
                        id: raw.id,
                        channelName: raw.channel.name,
                        senderName: raw.channel.owner.username,
                    }));

                    console.log("Transformed invitations data:", transformedData);
                    dispatch({ type: "SET_INVITATIONS", payload: transformedData });
                } else {
                    console.error("Failed to fetch invitations. Status:", response.status);
                    throw new Error("Failed to fetch invitations");
                }
            } catch (err: any) {
                console.error("Error fetching invitations:", err.message);
                dispatch({ type: "SET_ERROR", payload: err.message });
            }
        };

        fetchInvitations();
    }, [userID, token]);



    const handleAcceptInvitation = async (invitationId: number) => {
        try {
            const response = await fetch(`http://localhost:8081/api/invitations/accept/${invitationId}`, {
                method: "GET",
                headers: { Authorization: `Bearer ${token}` },
            });

            if (response.ok) {
                alert("Invitation accepted successfully!");
                dispatch({ type: "REMOVE_INVITATION", payload: invitationId });
            } else {
                throw new Error("Failed to accept the invitation");
            }
        } catch (err: any) {
            console.error(err.message);
            alert("An error occurred while accepting the invitation.");
        }
    };

    const handleRejectInvitation = async (invitationId: number) => {
        try {
            const response = await fetch(`http://localhost:8081/api/invitations/remove?invID=${invitationId}`, {
                method: "GET",
                headers: { Authorization: `Bearer ${token}` },
            });

            if (response.ok) {
                alert("Invitation rejected successfully!");
                dispatch({ type: "REMOVE_INVITATION", payload: invitationId });
            } else {
                throw new Error("Failed to reject the invitation");
            }
        } catch (err: any) {
            console.error(err.message);
            alert("An error occurred while rejecting the invitation.");
        }
    };

    if (loading) {
        return <div>Loading notifications...</div>;
    }

    if (error) {
        return <div>Error: {error}</div>;
    }

    return (
        <div className="container mt-5">
            <h1>Invitations</h1>
            {invitations.length > 0 ? (
                <ul className="list-group">
                    {invitations.map((invitation) => (
                        <li
                            key={invitation.id}
                            className="list-group-item d-flex justify-content-between align-items-center"
                        >
                            <div>
                                <strong>Channel: {invitation.channelName}</strong> <br />
                                <small>
                                    Invited by: <em>{invitation.senderName}</em>
                                </small>
                            </div>
                            <div>
                                <button
                                    className="btn btn-success btn-sm me-2"
                                    onClick={() => handleAcceptInvitation(invitation.id)}
                                >
                                    Accept
                                </button>
                                <button
                                    className="btn btn-danger btn-sm"
                                    onClick={() => handleRejectInvitation(invitation.id)}
                                >
                                    Reject
                                </button>
                            </div>
                        </li>
                    ))}
                </ul>
            ) : (
                <div className="alert alert-info mt-3" role="alert">
                    You have no notifications at the moment.
                </div>
            )}
            <button onClick={() => navigate("/")} className="btn btn-secondary mt-4">
                Go Back
            </button>
        </div>
    );
}





