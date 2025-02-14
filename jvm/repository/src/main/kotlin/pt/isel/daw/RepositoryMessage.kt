package pt.isel.daw

interface RepositoryMessage : Repository<Message> {
    fun findMessagesOfChannel(channel: Channel): List<Message>?

    fun createMessage(
        channel: Channel,
        user: User,
        message: String,
    ): Message

    fun clear()
}
