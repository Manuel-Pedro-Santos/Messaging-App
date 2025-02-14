import * as React from "react";
import { Link } from "react-router-dom";
import { useAuth } from "./AuthContext";
import 'bootstrap/dist/css/bootstrap.min.css';

export function Home() {
    const { loggedIn, userID, token } = useAuth();
    const [username, setUsername] = React.useState("");

    const fetchUserDetails = async () => {
        if (loggedIn && userID) {
            try {
                const response = await fetch(`http://localhost:8081/api/users/${userID}`);
                const data = await response.json();
                setUsername(data.username);
            } catch (error) {
                console.error("Error fetching user details:", error);
            }
        }
    };

    React.useEffect(() => {
        fetchUserDetails();
    }, [loggedIn, userID]);

    console.log("HOMEEE Token:", token); // Debugging log
    console.log("HOMEEE User ID:", userID); // Debugging log

    return (
        <div className="container mt-5 text-center">
            <div className="d-flex justify-content-between mb-3">
                <Link to="/authors">
                    <button className="btn btn-primary">About</button>
                </Link>
                <div>
                    {loggedIn ? (
                        <>
                            <Link to="/invite">
                                <button className="btn btn-success ms-2">Invite</button>
                            </Link>
                            <Link to={"/logout"}>
                                <button className="btn btn-danger">Logout</button>
                            </Link>
                        </>
                    ) : (
                        <>
                            <Link to="/register">
                                <button className="btn btn-secondary me-2">Register</button>
                            </Link>
                            <Link to="/login">
                                <button className="btn btn-primary">Login</button>
                            </Link>
                        </>
                    )}
                </div>
            </div>
            <h1 className="my-5">
                {loggedIn ? `Welcome ${username}, start chatting!` : "Hello and welcome to our Messaging App!"}
            </h1>
            {loggedIn && (
                <div className="row list-unstyled bg-light p-3 rounded shadow">
                    <div className="col mb-2">
                        <Link to="/channels" className="text-primary text-decoration-none fw-bold">CHANNELS</Link>
                    </div>
                    <div className="col mb-2">
                        <Link to="/invitations" className="text-primary text-decoration-none fw-bold">INVITATIONS</Link>
                    </div>
                    <div className="col mb-2">
                        <Link to="/me" className="text-primary text-decoration-none fw-bold">ME</Link>
                    </div>
                </div>
            )}
        </div>
    );
}
