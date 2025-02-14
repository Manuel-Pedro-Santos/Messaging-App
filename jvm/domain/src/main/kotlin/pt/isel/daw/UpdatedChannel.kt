package pt.isel.daw

import kotlinx.datetime.Instant

sealed interface UpdatedChannel {
    data class Message(
        val id: Long,
        val message: pt.isel.daw.Message,
    ) : UpdatedChannel

    data class KeepAlive(
        val timestamp: Instant,
    ) : UpdatedChannel
}
