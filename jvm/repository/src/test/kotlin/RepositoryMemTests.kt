import pt.isel.daw.Controls
import pt.isel.daw.GroupChannel
import pt.isel.daw.PasswordValidationInfo
import pt.isel.daw.SingleChannel
import pt.isel.daw.mem.RepositoryChannelMem
import pt.isel.daw.mem.RepositoryUserMem
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoryMemTests {
    private val repoUsers: RepositoryUserMem =
        RepositoryUserMem().also {
            // User(0, "user1", "user1@gmail", "123456")
            it.createUser(
                email = "user1@gmail.com",
                username = "user1",
                hashedPass = PasswordValidationInfo("123456"),
                registryToken = "sdasdasdadsadsda",
            )
        }

    private val channelRepo =
        RepositoryChannelMem().also {
            // SingleChannel(0, "channel0", User(0, "user1", "user1@gmail", "123456"), Controlls.PUBLIC)
            it.createSingleChannel(
                channelName = "channel0",
                owner = repoUsers.findById(0)!!,
            )
        }

    @Test
    fun `create a new user`() {
        val user =
            repoUsers.createUser(
                email = "user2@gmail.com",
                username = "user2",
                hashedPass = PasswordValidationInfo("123456"),
                registryToken = "sdasdasdadsadsda",
            )
        assertEquals(1, user.id)
        assertEquals("user2", user.username)
        assertEquals("user2@gmail.com", user.email)
        assertEquals("123456", user.passwordValidationInfo.validationInfo)

        println(repoUsers.findAll())
    }

    @Test
    fun `create a new Single channel`() {
        val newChannel =
            channelRepo.createSingleChannel(
                channelName = "channel1",
                owner = repoUsers.findById(0)!!,
            )

        assertEquals(1, newChannel.id)
        assertEquals("channel1", newChannel.name)

        println(channelRepo.findAll())
    }

    @Test
    fun `create a new Group channel`() {
        val newChannel =
            channelRepo.createGroupChannel(
                channelName = "channel2",
                owner = repoUsers.findById(0)!!,
                controls = Controls.PUBLIC,
            )

        assertEquals(1, newChannel.id)
        assertEquals("channel2", newChannel.name)
        assertEquals(Controls.PUBLIC, newChannel.controls)

        println(channelRepo.findAll())
    }

    @Test
    fun `find channel by name`() {
        val newChannel =
            channelRepo.createSingleChannel(
                channelName = "channel3",
                owner = repoUsers.findById(0)!!,
            )

        val channel = channelRepo.findChannelByName("channel3").first()
        assertEquals(newChannel, channel)
    }

    @Test
    fun `join a player to a Single channel`() {
        val newChannel = channelRepo.findById(0)!!
        val result = newChannel.addUser(repoUsers.findById(0)!!) as SingleChannel
        assertEquals(0, result.guest!!.id)

        println(channelRepo.findAll())
    }

    @Test
    fun `join a player to a Group channel`() {
        val newChannel =
            channelRepo.createGroupChannel(
                channelName = "channel4",
                owner = repoUsers.findById(0)!!,
                controls = Controls.PUBLIC,
            )

        val user =
            repoUsers.createUser(
                email = "user2@gmail.com",
                username = "user2",
                hashedPass = PasswordValidationInfo("123456"),
                registryToken = "sdasdasdadsadsda",
            )

        val result = newChannel.addUser(user) as GroupChannel

        println(result.guests)
        assertEquals(1, result.guests.size)
        assertEquals(1, result.guests[0].id)

        println(channelRepo.findAll())
    }

    @Test
    fun `remove a player from a Single channel`() {
        val channel = channelRepo.findById(0)!!
        val user =
            repoUsers.createUser(
                email = "user1@gmail.com",
                username = "user1",
                hashedPass = PasswordValidationInfo("123456"),
                registryToken = "sdasdasdadsadsda",
            )

        val newChannel = channel.addUser(user) as SingleChannel

        println(newChannel) // Verificar se o user foi adicionado ao canal

        val result = newChannel.removeUser(repoUsers.findById(1)!!) as SingleChannel
        assertEquals(null, result.guest)

        println(result) // Verificar se o user foi removido do canal
    }

    @Test
    fun `remove a player from a Group channel`() {
        val newChannel =
            channelRepo.createGroupChannel(
                channelName = "channel5",
                owner = repoUsers.findById(0)!!,
                controls = Controls.PUBLIC,
            )

        val user =
            repoUsers.createUser(
                email = "user1@gmail.com",
                username = "user1",
                hashedPass = PasswordValidationInfo("123456"),
                registryToken = "sdasdasdadsadsda",
            )

        val result = newChannel.addUser(user) as GroupChannel

        println(result.guests) // Verificar se o user foi adicionado ao canal

        val result2 = result.removeUser(repoUsers.findById(1)!!) as GroupChannel

        println(result2.guests) // Verificar se o user foi removido do canal

        assertEquals(0, result2.guests.size)
    }

    @Test
    fun `find channel by id`() {
        val newChannel =
            channelRepo.createSingleChannel(
                channelName = "channel6",
                owner = repoUsers.findById(0)!!,
            )

        val channel = channelRepo.findById(1)
        assertEquals(newChannel, channel)
    }
}
