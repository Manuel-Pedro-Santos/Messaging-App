import * as React from "react";
import { useAuth } from "./AuthContext";
import { useNavigate } from "react-router-dom";
import 'bootstrap/dist/css/bootstrap.min.css';

export function Logout() {
    const { setToken, setLoggedIn, setUserID, token } = useAuth();
    const navigate = useNavigate();
    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState<string | null>(null);

    const handleLogout = async () => {
        setLoading(true);
        setError(null);

        if(!token) {
            navigate("/login");
            return;
        }
        try {
            const response = await fetch("http://localhost:8081/api/users/logout", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`,
                },
            });

            if (!response.ok) {
                throw new Error("Failed to logout");
            }

            // Clear sessionStorage
            sessionStorage.clear();
            localStorage.clear();

            // Clear authentication state
            setToken(null);
            setLoggedIn(false);
            setUserID(null);

            // Redirect to the login page
            navigate("/login");
        } catch (err: any) {
            setError(err.message || "An unknown error occurred");
        } finally {
            setLoading(false);
        }
    };

    React.useEffect(() => {
        handleLogout();
    }, []);

    return (
        <div className="container mt-5">
            <h1>Logging out...</h1>
            {loading && <p>Loading...</p>}
            {error && <p className="text-danger">{error}</p>}
        </div>
    );
}