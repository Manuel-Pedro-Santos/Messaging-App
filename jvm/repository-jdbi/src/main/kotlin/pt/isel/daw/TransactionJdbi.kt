package pt.isel.daw

import org.jdbi.v3.core.Handle

class TransactionJdbi(
    private val handle: Handle,
) : Transaction {
    override val repoChannel = RepositoryChannelJdbi(handle)
    override val repoUser = RepositoryUserJdbi(handle)
    override val repoMessage: RepositoryMessageJdbi = RepositoryMessageJdbi(handle)
    override val repoInvitation: RepositoryInvitation = RepositoryInvitationJdbi(handle)

    override fun rollback() {
        handle.rollback()
    }
}
