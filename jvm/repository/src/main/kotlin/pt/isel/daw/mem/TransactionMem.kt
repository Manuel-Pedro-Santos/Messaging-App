package pt.isel.daw.mem

import pt.isel.daw.*

class TransactionMem(
    override val repoChannel: RepositoryChannel,
    override val repoMessage: RepositoryMessage,
    override val repoInvitation: RepositoryInvitation,
    override val repoUser: RepositoryUser,
) : Transaction {
    override fun rollback() {
        throw UnsupportedOperationException("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
