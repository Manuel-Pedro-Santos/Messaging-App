package pt.isel.daw

sealed class Channel(
    open val id: Int,
    open val name: String,
    open val owner: User,
    open val type: SelectionType,
) {
    abstract fun addUser(users: User): Channel

    abstract fun removeUser(users: User): Channel
}

data class SingleChannel(
    override val id: Int,
    override val name: String,
    override val owner: User,
    override val type: SelectionType = SelectionType.SINGLE,
    val guest: User? = null,
) : Channel(id, name, owner, type) {
    override fun addUser(users: User): Channel {
        check(guest == null) { "Channel already has a guest" }
        return copy(guest = users)
    }

    override fun removeUser(users: User): Channel {
        check(guest != null) { "Channel does not have a guest" }
        return copy(guest = null)
    }
}

data class GroupChannel(
    override val id: Int,
    override val name: String,
    override val owner: User,
    override val type: SelectionType = SelectionType.GROUP,
    val controls: Controls = Controls.PUBLIC,
    val guests: List<User> = emptyList(),
) : Channel(id, name, owner, type) {
    override fun addUser(users: User): Channel = copy(guests = guests.plus(users))

    override fun removeUser(users: User): Channel = copy(guests = guests.minus(users))
}
