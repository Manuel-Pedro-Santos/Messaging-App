package pt.isel.daw.model

import pt.isel.daw.InvitationType

data class InvitationInput(
    val channelId: Int,
    val guestId: Int,
    val invitationType: InvitationType,
)
