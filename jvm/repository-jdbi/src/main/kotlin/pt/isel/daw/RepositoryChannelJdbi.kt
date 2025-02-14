package pt.isel.daw

import org.jdbi.v3.core.Handle
import java.sql.ResultSet

class RepositoryChannelJdbi(
    private val handle: Handle,
) : RepositoryChannel {
    private val users = RepositoryUserJdbi(handle)

    override fun createSingleChannel(
        channelName: String,
        owner: User,
    ): SingleChannel {
        val id =
            handle
                .createUpdate("INSERT INTO dbo.channel (name, owner_id,channel_type) VALUES (:name, :owner, :channel_type)")
                .bind("name", channelName)
                .bind("owner", owner.id)
                .bind("channel_type", SelectionType.SINGLE.name)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        handle
            .createUpdate("INSERT INTO dbo.channel_single (channel_id) VALUES (:channel_id)")
            .bind("channel_id", id)
            .execute()

        return SingleChannel(id, channelName, owner)
    }
    override fun createGroupChannel(
        channelName: String,
        owner: User,
        controls: Controls,
    ): GroupChannel {
        val id =
            handle
                .createUpdate("INSERT INTO dbo.channel (name, owner_id, channel_type) VALUES (:name, :owner, :channel_type)")
                .bind("name", channelName)
                .bind("owner", owner.id)
                .bind("channel_type", SelectionType.GROUP.name)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        handle
            .createUpdate("INSERT INTO dbo.channel_group (channel_id, controls) VALUES (:channel_id, :controls)")
            .bind("channel_id", id)
            .bind("controls", controls.name)
            .execute()

        handle
            .createUpdate(
                "INSERT INTO dbo.channel_group_users (channel_group_id, user_id, access_type) " +
                        "VALUES ((SELECT id FROM dbo.channel_group WHERE channel_id = :channel_id), :user_id, :access_type)"
            )
            .bind("channel_id", id)
            .bind("user_id", owner.id)
            .bind("access_type", "READ_WRITE")
            .execute()

        return GroupChannel(id, channelName, owner)
    }

    override fun findChannelByName(name: String): List<Channel> {
        val channels = mutableListOf<Channel>()

        // Fetch channels with names that match the full name or contain the partial string
        val channelInfos =
            handle
                .createQuery("SELECT id, channel_type FROM dbo.channel WHERE name LIKE :name")
                .bind("name", "%$name%")
                .map { rs, _ ->
                    Pair(rs.getInt("id"), rs.getString("channel_type"))
                }.list()

        for ((channelId, type) in channelInfos) {
            when (type) {
                "SINGLE" -> {
                    // Fetch and map to SingleChannel
                    val singleChannel =
                        handle
                            .createQuery(
                                """
                    SELECT ch.id as channel_id, ch.name, ch.owner_id, sch.guest_id, 
                           ow.email, ow.username, ow.password_validation
                    FROM dbo.channel ch
                    JOIN dbo.channel_single sch ON ch.id = sch.channel_id
                    LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                    WHERE ch.id = :channel_id
                    """,
                            ).bind("channel_id", channelId)
                            .map { rs, _ -> mapRowToSingleChannel(rs) }
                            .list()
                            .firstOrNull()

                    if (singleChannel != null) {
                        channels.add(singleChannel)
                    }
                }

                "GROUP" -> {
                    // Fetch and map to GroupChannel
                    val groupChannel =
                        handle
                            .createQuery(
                                """
                    SELECT ch.id as channel_id, ch.name, ch.owner_id, cg.controls, 
                           ow.email, ow.username, ow.password_validation
                    FROM dbo.channel ch
                    JOIN dbo.channel_group cg ON ch.id = cg.channel_id
                    LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                    WHERE ch.id = :channel_id
                    """,
                            ).bind("channel_id", channelId)
                            .map { rs, _ -> mapRowToGroupChannel(rs) }
                            .list()
                            .firstOrNull()

                    if (groupChannel != null) {
                        channels.add(groupChannel)
                    }
                }
            }
        }

        return channels
    }

    override fun findChannelsByOwner(owner: User): List<Channel> {
        val channels = mutableListOf<Channel>()

        val singleChannels: MutableList<SingleChannel> =
            handle
                .createQuery(
                    """
                        SELECT ch.id as channel_id, ch.name, ch.owner_id, sch.guest_id, 
                        ow.email, ow.username, ow.password_validation
                        FROM dbo.channel ch
                        JOIN dbo.channel_single sch ON ch.id = sch.channel_id
                        LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                        WHERE ch.owner_id = :owner_id
                    """,
                ).bind("owner_id", owner.id)
                .map { rs, _ -> mapRowToSingleChannel(rs) }
                .list()

        channels.addAll(singleChannels)

        val groupChannels: MutableList<GroupChannel> =
            handle
                .createQuery(
                    """
                        SELECT ch.id as channel_id, ch.name, ch.owner_id, cg.controls, ow.email, ow.username, ow.password_validation
                        FROM dbo.channel ch
                        JOIN dbo.channel_group cg ON ch.id = cg.channel_id
                        LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                        WHERE ch.owner_id = :owner_id
                    """,
                ).bind("owner_id", owner.id)
                .map { rs, _ -> mapRowToGroupChannel(rs) }
                .list()

        channels.addAll(groupChannels)

        return channels
    }

    override fun findPublicChannels(): List<Channel> =
        handle
            .createQuery(
                """
            SELECT ch.id as channel_id, ch.name, ch.owner_id, cg.controls,
                   ow.email, ow.username, ow.password_validation
            FROM dbo.channel ch
            JOIN dbo.channel_group cg ON ch.id = cg.channel_id
            LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
            WHERE cg.controls = :controls
            ORDER BY ch.id ASC
            """,
            ).bind("controls", Controls.PUBLIC.name) // Bind the PUBLIC control
            .map { rs, _ -> mapRowToGroupChannel(rs) } // Map to GroupChannel
            .list()

    override fun userJoinChannelSingle(
        channel: Channel,
        user: User,
    ): SingleChannel {
        val id = channel.id
        val u = user.id

        // Check if the guest_id is NULL for the given channel_id
        val guestId =
            handle
                .createQuery("SELECT guest_id FROM dbo.channel_single WHERE channel_id = :channel_id")
                .bind("channel_id", id)
                .mapTo(Int::class.java)
                .findOne()
                .orElse(null)
        if (guestId == 0) {
            // Update the record with the new guest_id
            handle
                .createUpdate("UPDATE dbo.channel_single SET guest_id = :guest_id WHERE channel_id = :channel_id")
                .bind("channel_id", id)
                .bind("guest_id", u)
                .execute()
        } else {
            // Throw an exception if guest_id is not null
            throw IllegalStateException("Channel already has a guest")
        }

        return channel as SingleChannel
    }

    override fun userLeaveChannelSingle(
        channel: Channel,
        user: User,
    ): SingleChannel {
        val id = channel.id
        val u = user.id

        // Check if the guest_id is the same as the user_id
        val guestId =
            handle
                .createQuery("SELECT guest_id FROM dbo.channel_single WHERE channel_id = :channel_id")
                .bind("channel_id", id)
                .mapTo(Int::class.java)
                .findOne()
                .orElse(null)

        if (guestId == u) {
            // Update the record with the new guest_id
            handle
                .createUpdate("UPDATE dbo.channel_single SET guest_id = NULL WHERE channel_id = :channel_id")
                .bind("channel_id", id)
                .execute()
        } else {
            // Throw an exception if guest_id is not the same as the user_id
            throw IllegalStateException("User is not a guest")
        }

        return channel as SingleChannel
    }

    override fun userJoinChannelGroup(
        channel: Channel,
        user: User,
        rules: InvitationType,
    ): GroupChannel {
        val id = channel.id
        val uID = user.id

        // Check if the channel group exists and retrieve the channel_group_id
        val channelGroupId =
            handle
                .createQuery("SELECT id FROM dbo.channel_group WHERE channel_id = :channel_id")
                .bind("channel_id", id)
                .mapTo(Int::class.java)
                .one()

        if (channelGroupId == null) {
            throw IllegalStateException("Channel group with ID $id does not exist")
        }

        // Insert the user into the channel group using the correct channel_group_id
        handle
            .createUpdate(
                """
            INSERT INTO dbo.channel_group_users (channel_group_id, user_id, access_type)
            VALUES (:channel_group_id, :user_id, :access)
            """,
            ).bind("channel_group_id", channelGroupId) // Bind the correct channel_group_id
            .bind("user_id", uID)
            .bind("access", rules.name)
            .execute()

        return channel as GroupChannel
    }
    override fun findChannelsByUser(userId: Int): List<Channel>? {
        val channels = mutableSetOf<Channel>()

        // Fetch channels where the user is a guest in SingleChannel
        val singleChannelsAsGuest: MutableList<SingleChannel> =
            handle
                .createQuery(
                    """
                    SELECT ch.id as channel_id, ch.name, ch.owner_id, sch.guest_id, 
                    ow.email, ow.username, ow.password_validation
                    FROM dbo.channel ch
                    JOIN dbo.channel_single sch ON ch.id = sch.channel_id
                    LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                    WHERE sch.guest_id = :user_id
                """,
                ).bind("user_id", userId)
                .map { rs, _ -> mapRowToSingleChannel(rs) }
                .list()

        channels.addAll(singleChannelsAsGuest)

        // Fetch channels where the user is the owner in SingleChannel
        val singleChannelsAsOwner: MutableList<SingleChannel> =
            handle
                .createQuery(
                    """
                    SELECT ch.id as channel_id, ch.name, ch.owner_id, sch.guest_id, 
                    ow.email, ow.username, ow.password_validation
                    FROM dbo.channel ch
                    JOIN dbo.channel_single sch ON ch.id = sch.channel_id
                    LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                    WHERE ch.owner_id = :user_id
                """,
                ).bind("user_id", userId)
                .map { rs, _ -> mapRowToSingleChannel(rs) }
                .list()

        channels.addAll(singleChannelsAsOwner)

        // Fetch channels where the user is a member in GroupChannel
        val groupChannelsAsMember: MutableList<GroupChannel> =
            handle
                .createQuery(
                    """
                    SELECT ch.id as channel_id, ch.name, ch.owner_id, cg.controls, ow.email, ow.username, ow.password_validation
                    FROM dbo.channel ch
                    JOIN dbo.channel_group cg ON ch.id = cg.channel_id
                    LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                    WHERE EXISTS (
                        SELECT 1
                        FROM dbo.channel_group_users
                        WHERE channel_group_id = cg.id AND user_id = :user_id
                    )
                """,
                ).bind("user_id", userId)
                .map { rs, _ -> mapRowToGroupChannel(rs) }
                .list()

        channels.addAll(groupChannelsAsMember)

        // Fetch channels where the user is the owner in GroupChannel
        val groupChannelsAsOwner: MutableList<GroupChannel> =
            handle
                .createQuery(
                    """
                    SELECT ch.id as channel_id, ch.name, ch.owner_id, cg.controls, ow.email, ow.username, ow.password_validation
                    FROM dbo.channel ch
                    JOIN dbo.channel_group cg ON ch.id = cg.channel_id
                    LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                    WHERE ch.owner_id = :user_id
                """,
                ).bind("user_id", userId)
                .map { rs, _ -> mapRowToGroupChannel(rs) }
                .list()

        channels.addAll(groupChannelsAsOwner)

        // Fetch public channels
        /*val publicChannels: MutableList<GroupChannel> =
            handle
                .createQuery(
                    """
                    SELECT ch.id as channel_id, ch.name, ch.owner_id, cg.controls, ow.email, ow.username, ow.password_validation
                    FROM dbo.channel ch
                    JOIN dbo.channel_group cg ON ch.id = cg.channel_id
                    LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                    WHERE cg.controls = :controls
                """,
                ).bind("controls", Controls.PUBLIC.name)
                .map { rs, _ -> mapRowToGroupChannel(rs) }
                .list()

        channels.addAll(publicChannels)*/

        return channels.toList()
    }


    override fun userLeaveChannelGroup(
        channel: Channel,
        user: User,
    ): GroupChannel {
        val id = channel.id
        val u = user.id

        val channelGroupId =
            handle
                .createQuery("SELECT id FROM dbo.channel_group WHERE channel_id = :channel_id")
                .bind("channel_id", id)
                .mapTo(Int::class.java)
                .one()

        if (channelGroupId == null) {
            throw IllegalStateException("Channel group with ID $id does not exist")
        }

        handle
            .createUpdate(
                """
                DELETE FROM dbo.channel_group_users
                WHERE channel_group_id = :channel_group_id AND user_id = :user_id
                """,
            ).bind("channel_group_id", channelGroupId)
            .bind("user_id", u)
            .execute()

        return channel as GroupChannel
    }

    override fun isUserInChannelAndCanSendMessages(
        channel: Channel,
        user: User,
    ): Boolean {
        val id = channel.id
        val u = user.id

        val channelType =
            handle
                .createQuery("SELECT channel_type FROM dbo.channel WHERE id = :id")
                .bind("id", id)
                .mapTo(String::class.java)
                .firstOrNull()

        return when (channelType) {
            "SINGLE" -> {
                val guestId =
                    handle
                        .createQuery("SELECT guest_id FROM dbo.channel_single WHERE channel_id = :channel_id")
                        .bind("channel_id", id)
                        .mapTo(Int::class.java)
                        .firstOrNull()


                guestId == u || channel.owner.id == u
            }

            "GROUP" -> {
                val channelGroupId =
                    handle
                        .createQuery("SELECT id FROM dbo.channel_group WHERE channel_id = :channel_id")
                        .bind("channel_id", id)
                        .mapTo(Int::class.java)
                        .firstOrNull()


                if (channelGroupId == null) {
                    throw IllegalStateException("Channel group with ID $id does not exist")
                }

                val accessType =
                    handle
                        .createQuery(
                            """
                            SELECT access_type
                            FROM dbo.channel_group_users
                            WHERE channel_group_id = :channel_group_id AND user_id = :user_id
                            """,
                        ).bind("channel_group_id", channelGroupId)
                        .bind("user_id", u)
                        .mapTo(String::class.java)
                        .firstOrNull()

                if (accessType == null) {
                    return false
                }
                accessType == InvitationType.READ_WRITE.name || channel.owner.id == u
            }

            else -> false
        }
    }

    override fun isUserInChannel(
        channel: Channel,
        user: User,
    ): Boolean {
        val id = channel.id
        val u = user.id

        val channelType =
            handle
                .createQuery("SELECT channel_type FROM dbo.channel WHERE id = :id")
                .bind("id", id)
                .mapTo(String::class.java)
                .one()

        return when (channelType) {
            "SINGLE" -> {
                val guestId =
                    handle
                        .createQuery("SELECT guest_id FROM dbo.channel_single WHERE channel_id = :channel_id")
                        .bind("channel_id", id)
                        .mapTo(Int::class.java)
                        .findOne()
                        .orElse(null)

                guestId == u || channel.owner.id == u
            }

            "GROUP" -> {
                val channelGroupId =
                    handle
                        .createQuery("SELECT id FROM dbo.channel_group WHERE channel_id = :channel_id")
                        .bind("channel_id", id)
                        .mapTo(Int::class.java)
                        .findOne()
                        .orElse(null)

                if (channelGroupId == null) {
                    throw IllegalStateException("Channel group with ID $id does not exist")
                }

                val accessType =
                    handle
                        .createQuery(
                            """
                            SELECT access_type
                            FROM dbo.channel_group_users
                            WHERE channel_group_id = :channel_group_id AND user_id = :user_id
                            """,
                        ).bind("channel_group_id", channelGroupId)
                        .bind("user_id", u)
                        .mapTo(String::class.java)
                        .findOne()
                        .orElse(null)

                accessType == InvitationType.READ_WRITE.name || accessType == InvitationType.READ_ONLY.name || channel.owner.id == u
            }

            else -> false
        }
    }

    override fun findById(id: Int): Channel? {
        val singleChannel =
            handle
                .createQuery(
                    """
                        SELECT ch.id as channel_id, ch.name, ch.owner_id, sch.guest_id, 
                        ow.email, ow.username, ow.password_validation
                        FROM dbo.channel ch
                        JOIN dbo.channel_single sch ON ch.id = sch.channel_id
                        LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                        WHERE ch.id = :id
                    """,
                ).bind("id", id)
                .map { rs, _ -> mapRowToSingleChannel(rs) }
                .firstOrNull()

        if (singleChannel != null) {
            return singleChannel
        } else {
            val groupChannel =
                handle
                    .createQuery(
                        """
                            SELECT ch.id as channel_id, ch.name, ch.owner_id, cg.controls, ow.email, ow.username, ow.password_validation
                            FROM dbo.channel ch
                            JOIN dbo.channel_group cg ON ch.id = cg.channel_id
                            LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                            WHERE ch.id = :id
                        """,
                    ).bind("id", id)
                    .map { rs, _ -> mapRowToGroupChannel(rs) }
                    .firstOrNull()
            if (groupChannel != null) {
                return groupChannel
            }
        }
        return null
    }

    override fun findAll(): List<Channel> {
        val channels = mutableListOf<Channel>()

        val singleChannels: MutableList<SingleChannel> =
            handle
                .createQuery(
                    """
                        SELECT ch.id as channel_id, ch.name, ch.owner_id, sch.guest_id, 
                        ow.email, ow.username, ow.password_validation
                        FROM dbo.channel ch
                        JOIN dbo.channel_single sch ON ch.id = sch.channel_id
                        LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                        ORDER BY sch.id DESC
                    """,
                ).map { rs, _ -> mapRowToSingleChannel(rs) }
                .list()

        channels.addAll(singleChannels)

        val groupChannels: MutableList<GroupChannel> =
            handle
                .createQuery(
                    """
                        SELECT ch.id as channel_id, ch.name, ch.owner_id, cg.controls, ow.email, ow.username, ow.password_validation
                        FROM dbo.channel ch
                        JOIN dbo.channel_group cg ON ch.id = cg.channel_id
                        LEFT JOIN dbo.users ow ON ch.owner_id = ow.id
                        ORDER BY cg.id DESC
                    """,
                ).map { rs, _ -> mapRowToGroupChannel(rs) }
                .list()

        channels.addAll(groupChannels)

        return channels.sortedByDescending { it.id }
    }

    override fun save(entity: Channel) {
        when (entity) {
            is SingleChannel -> {
                // Update existing channel record
                handle
                    .createUpdate(
                        """
                    UPDATE dbo.channel 
                    SET name = :name, owner_id = :owner, channel_type = 'SINGLE'
                    WHERE id = :id
                    """,
                    ).bind("id", entity.id)
                    .bind("name", entity.name)
                    .bind("owner", entity.owner.id)
                    .execute()

                // Update or insert into channel_single if needed
                handle
                    .createUpdate(
                        """
                    UPDATE dbo.channel_single 
                    SET guest_id = :guest_id
                    WHERE channel_id = :channel_id
                    """,
                    ).bind("channel_id", entity.id)
                    .bind("guest_id", entity.guest?.id) // Assuming SingleChannel has a 'guest'
                    .execute()
            }

            is GroupChannel -> {
                // Update existing channel record
                handle
                    .createUpdate(
                        """
                    UPDATE dbo.channel 
                    SET name = :name, owner_id = :owner, channel_type = 'GROUP'
                    WHERE id = :id
                    """,
                    ).bind("id", entity.id)
                    .bind("name", entity.name)
                    .bind("owner", entity.owner.id)
                    .execute()

                // Update channel_group controls
                handle
                    .createUpdate(
                        """
                    UPDATE dbo.channel_group 
                    SET controls = :controls
                    WHERE channel_id = :channel_id
                    """,
                    ).bind("channel_id", entity.id)
                    .bind("controls", entity.controls.name)
                    .execute()
            }
        }
    }

    override fun deleteById(id: Int) {
        // Check if the channel is a group or single channel
        val channelType =
            handle
                .createQuery("SELECT channel_type FROM dbo.channel WHERE id = :id")
                .bind("id", id)
                .mapTo(String::class.java)
                .one()

        // If it's a group channel, delete associated records in the correct order
        if (channelType == "GROUP") {
            // Step 1: Delete from channel_group_users
            handle
                .createUpdate(
                    """
                    DELETE FROM dbo.channel_group_users WHERE channel_group_id = 
                    (SELECT id FROM dbo.channel_group WHERE channel_id = :id)
                    """,
                ).bind("id", id)
                .execute()

            // Step 2: Delete from channel_group
            handle
                .createUpdate("DELETE FROM dbo.channel_group WHERE channel_id = :id")
                .bind("id", id)
                .execute()
        }

        // If it's a single channel, delete from channel_single
        if (channelType == "SINGLE") {
            handle
                .createUpdate("DELETE FROM dbo.channel_single WHERE channel_id = :id")
                .bind("id", id)
                .execute()
        }

        // Step 3: Delete associated messages in the message table
        handle
            .createUpdate("DELETE FROM dbo.message WHERE channel_id = :id")
            .bind("id", id)
            .execute()

        // Step 4: Finally, delete the channel itself
        handle
            .createUpdate("DELETE FROM dbo.channel WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE from dbo.channel_single").execute()
        handle.createUpdate("DELETE from dbo.channel_group_users").execute()
        handle.createUpdate("DELETE from dbo.channel_group").execute()
        handle.createUpdate("DELETE FROM dbo.channel").execute()
    }

    private fun mapRowToSingleChannel(rs: ResultSet): SingleChannel {
        val owner =
            if (rs.getInt("owner_id") != 0) {
                User(
                    id = rs.getInt("owner_id"),
                    email = rs.getString("email"),
                    username = rs.getString("username"),
                    passwordValidationInfo = PasswordValidationInfo(rs.getString("password_validation")),
                )
            } else {
                null
            }

        val guest =
            if (rs.getInt("guest_id") != 0) {
                users.findById(rs.getInt("guest_id"))!!
            } else {
                null
            }
        return SingleChannel(
            id = rs.getInt("channel_id"),
            name = rs.getString("name"),
            owner = owner ?: throw IllegalStateException("Owner is null"),
            guest = guest,
        )
    }

    private fun mapRowToGroupChannel(rs: ResultSet): GroupChannel {
        val owner =
            if (rs.getInt("owner_id") != 0) {
                User(
                    id = rs.getInt("owner_id"),
                    email = rs.getString("email"),
                    username = rs.getString("username"),
                    passwordValidationInfo = PasswordValidationInfo(rs.getString("password_validation")),
                )
            } else {
                null
            }

        // Fetch the channel_group_id using the channel_id
        val channelGroupId =
            handle
                .createQuery("SELECT id FROM dbo.channel_group WHERE channel_id = :channel_id")
                .bind("channel_id", rs.getInt("channel_id"))
                .mapTo(Int::class.java)
                .one()

        // Fetch the guests using the correct channel_group_id
        val guests =
            handle
                .createQuery("SELECT * FROM dbo.channel_group_users WHERE channel_group_id = :channel_group_id")
                .bind("channel_group_id", channelGroupId)
                .map { guestRs, _ -> users.findById(guestRs.getInt("user_id"))!! }
                .list()

        return GroupChannel(
            id = rs.getInt("channel_id"),
            name = rs.getString("name"),
            owner = owner ?: throw IllegalStateException("Owner is null"),
            controls = Controls.valueOf(rs.getString("controls")),
            guests = guests,
        )
    }
}
