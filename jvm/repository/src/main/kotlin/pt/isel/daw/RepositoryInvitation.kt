package pt.isel.daw

interface RepositoryInvitation : Repository<Invitation> {
    fun findInvitationsForChannel(channelId: Channel): List<Invitation>?

    fun findInvitationForCreator(creatorId: User): List<Invitation>?

    fun findInvitationsForUser(guest: User): List<Invitation>?

    fun sendInvitationAuthenticated(
        channelId: Channel,
        guestId: User,
        type: InvitationType,
    ): Invitation

    fun removeInvitation(invitation: Invitation): Unit

    fun clear()

    fun createRegistryToken(): String
}
