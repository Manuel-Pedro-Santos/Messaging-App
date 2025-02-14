import * as React from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import 'bootstrap/dist/css/bootstrap.min.css';

type LoginState = {
    email: string;
    password: string;
    submitting: boolean;
    error: string;
};

type LoginAction =
    | { type: "SET_EMAIL"; payload: string }
    | { type: "SET_PASSWORD"; payload: string }
    | { type: "SET_SUBMITTING"; payload: boolean }
    | { type: "SET_ERROR"; payload: string };

const initialState: LoginState = {
    email: "",
    password: "",
    submitting: false,
    error: "",
};

function loginReducer(state: LoginState, action: LoginAction): LoginState {
    switch (action.type) {
        case "SET_EMAIL":
            return { ...state, email: action.payload };
        case "SET_PASSWORD":
            return { ...state, password: action.payload };
        case "SET_SUBMITTING":
            return { ...state, submitting: action.payload };
        case "SET_ERROR":
            return { ...state, error: action.payload };
        default:
            throw new Error(`Unhandled action type: ${(action as any).type}`);
    }
}

export function Login() {
    const [state, dispatch] = React.useReducer(loginReducer, initialState);
    const { email, password, submitting, error } = state;

    const context = useAuth();
    const navigate = useNavigate();

    // Handles the form submission
    async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        dispatch({ type: "SET_SUBMITTING", payload: true });
        dispatch({ type: "SET_ERROR", payload: "" }); // Clear previous errors

        try {
            // Make the login request
            const response = await fetch("http://localhost:8081/api/users/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ email, password }),
            });

            if (!response.ok) {
                throw new Error("Login failed: Invalid email or password");
            }

            const responseData = await response.json();
            const token = responseData.token;

            if (!token) {
                throw new Error("No token received from the server");
            }

            // Save token to context and mark user as logged in
            context.setToken(token);
            context.setLoggedIn(true);

            // Fetch user details using the token
            const userResponse = await fetch("http://localhost:8081/api/me", {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
            });

            if (!userResponse.ok) {
                throw new Error("Failed to fetch user details");
            }

            const userData = await userResponse.json();
            context.setUserID(userData.id);

            console.log("User ID:", context.userID); // Debugging log
            console.log("User Token:", context.token); // Debugging log

            // Navigate to home after successful login
            navigate("/");
        } catch (err: any) {
            dispatch({ type: "SET_ERROR", payload: err.message || "An unknown error occurred" });
        } finally {
            dispatch({ type: "SET_SUBMITTING", payload: false });
        }
    }

    return (
        <div className="container mt-5">
            <h1>Login</h1>
            <form onSubmit={handleSubmit}>
                <fieldset disabled={submitting}>
                    <div className="mb-3">
                        <label htmlFor="email" className="form-label">Email address</label>
                        <input
                            type="email"
                            className="form-control"
                            id="email"
                            placeholder="name@example.com"
                            value={email}
                            onChange={(e) => dispatch({ type: "SET_EMAIL", payload: e.target.value })}
                            required
                        />
                    </div>
                    <div className="mb-3">
                        <label htmlFor="password" className="form-label">Password</label>
                        <input
                            type="password"
                            className="form-control"
                            id="password"
                            placeholder="Password"
                            value={password}
                            onChange={(e) => dispatch({ type: "SET_PASSWORD", payload: e.target.value })}
                            required
                        />
                    </div>
                    <button type="submit" className="btn btn-primary">Login</button>
                </fieldset>
            </form>
            {submitting && <p>Submitting...</p>}
            {error && <p className="text-danger">{error}</p>}
        </div>
    );
}
