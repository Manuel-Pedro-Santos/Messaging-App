package pt.isel.daw

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pt.isel.daw.model.InvitationEmail
import pt.isel.daw.model.InvitationInput
import pt.isel.daw.model.Problem

@RestController
@RequestMapping("/api/invitations")
class InvitationsController(
    private val userService: UserService,
) {
    // _____________________________ INVITATIONS __________________________________________

    @GetMapping("/{userId}")
    fun getAllInvitations(
        @PathVariable userId: Int,
    ): ResponseEntity<*> {
        val result: Either<UserError, List<Invitation>> = userService.findAllInvitations(userId)
        return when (result) {
            is Success -> ResponseEntity.ok(result.value)
            is Failure ->
                when (result.value) {
                    is UserError.UserNotFound -> Problem.UserNotFound.response(
                        HttpStatus.NOT_FOUND,
                    )

                    else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
                }
        }
    }

    @PostMapping("/send")
    fun sendInvitationToAlreadyAuthenticated(
        owner: AuthenticatedUser,
        @RequestBody invitationInput: InvitationInput,
    ): ResponseEntity<*> =
        when (val result: Either<UserError, Invitation> = userService.sendInvitationToAlreadyAuth(
            owner.user.id,
            invitationInput.guestId,
            invitationInput.channelId,
            invitationInput.invitationType,
        )
        ) {
            is Success -> ResponseEntity.ok("Invitation sent successfully to user with id ${result.value.guest.id}")
            is Failure -> when (result.value) {
                is UserError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.NOT_FOUND,
                )

                is UserError.ChannelNotFound -> Problem.ChannelNotFound.response(
                    HttpStatus.NOT_FOUND,
                )

                else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            }
        }

    @GetMapping("/accept/{invID}")
    fun acceptInvitationOfAuthenticatedUser(
        guest: AuthenticatedUser,
        @PathVariable invID: Int,
    ): ResponseEntity<*> {
        val result: Either<UserError, Channel> = userService.acceptInvitationAlreadyAuthenticated(invID, guest.user.id)
        return when (result) {
            is Success -> ResponseEntity.ok(result.value)
            is Failure -> when (result.value) {
                is UserError.InvitationNotFound -> Problem.InvitationNotFound.response(
                    HttpStatus.NOT_FOUND,
                )

                is UserError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.NOT_FOUND,
                )

                is UserError.ChannelNotFound -> Problem.ChannelNotFound.response(
                    HttpStatus.NOT_FOUND,
                )

                else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            }
        }
    }

    @GetMapping("/remove")
    fun removeInvitationOfAuthenticatedUser(
        guest: AuthenticatedUser,
        @RequestParam invID: Int
    ): ResponseEntity<*> {
        val result: Either<ChannelError, Unit> = userService.removeInvitation(invID)
        return when (result) {
            is Success -> ResponseEntity.ok(result.value)
            is Failure -> when (result.value) {
                is ChannelError.InvitationNotFound -> Problem.InvitationNotFound.response(
                    HttpStatus.NOT_FOUND
                )
                else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            }
        }
    }


    @PostMapping("/sendEmail")
    fun sendInvitationToNewUser(
        sender: AuthenticatedUser,
        @RequestBody invitationInput: InvitationEmail,
    ): ResponseEntity<*> {
        val result: Either<UserError, Boolean> = userService.sendInvitationToNewUser(
            email = invitationInput.email,
            senderEmail = sender.user.email)
        return when (result) {
            is Success -> ResponseEntity.ok("""Invitation sent successfully to user with email ${invitationInput.email} from user with email ${sender.user.email}""")
            is Failure -> when (result.value) {
                is UserError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.NOT_FOUND,
                )

                else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            }
        }
    }
}
