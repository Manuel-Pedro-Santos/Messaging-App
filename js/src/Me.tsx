import * as React from "react";
import { useState, useEffect } from "react";
import { useAuth } from "./AuthContext";
import {Link} from "react-router-dom";

interface UserHomeOutputModel {
    id: number;
    name: string;
    email: string;
}

function Me() {
    const { token } = useAuth();
    const [user, setUser] = useState<UserHomeOutputModel | null>(null);

    useEffect(() => {
        const fetchUser = async () => {
            try {
                const response = await fetch("http://localhost:8081/api/me", {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                });
                const data = await response.json();
                setUser(data);
            } catch (error) {
                console.error("Error fetching user data:", error);
            }
        };

        fetchUser();
    }, [token]);

    if (!user) {
        return <div>Loading...</div>;
    }

    return (
        <div className="container mt-5">
            <div className="card">
                <div className="card-header">
                    <h2>User Information</h2>
                </div>
                <div className="card-body">
                    <h5 className="card-title">Name: {user.name}</h5>
                    <p className="card-text">Email: {user.email}</p>
                    <p className="card-text">ID: {user.id}</p>
                </div>
            </div>
            <Link to="/">
                <button className="btn btn-primary mt-4">Go Back</button>
            </Link>
        </div>
    );
}

export default Me;