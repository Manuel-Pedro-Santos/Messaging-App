import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.daw.*
import pt.isel.daw.mem.TransactionManagerMem
import java.util.stream.Stream
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

fun newTokenValidationData() = "token-${abs(Random.nextLong())}"

class ChannelControllerTest {
    companion object {
        private val jdbi =
            Jdbi
                .create(
                    PGSimpleDataSource().apply {
                        setURL(Environment.getDbUrl())
                    },
                ).configureWithAppRequirements()

        @JvmStatic
        fun transactionManagers(): Stream<TransactionManager> =
            Stream.of(
                TransactionManagerMem().also { cleanup(it) },
                TransactionManagerJdbi(jdbi).also { cleanup(it) },
            )

        private fun cleanup(trxManager: TransactionManager) {
            trxManager.run {
                repoChannel.clear()
                repoUser.clear()
                repoMessage.clear()
                repoInvitation.clear()
            }
        }

        private fun createUserService(
            trxManager: TransactionManager,
            testClock: TestClock,
            tokenTtl: Duration = 30.days,
            tokenRollingTtl: Duration = 30.minutes,
            maxTokensPerUser: Int = 3,
        ) = UserService(
            trxManager,
            UserUtils(
                BCryptPasswordEncoder(),
                Sha256TokenEncoder(),
                UsersDomainConfig(
                    tokenSizeInBytes = 256 / 8,
                    tokenTtl = tokenTtl,
                    tokenRollingTtl,
                    maxTokensPerUser = maxTokensPerUser,
                ),
            ),
            testClock,
        )
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getAllEvents should return a list of events`(trxManager: TransactionManager) {
        // Arrange
        val rose =
            trxManager.run {
                val regToken = repoInvitation.createRegistryToken()
                repoUser.createUser(
                    "rose@gmail.com",
                    "Rose Mary",
                    PasswordValidationInfo(newTokenValidationData()),
                    registryToken = regToken,
                )
            }

        trxManager.run { repoChannel.createSingleChannel("Chat1", rose) }
        trxManager.run {
            repoChannel.createGroupChannel(
                "Status Meeting",
                rose,
                Controls.PUBLIC,
            )
        }
        val controllerChannel = ChannelController(ChannelService(trxManager))
        val resp = controllerChannel.getChannels()
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals(2, resp.body?.size)
    }
}
