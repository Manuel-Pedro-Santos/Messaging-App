import * as React from "react";
import { useAuth } from "./AuthContext"; // Certifique-se de importar o hook correto para autenticação

export const AppInvites = () => {
    const { token } = useAuth(); // Obter o token do contexto de autenticação
    const [email, setEmail] = React.useState("");

    const sendInvite = async () => {
        if (!token) {
            console.error("Token not found. User might not be logged in.");
            alert("You must be logged in to send invites.");
            return;
        }

        try {
            const response = await fetch("http://localhost:8081/api/invitations/sendEmail", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`, // Passa o token JWT no cabeçalho
                },
                body: JSON.stringify({ email }), // O corpo precisa ter apenas o email
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));;
                console.error("Error response from server:", errorData);
                alert(`Failed to send invite: ${errorData.message || response.statusText}`);
                return;
            }

            const data = await response.text(); // O backend retorna uma string de sucesso
            console.log("Invite sent successfully:", data);
            alert(data); // Mostra a mensagem de sucesso
        } catch (error) {
            console.error("Error sending invite:", error);
            alert("Failed to send invite. Please try again later.");
        }
    };

    return (
        <div className="invite-container">
            <h2>Send Invitation</h2>
            <input
                type="email"
                placeholder="Enter email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="form-control mb-3"
            />
            <button
                className="btn btn-primary"
                onClick={sendInvite} // Chama a função ao clicar
                disabled={!email.trim()} // Desativa o botão se o email estiver vazio
            >
                Send Invite
            </button>
        </div>
    );
};
