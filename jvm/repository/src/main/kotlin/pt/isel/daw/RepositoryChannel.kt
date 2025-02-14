package pt.isel.daw

interface RepositoryChannel : Repository<Channel> {
    fun findChannelByName(name: String): List<Channel>

    fun findChannelsByOwner(owner: User): List<Channel>?

    fun findPublicChannels(): List<Channel>?

    fun createSingleChannel(
        channelName: String,
        owner: User,
    ): SingleChannel

    fun createGroupChannel(
        channelName: String,
        owner: User,
        controls: Controls,
    ): GroupChannel

    fun userJoinChannelSingle(
        channel: Channel,
        user: User,
    ): SingleChannel

    fun userLeaveChannelSingle(
        channel: Channel,
        user: User,
    ): SingleChannel

    fun userJoinChannelGroup(
        channel: Channel,
        user: User,
        rules: InvitationType,
    ): GroupChannel

    fun findChannelsByUser(userId: Int): List<Channel>?

    fun userLeaveChannelGroup(
        channel: Channel,
        user: User,
    ): GroupChannel

    fun isUserInChannelAndCanSendMessages(
        channel: Channel,
        user: User,
    ): Boolean

    fun isUserInChannel(
        channel: Channel,
        user: User,
    ): Boolean

    fun clear()
}
