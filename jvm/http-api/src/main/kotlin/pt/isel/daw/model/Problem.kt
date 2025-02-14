package pt.isel.daw.model

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI

private const val MEDIA_TYPE = "application/problem+json"
private const val PROBLEM_URI_PATH = "https://github.com/isel-leic-daw/2024-daw-leic53d-g01-53d/tree/main/code/jvm/docs/problems"

sealed class Problem(
    typeUri: URI,
) {
    val type = typeUri.toString()
    val title = typeUri.toString().split("/").last()

    fun response(status: HttpStatus): ResponseEntity<Any> =
        ResponseEntity
            .status(status)
            .header("Content-Type", MEDIA_TYPE)
            .body(this)

    data object ChannelNotFound : Problem(URI("$PROBLEM_URI_PATH/channel-not-found"))

    data object ChannelNameInUse : Problem(URI("$PROBLEM_URI_PATH/channel-already-in-use"))

    data object UserNotFound : Problem(URI("$PROBLEM_URI_PATH/user-not-found"))

    data object InvalidCredentials : Problem(URI("$PROBLEM_URI_PATH/invalid-user-credentials"))

    data object UserAlreadyAllocated : Problem(URI("$PROBLEM_URI_PATH/user-creation-error"))

    data object InvitationNotFound : Problem(URI("$PROBLEM_URI_PATH/invitation-not-found"))
}
