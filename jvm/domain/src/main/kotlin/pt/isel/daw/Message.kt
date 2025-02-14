package pt.isel.daw

import java.time.LocalDateTime

data class Message(
    val id: Long,
    val text: String,
    val channel: Channel,
    val user: User,
    val dateCreated: LocalDateTime,
)
