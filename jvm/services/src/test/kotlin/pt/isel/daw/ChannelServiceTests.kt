package pt.isel.daw

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.daw.mem.TransactionManagerMem
import java.util.stream.Stream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class ChannelServiceTests {
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
                repoMessage.clear()
                repoInvitation.clear()
                repoChannel.clear()
                repoUser.clear()
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
    fun `create single channel`(txr: TransactionManager) {
        val channelService = ChannelService(txr)
        val userService = createUserService(txr, TestClock())

        val regToken = userService.createRegistryToken()

        val owner = userService.createUser("owner@gmail.com", "owner", "1234", registryToken = regToken) as Success<User>

        channelService.createSingleChannel("Single Channel", owner.value.id).also {
            val ch = it as Success<SingleChannel>

            assertEquals("Single Channel", ch.value.name)
            assertEquals(owner.value.id, ch.value.owner.id)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create group channel`(txr: TransactionManager) {
        val channelService = ChannelService(txr)
        val userService = createUserService(txr, TestClock())
        val regToken = userService.createRegistryToken()

        val owner =
            userService.createUser("owner@gmail.com", "owner", "1234", registryToken = regToken) as Success<User>

        channelService.createGroupChannel("Group Channel", owner = owner.value.id, controls = Controls.PUBLIC).also {
            val ch = it as Success<GroupChannel>

            assertEquals("Group Channel", ch.value.name)
            assertEquals(owner.value.id, ch.value.owner.id)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `send invitation to user`(txr: TransactionManager) {
        val channelService = ChannelService(txr)
        val userService = createUserService(txr, TestClock())

        val regToken = userService.createRegistryToken()

        val owner =
            userService.createUser("owner@gmail.com", "owner", "1234", registryToken = regToken).let {
                check(it is Success<User>)
                it
            }

        val guest =
            userService.createUser("user@gmail.com", "user", "1234", registryToken = regToken).let {
                check(it is Success<User>)
                it
            }

        val channel =
            channelService.createSingleChannel("NEW NAME", owner.value.id).let {
                check(it is Success<SingleChannel>)
                it
            }

        userService
            .sendInvitationToAlreadyAuth(owner.value.id, guest.value.id, channel.value.id, InvitationType.READ_ONLY)
            .let {
                check(it is Success<Invitation>)
                it
            }.also { invitation ->

                assertEquals(channel.value.id, invitation.value.channel.id)
                assertEquals(guest.value.id, invitation.value.guest.id)
                assertEquals(InvitationType.READ_ONLY, invitation.value.invitationType)
            }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `remove invitation`(txr: TransactionManager) {
        val channelService = ChannelService(txr)
        val userService = createUserService(txr, TestClock())

        val regToken = userService.createRegistryToken()

        val owner = userService.createUser("owner@gmail.com", "owner", "1234", registryToken = regToken) as Success<User>

        val guest = userService.createUser("user@gmail.com", "user", "1234", registryToken = regToken) as Success<User>

        val channel = channelService.createSingleChannel("Single Channel", owner.value.id) as Success<Channel>

        val invitation =
            userService.sendInvitationToAlreadyAuth(
                owner.value.id,
                guest.value.id,
                channel.value.id,
                InvitationType.READ_ONLY,
            ) as Success<Invitation>

        userService.removeInvitation(invitation.value.id).also {
            val removed = it as Success<Unit>

            assertEquals(Unit, removed.value)
        }
    }
}
