package pt.isel.daw

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.daw.mem.TransactionManagerMem
import pt.isel.daw.model.UserInput
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
class UserControllerTest {
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
    fun `can create an user, obtain a token, and access user home, and logout`(trxManager: TransactionManager) {
        val controllerUser = UserController(createUserService(trxManager, TestClock()))
        val regToken =
            trxManager.run {
                repoInvitation.createRegistryToken()
            }

        // given: a user
        val email = "john@rambo.vcom"
        val name = "John Rambo"
        val password = "badGuy"

        // when: creating an user
        // then: the response is a 201 with a proper Location header
        controllerUser.createUser(UserInput(email, name, password, regToken)).let { resp ->
            assertEquals(HttpStatus.CREATED, resp.statusCode)
            val location = resp.headers.getFirst(HttpHeaders.LOCATION)
            assertNotNull(location)
            assertTrue(location.startsWith("/api/users"))
            location.split("/").last().toInt()
        }
    }
}
