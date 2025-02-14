package pt.isel.daw

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import pt.isel.daw.model.ChannelInput
import kotlin.test.Test
import kotlin.test.assertIs

@SpringBootTest(
    properties = ["spring.main.allow-bean-definition-overriding=true"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class AbstractChannelControllerTest {
    @Autowired
    private lateinit var userService: UserService

    // Injected by the test environment
    @LocalServerPort
    var port: Int = 0

    @Autowired
    private lateinit var trxManager: TransactionManager

    @BeforeEach
    fun setUp() {
        trxManager.run {
            repoMessage.clear()
            repoInvitation.clear()
            repoChannel.clear()
            repoUser.clear()
        }
    }

    @Test
    fun `getAllChannels should return all channels`() {
        // Arrange
        val user =
            trxManager.run {
                val registerUserToken = repoInvitation.createRegistryToken()
                repoUser.createUser("user@gmail.com", "user", PasswordValidationInfo("1234"), registryToken = registerUserToken)
            }
        val channel1 =
            trxManager.run {
                repoChannel.createSingleChannel("channel1", user)
            }

        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        client
            .get()
            .uri("/channels")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(SingleChannel::class.java)
            .hasSize(1)
            .contains(channel1)
    }

    @Test
    fun `getChannel should return the channel with the given id`() {
        // Arrange
        val user =
            trxManager.run {
                val registerUserToken = repoInvitation.createRegistryToken()
                repoUser.createUser("user@gmail.com", "user", PasswordValidationInfo("1234"), registryToken = registerUserToken)
            }

        val channel1 =
            trxManager.run {
                repoChannel.createSingleChannel("channel1", user)
            }

        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        client
            .get()
            .uri("/channels/${channel1.id}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(SingleChannel::class.java)
            .isEqualTo(channel1)
    }

    @Test
    fun `createSingleChannel should return the created channel`() {
        // Arrange
        val registerUserToken = trxManager.run { repoInvitation.createRegistryToken() }
        val user = userService.createUser("user@gmail.com", "user", "1234", registryToken = registerUserToken)
        assertIs<Success<User>>(user)

        val token = userService.createToken(user.value.email, "1234")
        assertIs<Success<TokenExternalInfo>>(token)

        val channel = ChannelInput("channel1", SelectionType.SINGLE)

        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api/channels").build()

        client
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
    }

    @Test
    fun `createGroupChannel should return the created channel`() {
        val userPass = "1234"
        // Arrange
        val registerUserToken = trxManager.run { repoInvitation.createRegistryToken() }
        val user = userService.createUser("user@gmail.com", "user", userPass, registryToken = registerUserToken)
        assertIs<Success<User>>(user)

        val token = userService.createToken(user.value.email, userPass)
        assertIs<Success<TokenExternalInfo>>(token)

        val channel = ChannelInput("channel2", SelectionType.GROUP, Controls.PUBLIC)

        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        client
            .post()
            .uri("/channels/create")
            .bodyValue(channel)
            .header("Authorization", "Bearer ${token.value.tokenValue}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(String::class.java)
            .also {
                val channelId = trxManager.run { repoChannel.findAll().last().id }
                it.isEqualTo(channelId.toString())
            }
    }
}
