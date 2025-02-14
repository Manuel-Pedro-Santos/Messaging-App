package pt.isel.daw.mem

import pt.isel.daw.Channel
import pt.isel.daw.Message
import pt.isel.daw.RepositoryMessage
import pt.isel.daw.User
import java.time.LocalDateTime

class RepositaryMessageMem : RepositoryMessage {
    private val messages = mutableListOf<Message>()

    override fun findMessagesOfChannel(channel: Channel): List<Message> = messages.filter { it.channel == channel }

    override fun createMessage(
        channel: Channel,
        user: User,
        message: String,
    ): Message {
        val msg =
            Message(messages.size.toLong(), text = message, channel = channel, user = user, dateCreated = LocalDateTime.now())
        messages.add(msg)
        return msg
    }

    override fun clear() {
        messages.clear()
    }

    override fun findById(id: Int): Message? = messages.find { it.id.toInt() == id }

    override fun findAll(): List<Message> = messages

    override fun save(entity: Message) {
        messages.add(entity)
    }

    override fun deleteById(id: Int) {
        messages.removeIf { it.id.toInt() == id }
    }
}
