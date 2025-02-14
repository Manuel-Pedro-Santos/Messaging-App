import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import pt.isel.daw.*
import java.time.LocalDateTime

class ChannelDomainTests {
    @Test
    fun test_channel_creation_test() {
        val user = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))

        val channel: Channel = SingleChannel(1, "channel", user)

        assert(channel.owner == user)

        assert(channel.id == 1)
    }

    @Test
    fun test_channel_add_user_test() {
        val user = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))

        val channel: Channel = SingleChannel(1, "channel", owner = user)

        val user2 = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))

        val channel2: Channel = channel.addUser(user2)

        assert((channel2 as SingleChannel).guest == user2)
    }

    @Test
    fun test_channel_remove_user_test() {
        val user = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))

        val user2 = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))

        val channel: Channel = SingleChannel(1, "channel", user)

        val action1: Channel = channel.addUser(user2)

        val action2: Channel = action1.removeUser(user2)

        assert((action2 as SingleChannel).guest == null)
    }

    @Test
    fun test_channel_creation_group_test() {
        val user = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))

        val channel: Channel = GroupChannel(1, "channel", user, controls = Controls.PUBLIC)

        assert(channel.owner == user)

        assert(channel.id == 1)
    }

    @Test
    fun test_channel_add_user_group_test() {
        val user = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))

        val user2 = User(2, email = "user2@gmail.com", "user", PasswordValidationInfo("pass"))

        val channel: Channel = GroupChannel(1, "channel", user, controls = Controls.PUBLIC)

        val channel2: Channel = channel.addUser(user2)

        assert((channel2 as GroupChannel).guests.contains(user2))
    }

    @Test
    fun test_channel_remove_user_group_test() {
        val user = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))

        val user2 = User(2, email = "user2@gmail.com", "user", PasswordValidationInfo("pass"))

        val channel: Channel = GroupChannel(1, "channel", user, controls = Controls.PUBLIC)

        val action1: Channel = channel.addUser(user2)

        val action2: Channel = action1.removeUser(user2)

        assert(!(action2 as GroupChannel).guests.contains(user2))
    }

    @Test
    fun test_message_creation_test() {
        val user = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))

        val channel: Channel = SingleChannel(1, "channel", user)

        val message = Message(1, "message", channel, user, LocalDateTime.now())

        assert(message.id.toInt() == 1)

        assert(message.text == "message")

        assert(message.channel == channel)

        assert(message.user == user)
    }


    @Test
    fun test_single_channel_add_user_when_guest_exists() {
        val user = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))
        val guest = User(2, email = "guest@gmail.com", "guest", PasswordValidationInfo("pass"))

        val channel: Channel = SingleChannel(1, "channel", user, guest = guest)

        val newGuest = User(3, email = "newguest@gmail.com", "newguest", PasswordValidationInfo("pass"))

        assertThrows(IllegalStateException::class.java) {
            channel.addUser(newGuest)
        }
    }

    @Test
    fun test_single_channel_remove_user_when_no_guest() {
        val user = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))

        val channel: Channel = SingleChannel(1, "channel", user)

        val guest = User(2, email = "guest@gmail.com", "guest", PasswordValidationInfo("pass"))

        assertThrows(IllegalStateException::class.java) {
            channel.removeUser(guest)
        }
    }

    @Test
    fun test_group_channel_add_multiple_users() {
        val user = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))
        val user2 = User(2, email = "user2@gmail.com", "user", PasswordValidationInfo("pass"))
        val user3 = User(3, email = "user3@gmail.com", "user", PasswordValidationInfo("pass"))

        val channel: Channel = GroupChannel(1, "channel", user, controls = Controls.PUBLIC)

        val channel2: Channel = channel.addUser(user2)
        val channel3: Channel = channel2.addUser(user3)

        assert((channel3 as GroupChannel).guests.contains(user2))
        assert(channel3.guests.contains(user3))
    }

    @Test
    fun test_single_channel_add_invitation() {
        val user = User(1, email = "user1@gmail.com", "user", PasswordValidationInfo("pass"))
        val guest = User(2, email = "guest@gmail.com", "guest", PasswordValidationInfo("pass"))

        val channel: Channel = SingleChannel(1, "channel", user)

        val invitation = Invitation(1, channel, guest, InvitationType.READ_WRITE)

        assert(invitation.id == 1)
    }
}
