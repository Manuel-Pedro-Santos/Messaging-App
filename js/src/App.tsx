import * as React from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { Home } from "./Home";
import { Authors } from "./Authors";
import { AuthProvider } from "./AuthContext";
import { Login } from "./Login";
import { Register } from "./Register";
import { Channel } from "./Channel";
import { ChannelDetails } from "./ChannelDetails";
import Me from "./Me";
import {Logout} from "./Logout";
import { AppInvites } from "./AppInvites"
import {Invitations} from "./Invitations";

const router = createBrowserRouter([
    {
        path: '/',
        element: <Home />,
    },
    {
        path: '/channels',
        element: <Channel />,
    },
    {
        path: '/channels/:id',
        element: <ChannelDetails />,
    },
    {
        path: '/invitations',
        element: <Invitations />,
    },
    {
        path: '/authors',
        element: <Authors />,
    },
    {
        path: '/login',
        element: <Login />,
    },
    {
        path: '/logout',
        element: <Logout />,
    },
    {
        path: '/register',
        element: <Register />,
    },
    {
        path: "/me",
        element: <Me />,
    },
    {
        path: "/invite",
        element: <AppInvites />,
    }
]);

export function App() {
    createRoot(document.getElementById("container")).render(
        <AuthProvider>
            <RouterProvider router={router} future={{ v7_startTransition: true }} />
        </AuthProvider>
    );
}