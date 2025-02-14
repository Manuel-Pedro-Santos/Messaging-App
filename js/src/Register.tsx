import * as React from "react";
import { useNavigate } from "react-router-dom";
import 'bootstrap/dist/css/bootstrap.min.css';
import { useAuth } from "./AuthContext";

type State = {
    email: string;
    username: string;
    password: string;
    registryToken: string;
    submitting: boolean;
    error: string;
};

type Action =
    | { type: "SET_FIELD"; field: keyof State; value: string | boolean }
    | { type: "SET_SUBMITTING"; value: boolean }
    | { type: "SET_ERROR"; value: string };

const initialState: State = {
    email: "",
    username: "",
    password: "",
    registryToken: "",
    submitting: false,
    error: "",
};

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "SET_FIELD":
            return { ...state, [action.field]: action.value };
        case "SET_SUBMITTING":
            return { ...state, submitting: action.value };
        case "SET_ERROR":
            return { ...state, error: action.value };
        default:
            throw new Error(`Unhandled action type: ${(action as any).type}`);
    }
}

export function Register() {
    const [state, dispatch] = React.useReducer(reducer, initialState);
    const navigate = useNavigate();

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        dispatch({ type: "SET_SUBMITTING", value: true });
        dispatch({ type: "SET_ERROR", value: "" }); // Clear previous errors

        try {
            const response = await fetch("http://localhost:8081/api/users/create", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    email: state.email,
                    username: state.username,
                    password: state.password,
                    registryToken: state.registryToken,
                }),
            });

            if (!response.ok) {
                throw new Error("Registration failed");
            }

            // Automatically navigate to login after successful registration
            navigate("/login");
        } catch (err: any) {
            dispatch({ type: "SET_ERROR", value: err.message || "An unknown error occurred" });
        } finally {
            dispatch({ type: "SET_SUBMITTING", value: false });
        }
    };

    const handleFieldChange = (field: keyof State) => (event: React.ChangeEvent<HTMLInputElement>) => {
        dispatch({ type: "SET_FIELD", field, value: event.target.value });
    };

    return (
        <div className="container mt-5">
            <h1>Register</h1>
            <form onSubmit={handleSubmit}>
                <div className="mb-3">
                    <label htmlFor="email" className="form-label">Email address</label>
                    <input
                        type="email"
                        className="form-control"
                        id="email"
                        placeholder="name@example.com"
                        value={state.email}
                        onChange={handleFieldChange("email")}
                        required
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="username" className="form-label">Username</label>
                    <input
                        type="text"
                        className="form-control"
                        id="username"
                        placeholder="Username"
                        value={state.username}
                        onChange={handleFieldChange("username")}
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
                        value={state.password}
                        onChange={handleFieldChange("password")}
                        required
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="registryToken" className="form-label">Registry Token</label>
                    <input
                        type="text"
                        className="form-control"
                        id="registryToken"
                        placeholder="Registry Token"
                        value={state.registryToken}
                        onChange={handleFieldChange("registryToken")}
                        required
                    />
                </div>
                <button type="submit" className="btn btn-primary" disabled={state.submitting}>
                    Register
                </button>
            </form>
            {state.submitting && <p>Submitting...</p>}
            {state.error && <p className="text-danger">{state.error}</p>}
        </div>
    );
}
