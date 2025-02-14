package pt.isel.daw

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import pt.isel.daw.model.ChannelInput
import pt.isel.daw.model.Problem
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/channels")
class ChannelController(
    private val service: ChannelService,
) {
    @Autowired
    private lateinit var channelService: ChannelService

    @GetMapping
    fun getChannels(): ResponseEntity<List<Channel>> {
        val channels = service.getChannels()
        return ResponseEntity.ok(channels)
    }

    @GetMapping("/user/{userId}")
    fun getChannelsByUser(
        @PathVariable userId: Int,
    ): ResponseEntity<*> {
        val channels = service.getChannelsByUser(userId)
        return ResponseEntity.ok(channels)
    }

    @GetMapping("/{name}")
    fun getChannel(
        @PathVariable name: String,
    ): ResponseEntity<*> {
        val channel = service.getChannelByName(name)
        return ResponseEntity.ok(channel)
    }

    @GetMapping("/{channelId}/{userId}")
    fun checkUserInChannel(
        @PathVariable channelId: Int,
        @PathVariable userId: Int,
    ): ResponseEntity<*> {
        try {
            val channel = service.getChannelById(channelId)
            if (channel == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Channel not found")
            }
            val isUserInChannel = service.isUserInChannel(channelId, userId)
            return ResponseEntity.ok(isUserInChannel)
        } catch (e: Exception) {
            print("Error checking if user is in channel: ${e.message}")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error checking user in channel")
        }
    }

    @GetMapping("/find/{channelId}")
    fun getChannelById(
        @PathVariable channelId: Int,
    ): ResponseEntity<*> {
        val channel = service.getChannelById(channelId)
        return ResponseEntity.ok(channel)
    }

    @PostMapping("/create")
    fun createChannel(
        owner: AuthenticatedUser,
        @RequestBody ch: ChannelInput,
    ): ResponseEntity<*> =
        when (
            val channel: Either<ChannelError, Channel> =
                if (ch.type == SelectionType.SINGLE) {
                    service.createSingleChannel(ch.name, owner.user.id)
                } else {
                    service.createGroupChannel(ch.name, owner.user.id, ch.controls)
                }
        ) {
            is Success -> ResponseEntity.ok(channel.value.id)
            is Failure ->
                when (channel.value) {
                    is ChannelError.UserNotFound -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
                    is ChannelError.UserAlreadyExists -> Problem.UserAlreadyAllocated.response(HttpStatus.CONFLICT)
                    is ChannelError.ChannelNameAlreadyInUse -> Problem.ChannelNameInUse.response(HttpStatus.CONFLICT)
                    else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(channel.value)
                }
        }

    @GetMapping("/leaveChannel/{channelId}/{userId}")
    fun leaveChannel(
        @PathVariable channelId: Int,
        @PathVariable userId: Int,
    ): ResponseEntity<*> {
        return try {
            val result = service.leaveChannel(channelId, userId)
            when (result) {
                is Success -> ResponseEntity.ok("User successfully left the channel.")
                is Failure -> {
                    val error = result.value
                    when (error) {
                        is ChannelError.ChannelNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Channel not found")
                        is ChannelError.UserNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found")
                        else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to leave the channel")
                    }
                }
            }
        } catch (e: Exception) {
            print("Error leaving channel: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error leaving channel")
        }
    }

    @GetMapping("/join/{channelId}")
    fun joinChannel(
        user: AuthenticatedUser,
        @PathVariable channelId: Int,
    ): ResponseEntity<*> {
        return try {
            val channel = service.getChannelById(channelId)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Channel not found")
            if (channel is GroupChannel && channel.controls == Controls.PUBLIC) {
                when (val result = service.joinChannel(channelId, user.user.id, InvitationType.READ_WRITE)) {
                    is Success -> ResponseEntity.ok("Successfully joined channel")
                    is Failure -> when (result.value) {
                        is ChannelError.UserAlreadyExists ->
                            ResponseEntity.status(HttpStatus.CONFLICT).body("User already in channel")
                        is ChannelError.ChannelNotFound ->
                            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Channel not found")
                        else ->
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to join channel")
                    }
                }
            } else {
                ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only join public group channels")
            }
        } catch (e: Exception) {
            print("Error joining channel: ${e.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error joining channel")
        }
    }

    @GetMapping("/{channelId}/listen")
    fun listen(@PathVariable channelId: Int): SseEmitter {
        val sseEmitter = SseEmitter()
        channelService.addEmitter(channelId, SseUpdatedChannelEmitterAdapter(sseEmitter))
        return sseEmitter
    }
}
