package pt.isel.daw

import java.util.Properties
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class MailService {
    fun sendEmail(
        to: String,
        subject: String,
        bodyText: String,
        inviterEmail: String // Email do utilizador que enviou o convite
    ) {
        val properties = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.ssl.trust", "smtp.gmail.com")
        }

        val session = Session.getInstance(
            properties,
            object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication() =
                    javax.mail.PasswordAuthentication(
                        "melsalinho2@gmail.com", // Conta fixa
                        "sboq bbsq kawe toub"
                    )
            }
        )

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress("melsalinho2@gmail.com")) // Remetente fixo
            addRecipient(Message.RecipientType.TO, InternetAddress(to))
            this.subject = subject
            setText(
                """
                Olá,

                Você foi convidado a se juntar ao G01 Messaging App por ${inviterEmail}.
                
                Aqui está o seu token de registro: ${bodyText}
                """.trimIndent()
            )
        }

        Transport.send(message)
    }
}

