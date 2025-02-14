package pt.isel.daw

import jakarta.annotation.PreDestroy
import jakarta.inject.Named
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

sealed class ChannelError {
    data object ChannelNotFound : ChannelError()
    data object ChannelNameAlreadyInUse : ChannelError()
    data object UserNotFound : ChannelError()
    data object UserAlreadyExists : ChannelError()
    data object InvitationNotFound : ChannelError()
}

@Named
class ChannelService(
    private val transactionManager: TransactionManager,
) {
    private val listeners = mutableMapOf<Int, MutableList<UpdatedChannelEmitter>>()
    private val lock = ReentrantLock()
    private val scheduler: ScheduledExecutorService =
        Executors.newScheduledThreadPool(1).also {
            it.scheduleAtFixedRate({ keepAlive() }, 3, 5, TimeUnit.SECONDS)
        }
    private var currentId: Long = 0

    @PreDestroy
    fun shutdown() {
        scheduler.shutdown()
    }

    fun addEmitter(channelId: Int, emitter: UpdatedChannelEmitter) = lock.withLock {
        listeners.computeIfAbsent(channelId) { mutableListOf() }.add(emitter)
        emitter.onCompletion { removeEmitter(channelId, emitter) }
        emitter.onError { removeEmitter(channelId, emitter) }
    }

    private fun removeEmitter(channelId: Int, emitter: UpdatedChannelEmitter) = lock.withLock {
        listeners[channelId]?.remove(emitter)
    }

    private fun keepAlive() = lock.withLock {
        val signal = UpdatedChannel.KeepAlive(Clock.System.now())
        listeners.values.flatten().forEach {
            try {
                it.emit(signal)
            } catch (ex: Exception) {
                // Handle exception
            }
        }
    }

    fun notifyChannel(channelId: Int, message: Message) {
        val signal = UpdatedChannel.Message(message.id.toLong(), message)
        listeners[channelId]?.forEach {
            try {
                it.emit(signal)
            } catch (ex: Exception) {
                // Handle exception
            }
        }
    }

    fun isUserInChannel(
        channelId: Int,
        userId: Int,
    ): Boolean = transactionManager.run {
        val channel = repoChannel.findById(channelId) ?: return@run false
        val user = repoUser.findById(userId) ?: return@run false
        repoChannel.isUserInChannel(channel, user)
    }

    private fun sendEventToAll(
        ch: Channel,
        signal: UpdatedChannel,
    ) {
        listeners[ch.id]?.forEach {
            try {
                it.emit(signal)
            } catch (ex: Exception) {
                logger.info("Exception while sending Message signal - {}", ex.message)
            }
        }
    }

    fun getChannelsByUser(userId: Int): List<Channel>? = transactionManager.run {
        repoChannel.findChannelsByUser(userId)
    }

    fun createSingleChannel(
        name: String,
        ownerId: Int,
    ): Either<ChannelError, SingleChannel> =
        transactionManager.run {
            val checkName = repoChannel.findChannelByName(name)
            if (checkName.isNotEmpty()) return@run failure(ChannelError.ChannelNameAlreadyInUse)
            val owner: User = repoUser.findById(ownerId) ?: return@run failure(ChannelError.UserNotFound)
            val channel: SingleChannel = repoChannel.createSingleChannel(name, owner)
            success(channel)
        }

    fun createGroupChannel(
        name: String,
        owner: Int,
        controls: Controls,
    ): Either<ChannelError, GroupChannel> =
        transactionManager.run {
            val ownerUser: User = repoUser.findById(owner) ?: return@run failure(ChannelError.UserNotFound)
            val checkName = repoChannel.findChannelByName(name)
            if (checkName.isNotEmpty()) return@run failure(ChannelError.ChannelNameAlreadyInUse)
            val channel: GroupChannel = repoChannel.createGroupChannel(name, ownerUser, controls)
            return@run success(channel)
        }

    fun joinChannel(
        channelId: Int,
        userId: Int,
        type: InvitationType,
    ): Either<ChannelError, Channel> =
        transactionManager.run {
            val checkChannelExistance: Channel =
                repoChannel.findById(channelId) ?: return@run failure(ChannelError.ChannelNotFound)
            val checkUserExistance = repoUser.findById(userId) ?: return@run failure(ChannelError.UserNotFound)

            when (checkChannelExistance) {
                is SingleChannel -> {
                    val channel: Channel = repoChannel.userJoinChannelSingle(checkChannelExistance, checkUserExistance)
                    //val message = Message(++currentId, "User joined the channel",channel,checkUserExistance, dateCreated = LocalDateTime.now())
                    //sendEventToAll(checkChannelExistance, UpdatedChannel.Message(message.id.toLong(), message))
                    success(channel)
                }
                is GroupChannel -> {
                    val channel: Channel = repoChannel.userJoinChannelGroup(checkChannelExistance, checkUserExistance, type)
                    //val message = Message(++currentId, "User joined the channel",channel,checkUserExistance, dateCreated = LocalDateTime.now())
                    //sendEventToAll(checkChannelExistance, UpdatedChannel.Message(message.id, message))
                    success(channel)
                }
            }
        }

    fun leaveChannel(
        channelId: Int,
        userId: Int,
    ): Either<ChannelError, Channel> =
        transactionManager.run {
            val checkChannelExistance =
                repoChannel.findById(channelId) ?: return@run failure(ChannelError.ChannelNotFound)
            val checkUserExistance = repoUser.findById(userId) ?: return@run failure(ChannelError.UserNotFound)
            val channel: Channel =
                if (checkChannelExistance is SingleChannel) {
                    repoChannel.userLeaveChannelSingle(checkChannelExistance, checkUserExistance)
                } else {
                    repoChannel.userLeaveChannelGroup(checkChannelExistance, checkUserExistance)
                }
            success(channel)
        }

    fun getChannels(): List<Channel> = transactionManager.run { repoChannel.findAll() }

    fun getChannelById(channelId: Int): Channel? = transactionManager.run {
        repoChannel.findById(channelId)
    }

    fun getChannelByName(name: String): List<Channel> =
        transactionManager.run {
            repoChannel.findChannelByName(name)
        }

    companion object {
        private val logger = LoggerFactory.getLogger(ChannelService::class.java)
    }
}