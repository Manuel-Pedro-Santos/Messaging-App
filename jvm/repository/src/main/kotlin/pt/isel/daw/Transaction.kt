package pt.isel.daw

interface Transaction {
    val repoChannel: RepositoryChannel
    val repoMessage: RepositoryMessage
    val repoInvitation: RepositoryInvitation
    val repoUser: RepositoryUser

    fun rollback()
}
