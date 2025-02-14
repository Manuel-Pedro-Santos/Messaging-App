package pt.isel.daw

import org.jdbi.v3.core.Handle
import java.sql.ResultSet

class RepositoryInvitationJdbi(
    private val handle: Handle,
) : RepositoryInvitation {
    private val users = RepositoryUserJdbi(handle)
    private val channels = RepositoryChannelJdbi(handle)

    override fun findInvitationsForChannel(channelId: Channel): List<Invitation>? {
        val id = channelId.id
        return handle
            .createQuery("SELECT * FROM dbo.invitation WHERE channel_id = :channel_id")
            .bind("channel_id", id)
            .map { rs, _ -> mapRowtoInvitation(rs) }
            .list()
            .ifEmpty { null }
    }

    override fun findInvitationForCreator(creatorId: User): List<Invitation>? {
        val id = creatorId.id
        return handle
            .createQuery("SELECT * FROM dbo.invitation WHERE creator_id = :creator_id")
            .bind("creator_id", id)
            .map { rs, _ -> mapRowtoInvitation(rs) }
            .list()
            .ifEmpty { null }
    }

    override fun findInvitationsForUser(guest: User): List<Invitation>? {
        val id = guest.id
        return handle
            .createQuery("SELECT * FROM dbo.invitation WHERE guest_id = :guest_id")
            .bind("guest_id", id)
            .map { rs, _ -> mapRowtoInvitation(rs) }
            .list()
            .ifEmpty { null }
    }

    override fun sendInvitationAuthenticated(
        channelId: Channel,
        guestId: User,
        type: InvitationType,
    ): Invitation {
        val id =
            handle
                .createUpdate(
                    """
            INSERT INTO dbo.invitation (channel_id, guest_id, invitation_type)
            VALUES (:channel_id, :guest_id, :type)
            """,
                ).bind("channel_id", channelId.id)
                .bind("guest_id", guestId.id)
                .bind("type", type.name)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        return Invitation(
            id = id,
            channel = channelId,
            guest = guestId,
            invitationType = type,
        )
    }

    override fun removeInvitation(invitation: Invitation) {
        handle
            .createUpdate("DELETE FROM dbo.invitation WHERE id = :id")
            .bind("id", invitation.id)
            .execute()
    }

    override fun findById(id: Int): Invitation? =
        handle
            .createQuery("SELECT * FROM dbo.invitation WHERE id = :id")
            .bind("id", id)
            .map { rs, _ -> mapRowtoInvitation(rs) }
            .findOne()
            .orElse(null)

    override fun findAll(): List<Invitation> =
        handle
            .createQuery("SELECT * FROM dbo.invitation")
            .map { rs, _ -> mapRowtoInvitation(rs) }
            .list()

    override fun save(entity: Invitation) {
        handle
            .createUpdate(
                """
            UPDATE dbo.invitation
            SET channel_id = :channel_id, guest_id = :guest_id
            WHERE id = :id
            """,
            ).bind("channel_id", entity.channel.id)
            .bind("guest_id", entity.guest.id)
            .bind("id", entity.id)
            .execute()
    }

    override fun deleteById(id: Int) {
        handle
            .createUpdate("DELETE FROM dbo.invitation WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun createRegistryToken(): String {
        val token =
            java.util.UUID
                .randomUUID()
                .toString()
        handle
            .createUpdate(
                """
            INSERT INTO dbo.registry_token (registry_token)
            VALUES (:registry_token)
            """,
            ).bind("registry_token", token)
            .execute()
        return token
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM dbo.invitation").execute()
    }

    private fun mapRowtoInvitation(rs: ResultSet): Invitation =
        Invitation(
            id = rs.getInt("id"),
            channel = channels.findById(rs.getInt("channel_id")) ?: throw IllegalStateException("Channel not found"),
            guest = users.findById(rs.getInt("guest_id"))!!,
            invitationType = InvitationType.valueOf(rs.getString("invitation_type")),
        )
}
