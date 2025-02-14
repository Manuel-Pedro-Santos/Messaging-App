package pt.isel.daw.mem

import pt.isel.daw.Channel
import pt.isel.daw.Invitation
import pt.isel.daw.InvitationType
import pt.isel.daw.RepositoryInvitation
import pt.isel.daw.User

class RepositaryInvitationMem : RepositoryInvitation {
    private val invitations = mutableListOf<Invitation>()
    private val registryTokens = mutableListOf<String>()

    override fun findInvitationsForChannel(channelId: Channel): List<Invitation> = invitations.filter { it.channel == channelId }

    override fun findInvitationForCreator(creatorId: User): List<Invitation> = invitations.filter { it.channel.owner == creatorId }

    override fun findInvitationsForUser(guest: User): List<Invitation> = invitations.filter { it.guest == guest }

    override fun sendInvitationAuthenticated(
        channelId: Channel,
        guestId: User,
        type: InvitationType,
    ): Invitation {
        val invitation = Invitation(invitations.size, channelId, guest = guestId, type)
        invitations.add(invitation)
        return invitation
    }

    override fun removeInvitation(invitation: Invitation) {
        invitations.remove(invitation)
    }

    override fun clear() {
        invitations.clear()
    }

    override fun createRegistryToken(): String {
        val token = registryTokens.size.toString()
        registryTokens.add(token)
        return token
    }

    override fun findById(id: Int): Invitation? = invitations.find { it.id == id }

    override fun findAll(): List<Invitation> = invitations

    override fun save(entity: Invitation) {
        invitations.add(entity)
    }

    override fun deleteById(id: Int) {
        invitations.removeIf { it.id == id }
    }
}
