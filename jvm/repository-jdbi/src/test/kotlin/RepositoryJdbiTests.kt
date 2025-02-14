import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.daw.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RepositoryJdbiTests {
    companion object {
        private fun runWithHandle(block: (Handle) -> Unit) = jdbi.useTransaction<Exception>(block)

        private val jdbi =
            Jdbi
                .create(
                    PGSimpleDataSource().apply {
                        setURL(Environment.getDbUrl())
                    },
                ).configureWithAppRequirements()
    }

    @BeforeEach
    fun clean() {
        runWithHandle { handle: Handle ->
            RepositoryMessageJdbi(handle).clear()
            RepositoryInvitationJdbi(handle).clear()
            RepositoryChannelJdbi(handle).clear()
            RepositoryUserJdbi(handle).clear()
        }
    }

    @Test
    fun `create single channel`() {
        runWithHandle { handle: Handle ->
            val repoCh = RepositoryChannelJdbi(handle)
            val repoUs = RepositoryUserJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUs.createUser("owner@gmail.com", "manu", PasswordValidationInfo("1234"), registryToken = regToken)
            val channel = repoCh.createSingleChannel("channel1", ow)

            assert(channel.name == "channel1")
            assertEquals(channel.owner, ow)
        }
    }

    @Test
    fun `create single channel with user`() {
        runWithHandle { handle: Handle ->
            val repoCh = RepositoryChannelJdbi(handle)
            val repoUs = RepositoryUserJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUs.createUser("owner2@gmail.com", "ow2", PasswordValidationInfo("1234"), registryToken = regToken)
            val user = repoUs.createUser("user1@gmail.com", "user1", PasswordValidationInfo("1234"), registryToken = regToken)

            val channel = repoCh.createSingleChannel("channel2", ow)
            repoCh.userJoinChannelSingle(channel, user)

            val ch = repoCh.findById(channel.id)
            assertIs<SingleChannel>(ch)

            assert(ch.name == "channel2")

            assert(ch.guest?.id == user.id)
            assert(ch.guest?.email == user.email)
            assert(ch.guest?.username == user.username)

            assert(ch.owner.id == ow.id)
            assert(ch.owner.email == ow.email)
            assert(ch.owner.username == ow.username)
        }
    }

    @Test
    fun `create single channel join a guest and then leave`() {
        runWithHandle { handle: Handle ->
            val repoUs = RepositoryUserJdbi(handle)
            val repoCh = RepositoryChannelJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUs.createUser("owner@gmail.com", "owner", PasswordValidationInfo("1234"), registryToken = regToken)
            val user = repoUs.createUser("user@gmail.com", "user", PasswordValidationInfo("1234"), registryToken = regToken)
            val channel = repoCh.createSingleChannel("channel", ow)

            repoCh.userJoinChannelSingle(channel, user)

            val ch = repoCh.findById(channel.id)
            assertIs<SingleChannel>(ch)

            assert(ch.guest?.id == user.id)
            assert(ch.guest?.email == user.email)
            assert(ch.guest?.username == user.username)

            repoCh.userLeaveChannelSingle(channel, user)

            val ch2 = repoCh.findById(channel.id)
            assertIs<SingleChannel>(ch2)

            assert(ch2.guest == null)
        }
    }

    @Test
    fun `create single channel with user and message`() {
        runWithHandle { handle: Handle ->
            val repoCh = RepositoryChannelJdbi(handle)
            val repoUs = RepositoryUserJdbi(handle)
            val repoMsg = RepositoryMessageJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUs.createUser("owner3@gmail.com", "ow3", PasswordValidationInfo("1234"), registryToken = regToken)
            val user = repoUs.createUser("user2@gmail.com", "user2", PasswordValidationInfo("1234"), registryToken = regToken)

            val channel = repoCh.createSingleChannel("channel4", ow)
            repoCh.userJoinChannelSingle(channel, user)

            val message = repoMsg.createMessage(channel, user, "message1")

            val ch = repoCh.findById(channel.id)
            assertIs<SingleChannel>(ch)

            val msg = repoMsg.findById(message.id.toInt())
            // assert(msg?.id == 1)
            assert(msg?.text == "message1")
            assert(msg?.channel?.id == channel.id)
            assert(msg?.user?.id == user.id)
        }
    }

    @Test
    fun `Create Group channel and add a player to it`() {
        runWithHandle { handle: Handle ->
            val repoChannel = RepositoryChannelJdbi(handle)
            val repoUser = RepositoryUserJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUser.createUser("owner4@gmail.com", "ow3", PasswordValidationInfo("1234"), registryToken = regToken)
            val ch = repoChannel.createGroupChannel("channel3", owner = ow, controls = Controls.PUBLIC)
            val us = repoUser.createUser("user3@gmail.com", "user3", PasswordValidationInfo("1234"), registryToken = regToken)

            repoChannel.userJoinChannelGroup(ch, us, InvitationType.READ_WRITE)

            val channel = repoChannel.findById(ch.id)
            assertIs<GroupChannel>(channel)

            assert(channel.name == "channel3")
            assert(channel.owner.id == ow.id)
            assert(channel.owner.email == ow.email)
            assert(channel.owner.username == ow.username)

            assert(channel.guests.size == 1)
            assert(channel.guests[0].id == us.id)
            assert(channel.guests[0].email == us.email)
            assert(channel.guests[0].username == us.username)
        }
    }

    @Test
    fun `Create Group channel and add a player to it with read only permission`() {
        runWithHandle { handle: Handle ->
            val repoChannel = RepositoryChannelJdbi(handle)
            val repoUser = RepositoryUserJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUser.createUser("owner1@gmail.com", "ow1", PasswordValidationInfo("1234"), registryToken = regToken)
            val ch = repoChannel.createGroupChannel("channel1", owner = ow, controls = Controls.PUBLIC)
            val us = repoUser.createUser("user1@gmail.com", "user1", PasswordValidationInfo("1234"), registryToken = regToken)

            repoChannel.userJoinChannelGroup(ch, us, InvitationType.READ_ONLY)

            val channel = repoChannel.findById(ch.id)
            assertIs<GroupChannel>(channel)

            assert(channel.name == "channel1")
            assert(channel.owner.id == ow.id)
            assert(channel.owner.email == ow.email)
            assert(channel.owner.username == ow.username)
        }
    }

    @Test
    fun `Create Group channel and add a player to it with read only permission and send a message`() {
        runWithHandle { handle: Handle ->
            val repoChannel = RepositoryChannelJdbi(handle)
            val repoUser = RepositoryUserJdbi(handle)
            val repoMessage = RepositoryMessageJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUser.createUser("owner@gmail.com", "ow", PasswordValidationInfo("1234"), registryToken = regToken)
            val ch = repoChannel.createGroupChannel("channel", owner = ow, controls = Controls.PUBLIC)
            val us = repoUser.createUser("user1@gmail.com", "user1", PasswordValidationInfo("1234"), registryToken = regToken)

            repoChannel.userJoinChannelGroup(ch, us, InvitationType.READ_WRITE)

            val message = repoMessage.createMessage(ch, us, "message1")

            val channel = repoChannel.findById(ch.id)
            assertIs<GroupChannel>(channel)

            val msg = repoMessage.findById(message.id.toInt())
            assert(msg?.text == "message1")
            assert(msg?.channel?.id == ch.id)
            assert(msg?.user?.id == us.id)

            assert(channel.name == "channel")
            assert(channel.owner.id == ow.id)
            assert(channel.owner.email == ow.email)
            assert(channel.owner.username == ow.username)

            assert(channel.guests.size == 1)
            assert(channel.guests[0].id == us.id)
            assert(channel.guests[0].email == us.email)
            assert(channel.guests[0].username == us.username)
        }
    }

    @Test
    fun `Create Group Channel and see user Leave`() {
        runWithHandle { handle: Handle ->
            val repoChannel = RepositoryChannelJdbi(handle)
            val repoUser = RepositoryUserJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUser.createUser("owner@gmail.com", "ow", PasswordValidationInfo("1234"), registryToken = regToken)
            val ch = repoChannel.createGroupChannel("channel", owner = ow, controls = Controls.PUBLIC)
            val us = repoUser.createUser("user@gmail.com", "user", PasswordValidationInfo("1234"), registryToken = regToken)

            repoChannel.userJoinChannelGroup(ch, us, InvitationType.READ_WRITE)

            val channel = repoChannel.findById(ch.id)
            assertIs<GroupChannel>(channel)

            assert(channel.guests.size == 1)

            repoChannel.userLeaveChannelGroup(ch, us)

            val channel2 = repoChannel.findById(ch.id)
            assertIs<GroupChannel>(channel2)

            assert(channel2.guests.isEmpty())
        }
    }

    @Test
    fun `send Invitation to a user`() {
        runWithHandle { handle: Handle ->
            val repoChannel = RepositoryChannelJdbi(handle)
            val repoUser = RepositoryUserJdbi(handle)
            val repoInvitation = RepositoryInvitationJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUser.createUser("owner@gmail.com", "ow", PasswordValidationInfo("1234"), registryToken = regToken)
            val ch = repoChannel.createGroupChannel("channel", owner = ow, controls = Controls.PUBLIC)
            val us = repoUser.createUser("user@gmail.com", "user", PasswordValidationInfo("1234"), registryToken = regToken)

            val invitation = repoInvitation.sendInvitationAuthenticated(ch, us, InvitationType.READ_WRITE)

            val inv = repoInvitation.findById(invitation.id)

            assert(inv?.channel?.id == ch.id)
            assert(inv?.guest?.id == us.id)
            assert(inv?.invitationType == InvitationType.READ_WRITE)

            val invitations = repoInvitation.findInvitationsForUser(us)
            assert(invitations?.size == 1)
            assert(invitations?.get(0)?.id == invitation.id)

            val invitations2 = repoInvitation.findInvitationsForChannel(ch)
            assert(invitations2?.size == 1)
        }
    }

    @Test
    fun `Find a Channel by Name`() {
        runWithHandle { handle: Handle ->
            val repoChannel = RepositoryChannelJdbi(handle)
            val repoUser = RepositoryUserJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUser.createUser("owner@gmail.com", "ow", PasswordValidationInfo("1234"), registryToken = regToken)
            val ch = repoChannel.createGroupChannel("channel", owner = ow, controls = Controls.PUBLIC)

            val channel = repoChannel.findChannelByName("channel").first()

            assert(channel.id == ch.id)
        }
    }

    @Test
    fun `Find Channels by Owner`() {
        runWithHandle { handle: Handle ->
            val repoChannel = RepositoryChannelJdbi(handle)
            val repoUser = RepositoryUserJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUser.createUser("owner@gmail.com", "ow", PasswordValidationInfo("1234"), registryToken = regToken)
            val ch = repoChannel.createGroupChannel("channel", owner = ow, controls = Controls.PUBLIC)
            val ch2 = repoChannel.createGroupChannel("channel2", owner = ow, controls = Controls.PUBLIC)

            val channels = repoChannel.findChannelsByOwner(ow)

            assert(channels.size == 2)
            assert(channels[0].id == ch.id)
            assert(channels[1].id == ch2.id)
        }
    }

    @Test
    fun `Find all Public Channels`() {
        runWithHandle { handle: Handle ->
            val repoChannel = RepositoryChannelJdbi(handle)
            val repoUser = RepositoryUserJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUser.createUser("owner@gmail.com", "ow", PasswordValidationInfo("1234"), registryToken = regToken)
            val ch = repoChannel.createGroupChannel("channel", owner = ow, controls = Controls.PUBLIC)
            val ch2 = repoChannel.createGroupChannel("channel2", owner = ow, controls = Controls.PUBLIC)
            val ch3 = repoChannel.createGroupChannel("channel3", owner = ow, controls = Controls.PRIVATE)

            val channels = repoChannel.findPublicChannels()

            println(channels)
            assert(channels.size == 2)
            assert(channels[0].name == ch.name)
            assert(channels[1].name == ch2.name)
            assert(channels.none { it.id == ch3.id })
        }
    }

    @Test
    fun `find All channels`() {
        runWithHandle { handle: Handle ->
            val repoChannel = RepositoryChannelJdbi(handle)
            val repoUser = RepositoryUserJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUser.createUser("owner@gmail.com", "ow", PasswordValidationInfo("1234"), registryToken = regToken)
            repoChannel.createGroupChannel("channel", owner = ow, controls = Controls.PUBLIC)
            repoChannel.createGroupChannel("channel2", owner = ow, controls = Controls.PUBLIC)
            repoChannel.createGroupChannel("channel3", owner = ow, controls = Controls.PRIVATE)
            repoChannel.createSingleChannel("channel4", owner = ow)
            repoChannel.createSingleChannel("channel5", owner = ow)

            val channels = repoChannel.findAll()

            assert(channels.size == 5)
        }
    }

    @Test
    fun `Update a singleChannel`() {
        runWithHandle { handle: Handle ->
            val repoChannel = RepositoryChannelJdbi(handle)
            val repoUser = RepositoryUserJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUser.createUser("owner@gmail.com", "ow", PasswordValidationInfo("1234"), registryToken = regToken)
            val us = repoUser.createUser("user@gmail.com", "user", PasswordValidationInfo("1234"), registryToken = regToken)
            val ch = repoChannel.createSingleChannel("channel", owner = ow)
            val newCh = SingleChannel(ch.id, "newChannel", ow, guest = us)

            assert(ch.name == "channel")
            assert(ch.owner.id == ow.id)
            assert(ch.guest == null)

            repoChannel.save(newCh)

            val ch2 = repoChannel.findById(ch.id)
            assertIs<SingleChannel>(ch2)

            assert(ch2.name == "newChannel")
            assert(ch2.owner.id == ow.id)
            assert(ch2.guest?.id == us.id)
        }
    }

    @Test
    fun `Update a GroupChannel`() {
        runWithHandle { handle: Handle ->
            val repoChannel = RepositoryChannelJdbi(handle)
            val repoUser = RepositoryUserJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUser.createUser("owner@gmail.com", "ow", PasswordValidationInfo("1234"), registryToken = regToken)
            val us = repoUser.createUser("user@gmail.com", "user", PasswordValidationInfo("1234"), registryToken = regToken)
            val ch = repoChannel.createGroupChannel("channel", owner = ow, controls = Controls.PUBLIC)
            val newCh = GroupChannel(ch.id, "newChannel", ow, controls = Controls.PRIVATE, guests = listOf(us))

            assert(ch.name == "channel")
            assert(ch.owner.id == ow.id)
            assert(ch.guests.isEmpty())

            repoChannel.save(newCh)

            val ch2 = repoChannel.findById(ch.id)
            assertIs<GroupChannel>(ch2)

            assert(ch2.name == "newChannel")
            assert(ch2.owner.id == ow.id)
        }
    }

    @Test
    fun `Delete a Channel`() {
        runWithHandle { handle: Handle ->
            val repoChannel = RepositoryChannelJdbi(handle)
            val repoUser = RepositoryUserJdbi(handle)
            val repoInv = RepositoryInvitationJdbi(handle)

            val regToken = repoInv.createRegistryToken()

            val ow = repoUser.createUser("owner@gmail.com", "ow", PasswordValidationInfo("1234"), registryToken = regToken)
            val ch = repoChannel.createGroupChannel("channel", owner = ow, controls = Controls.PUBLIC)

            val allChannels = repoChannel.findAll()
            assert(allChannels.size == 1)

            repoChannel.deleteById(ch.id)

            val allChannels2 = repoChannel.findAll()
            assert(allChannels2.isEmpty())
        }
    }
}
