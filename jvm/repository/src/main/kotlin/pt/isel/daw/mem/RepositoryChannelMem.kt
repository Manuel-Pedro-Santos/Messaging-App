package pt.isel.daw.mem

import pt.isel.daw.Channel
import pt.isel.daw.Controls
import pt.isel.daw.GroupChannel
import pt.isel.daw.InvitationType
import pt.isel.daw.RepositoryChannel
import pt.isel.daw.SingleChannel
import pt.isel.daw.User

class RepositoryChannelMem : RepositoryChannel {
    private val channels = mutableListOf<Channel>()

    override fun findChannelByName(name: String): List<Channel> = channels.filter { it.name == name }

    override fun findChannelsByOwner(owner: User): List<Channel> = channels.filter { it.owner == owner }

    override fun findPublicChannels(): List<Channel> = channels.filter { it is GroupChannel && it.controls == Controls.PUBLIC }

    override fun createSingleChannel(
        channelName: String,
        owner: User,
    ): SingleChannel {
        val channel = SingleChannel(channels.size, channelName, owner = owner)
        channels.add(channel)
        return channel
    }

    override fun createGroupChannel(
        channelName: String,
        owner: User,
        controls: Controls,
    ): GroupChannel {
        val channel = GroupChannel(channels.size, channelName, owner = owner, controls = controls)
        channels.add(channel)
        return channel
    }

    override fun userJoinChannelSingle(
        channel: Channel,
        user: User,
    ): SingleChannel {
        val newChannel = channel.addUser(user)
        channels[channels.indexOf(channel)] = newChannel
        return newChannel as SingleChannel
    }

    override fun userLeaveChannelSingle(
        channel: Channel,
        user: User,
    ): SingleChannel {
        val newChannel = channel.removeUser(user)
        channels[channels.indexOf(channel)] = newChannel
        return newChannel as SingleChannel
    }

    override fun userJoinChannelGroup(
        channel: Channel,
        user: User,
        rules: InvitationType,
    ): GroupChannel {
        val newChannel = channel.addUser(user)
        channels[channels.indexOf(channel)] = newChannel
        return newChannel as GroupChannel
    }

    override fun findChannelsByUser(userId: Int): List<Channel>? {
        TODO("Not yet implemented")
    }

    override fun userLeaveChannelGroup(
        channel: Channel,
        user: User,
    ): GroupChannel {
        val newChannel = channel.removeUser(user)
        channels[channels.indexOf(channel)] = newChannel
        return newChannel as GroupChannel
    }

    override fun isUserInChannelAndCanSendMessages(
        channel: Channel,
        user: User,
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun isUserInChannel(
        channel: Channel,
        user: User,
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        channels.clear()
    }

    override fun findById(id: Int): Channel? = channels.find { it.id == id }

    override fun findAll(): List<Channel> = channels

    override fun save(entity: Channel) {
        channels.add(entity)
    }

    override fun deleteById(id: Int) {
        channels.removeIf { it.id == id }
    }
}
