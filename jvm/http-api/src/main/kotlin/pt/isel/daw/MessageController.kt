package pt.isel.daw

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pt.isel.daw.model.MessageInput
import pt.isel.daw.model.Problem

@RestController
@RequestMapping("/api/messages")
class MessageController(
    val userService: UserService,
    val channelService: ChannelService,
) {
    // _____________________________ MESSAGES __________________________________________
    @PostMapping
    fun sendMessage(
        ownerOfMess: AuthenticatedUser,
        @RequestBody messageInput: MessageInput,
    ): ResponseEntity<*> {
        val result: Either<MessageError, Message> =
            userService.sendMessage(
                messageInput.channelId,
                ownerOfMess.user.id,
                messageInput.text,
            )

        return when (result) {
            is Success -> {
                channelService.notifyChannel(messageInput.channelId, result.value)
                ResponseEntity.ok(result.value)
            }
            is Failure ->
                when (result.value) {
                    is MessageError.UserNotFound ->
                        Problem.UserNotFound.response(
                            HttpStatus.NOT_FOUND,
                        )

                    is MessageError.ChannelNotFound ->
                        Problem.UserNotFound.response(
                            HttpStatus.NOT_FOUND,
                        )

                    else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
                }
        }
    }

    @GetMapping("/{channelId}")
    fun readMessages(
        authUser: AuthenticatedUser,
        @PathVariable channelId: Int,
    ): ResponseEntity<*> {
        val result: Either<MessageError, List<Message>> = userService.readMessages(channelId, authUser.user.id)
        return when (result) {
            is Success -> {
                ResponseEntity.ok(result.value.ifEmpty { emptyList<Message>() })
            }
            is Failure -> {
                when (result.value) {
                    is MessageError.ChannelNotFound -> Problem.ChannelNotFound.response(HttpStatus.NOT_FOUND)
                    else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
                }
            }
//            is Success -> ResponseEntity.ok(result.value)
//            is Failure ->
//                when (result.value) {
//                    is MessageError.ChannelNotFound ->
//                        Problem.ChannelNotFound.response(
//                            HttpStatus.NOT_FOUND,
//                        )
//
//                    else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
//                }
        }
    }
}
