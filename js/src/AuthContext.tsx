import * as React from "react";

interface AuthState {
    loggedIn: boolean;
    token: string | null;
    userID: number | null;
    gameId: number | null;
}

type AuthAction =
    | { type: "SET_LOGGED_IN"; payload: boolean }
    | { type: "SET_TOKEN"; payload: string | null }
    | { type: "SET_USER_ID"; payload: number | null }
    | { type: "SET_GAME_ID"; payload: number | null };

function authReducer(state: AuthState, action: AuthAction): AuthState {
    switch (action.type) {
        case "SET_LOGGED_IN":
            return { ...state, loggedIn: action.payload };
        case "SET_TOKEN":
            return { ...state, token: action.payload };
        case "SET_USER_ID":
            return { ...state, userID: action.payload };
        case "SET_GAME_ID":
            return { ...state, gameId: action.payload };
    }
}

interface AuthContextType extends AuthState {
    dispatch: React.Dispatch<AuthAction>;
    setToken: (token: string | null) => void;
    setLoggedIn: (loggedIn: boolean) => void;
    setUserID: (userID: number | null) => void;
    setGameId: (gameId: number | null) => void;
}

export const AuthContext = React.createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
    const [state, dispatch] = React.useReducer<React.Reducer<AuthState, AuthAction>>(authReducer, {
        loggedIn: !!localStorage.getItem("token"),
        token: localStorage.getItem("token"),
        userID: localStorage.getItem("userID") ? Number(localStorage.getItem("userID")) : null,
        gameId: localStorage.getItem("gameId") ? Number(localStorage.getItem("gameId")) : null,
    });

    const setToken = (token: string | null) => {
        if (token !== null) {
            localStorage.setItem("token", token);
        } else {
            localStorage.removeItem("token");
        }
        dispatch({ type: "SET_TOKEN", payload: token });
        dispatch({ type: "SET_LOGGED_IN", payload: !!token });
    };

    const setLoggedIn = (loggedIn: boolean) => {
        dispatch({ type: "SET_LOGGED_IN", payload: loggedIn });
    };

    const setUserID = (userID: number | null) => {
        if (userID !== null) {
            localStorage.setItem("userID", userID.toString());
        } else {
            localStorage.removeItem("userID");
        }
        dispatch({ type: "SET_USER_ID", payload: userID });
    };

    const setGameId = (gameId: number | null) => {
        if (gameId !== null) {
            localStorage.setItem("gameId", gameId.toString());
        } else {
            localStorage.removeItem("gameId");
        }
        dispatch({ type: "SET_GAME_ID", payload: gameId });
    };

    return (
        <AuthContext.Provider value={{ ...state, dispatch, setToken, setLoggedIn, setUserID, setGameId }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = React.useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;

};