package pt.isel.daw

data class Invitation(
    val id: Int,
    val channel: Channel,
    val guest: User,
    val invitationType: InvitationType,
)
