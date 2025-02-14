package pt.isel.daw

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import pt.isel.daw.model.*
import kotlin.test.assertIs
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.main.allow-bean-definition-overriding=true"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class AbstractUserControllerTest {
    @Autowired
    private lateinit var channelService: ChannelService

    @Autowired
    private lateinit var passwordEncoder: BCryptPasswordEncoder

    @Autowired
    private lateinit var userService: UserService

    @LocalServerPort
    var port: Int = 0

    @Autowired
    private lateinit var trxManager: TransactionManager

    @BeforeEach
    fun setup() {
        trxManager.run {
            repoMessage.clear()
            repoInvitation.clear()
            repoChannel.clear()
            repoUser.clear()
        }
    }

    @Test
    fun `can create a new user and obtain an access token`() {
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()
        val registerUserToken = trxManager.run { repoInvitation.createRegistryToken() }
        val newUser = UserInput("john@rambo.com", "John Rambo", "securePassword", registryToken = registerUserToken)

        // Create a new user
        client
            .post()
            .uri("/users/create")
            .bodyValue(newUser)
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .value("Location") {
                assertTrue(it.startsWith("/api/users/"))
            }

        val tokenInput = UserCreateTokenInputModel(newUser.email, newUser.password)
        // Generate a token for the created user
        val tokenResult =
            client
                .post()
                .uri("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tokenInput)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody(UserCreateTokenOutputModel::class.java)
                .returnResult()
                .responseBody!!

        // Ensure the token is valid
        client
            .get()
            .uri("/me")
            .header("Authorization", "Bearer ${tokenResult.token}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("email")
            .isEqualTo(newUser.email)
    }

    @Test
    fun `should handle user logout and token revocation`() {
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()
        val registerUserToken = trxManager.run { repoInvitation.createRegistryToken() }
        val newUser = UserInput("john1@rambo.com", "John11 Rambo", "securePassword", registryToken = registerUserToken)

        // Create a new user
        client
            .post()
            .uri("/users/create")
            .bodyValue(newUser)
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .value("Location") {
                assertTrue(it.startsWith("/api/users/"))
            }

        val tokenInput = UserCreateTokenInputModel(newUser.email, newUser.password)
        // Generate a token for the created user
        val tokenResult =
            client
                .post()
                .uri("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tokenInput)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody(UserCreateTokenOutputModel::class.java)
                .returnResult()
                .responseBody!!

        // Revoke the token by logging out
        client
            .post()
            .uri("/users/logout")
            .header("Authorization", "Bearer ${tokenResult.token}")
            .exchange()
            .expectStatus()
            .isOk

        // Ensure access with the revoked token is denied
        client
            .get()
            .uri("/me")
            .header("Authorization", "Bearer ${tokenResult.token}")
            .exchange()
            .expectStatus()
            .isUnauthorized
            .expectHeader()
            .valueEquals("WWW-Authenticate", "bearer")
    }

    @Test
    fun `sendInvitationToAlreadyAuthenticated should send an invitation`() {
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Create the first user (John Rambo)
        val registerUserToken1 = trxManager.run { repoInvitation.createRegistryToken() }
        val newUser1 = UserInput("john@gmail.com", "John Rambo", "password22", registryToken = registerUserToken1)

        client
            .post()
            .uri("/users/create")
            .bodyValue(newUser1)
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .value("Location") {
                assertTrue(it.startsWith("/api/users/"))
            }

        // Create the second user (User 2)
        val registerUserToken2 = trxManager.run { repoInvitation.createRegistryToken() }
        val newUser2 = UserInput("user2@gmail.com", "user2", "password2", registryToken = registerUserToken2)

        client
            .post()
            .uri("/users/create")
            .bodyValue(newUser2)
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .value("Location") {
                assertTrue(it.startsWith("/api/users/"))
            }

        val userId2 = trxManager.run { repoUser.findByEmail("user2@gmail.com")!!.id }

        val token = userService.createToken(newUser1.email, "password22")
        assertIs<Success<TokenExternalInfo>>(token)

        val channel = ChannelInput("channel1", SelectionType.SINGLE)

        val client2 = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api/channels").build()


        client2
            .post()
            .uri("/create")
            .header("Authorization", "Bearer ${token.value.tokenValue}")
            .bodyValue(channel)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(String::class.java)
            .also {
                val channelId = trxManager.run { repoChannel.findAll().last().id }
                it.isEqualTo(channelId.toString())
            }

        val channelId = trxManager.run { repoChannel.findAll().last().id }

        val invitationInput = InvitationInput(guestId = userId2, channelId = channelId, invitationType = InvitationType.READ_WRITE)


        // Use the token to send the invitation
        client.post()
            .uri("/invitations/send")
            .header("Authorization", "Bearer ${token.value.tokenValue}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invitationInput)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(String::class.java)
            .value {
                assertTrue(it.contains("Invitation sent successfully"))
            }
    }



    @Test
    fun `sendInvitationToNewUser should send an invitation email`() {
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Create an authenticated user
        val registerUserToken = trxManager.run { repoInvitation.createRegistryToken() }
        val newUser = UserInput("user1@gmail.com", "User One", "password1", registryToken = registerUserToken)

        client
            .post()
            .uri("/users/create")
            .bodyValue(newUser)
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .value("Location") {
                assertTrue(it.startsWith("/api/users/"))
            }

        val invitationEmail = InvitationEmail(email = "newuser@example.com")

        val tokenInput = UserCreateTokenInputModel("user1@gmail.com", "password1")

        // Generate a token for the authenticated user
        val tokenResult = client.post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(tokenInput)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(UserCreateTokenOutputModel::class.java)
            .returnResult()
            .responseBody!!

        // Use the token to send the invitation email
        client.post()
            .uri("/invitations/sendEmail")
            .header("Authorization", "Bearer ${tokenResult.token}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invitationEmail)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(String::class.java)
            .value {
                assertTrue(it.contains("Invitation sent successfully"))
            }
    }


}
