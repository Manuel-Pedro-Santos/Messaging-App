package pt.isel.daw

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.daw.mem.TransactionManagerMem
import java.util.stream.Stream
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class UserServiceTests {
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

        val owner = userService.createUser("owner@gmail.com", "owner", "1234", registryToken = regToken)
        assertIs<Success<User>>(owner)

        channelService.createSingleChannel("channel", owner.value.id).also {
            val ch = it as Success<SingleChannel>

            assertEquals("channel", ch.value.name)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create group channel`(txr: TransactionManager) {
        val channelService = ChannelService(txr)
        val userService = createUserService(txr, TestClock())

        val regToken = userService.createRegistryToken()

        val owner = userService.createUser("owner@gmail.com", "owner", "1234", registryToken = regToken)
        assertIs<Success<User>>(owner)

        channelService.createGroupChannel("channel", owner.value.id, controls = Controls.PRIVATE).also {
            val ch = it as Success<GroupChannel>

            assertEquals("channel", ch.value.name)
            assertEquals(owner.value, ch.value.owner)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `leave single channel`(txr: TransactionManager) {
        val channelService = ChannelService(txr)
        val userService = createUserService(txr, TestClock())

        val regToken = userService.createRegistryToken()

        val owner =
            userService.createUser("owner@gmail.com", "owner", "1234", registryToken = regToken).let {
                assertIs<Success<User>>(it)
                it.value
            }

        val user =
            userService.createUser("user@gmail.com", "user", "1234", registryToken = regToken).let {
                assertIs<Success<User>>(it)
                it.value
            }

        val singleChannel =
            channelService.createSingleChannel("channel", owner.id).let {
                assertIs<Success<SingleChannel>>(it)
                it.value
            }

        channelService.joinChannel(singleChannel.id, user.id, InvitationType.READ_ONLY).let {
            assertIs<Success<Channel>>(it)
        }

        channelService.leaveChannel(singleChannel.id, user.id).let {
            assertIs<Success<Channel>>(it)
        }

        val getChannel = channelService.getChannelByName(singleChannel.name)
        assertIs<Success<Channel>>(getChannel)

        val ch = getChannel.value as SingleChannel
        assertEquals(null, ch.guest)
        assertEquals(owner, ch.owner)
        assertEquals("channel", ch.name)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `leave group channel`(txr: TransactionManager) {
        val channelService = ChannelService(txr)
        val userService = createUserService(txr, TestClock())

        val regToken = userService.createRegistryToken()

        val owner =
            userService.createUser("owner@gmail.com", "owner", "1234", registryToken = regToken).let {
                assertIs<Success<User>>(it)
                it.value
            }

        val user =
            userService.createUser("user@gmail.com", "user", "1234", registryToken = regToken).let {
                assertIs<Success<User>>(it)
                it.value
            }

        val groupChannel =
            channelService.createGroupChannel("channel", owner.id, controls = Controls.PRIVATE).let {
                assertIs<Success<GroupChannel>>(it)
                it.value
            }

        channelService.joinChannel(groupChannel.id, user.id, InvitationType.READ_ONLY).let {
            assertIs<Success<Channel>>(it)
        }

        val getChannel1 = channelService.getChannelByName(groupChannel.name )
        assertIs<Success<Channel>>(getChannel1)

        val ch1 = getChannel1.value as GroupChannel
        assertEquals(1, ch1.guests.size)
        assertEquals(owner, ch1.owner)
        assertEquals("channel", ch1.name)

        channelService.leaveChannel(groupChannel.id, user.id).let {
            assertIs<Success<Channel>>(it)
        }

        val getChannel = channelService.getChannelByName(groupChannel.name)
        assertIs<Success<Channel>>(getChannel)

        val ch = getChannel.value as GroupChannel
        assertEquals(0, ch.guests.size)
        assertEquals(owner, ch.owner)
        assertEquals("channel", ch.name)
    }
}
