package pt.isel.daw.mem

import pt.isel.daw.Transaction
import pt.isel.daw.TransactionManager

class TransactionManagerMem : TransactionManager {
    val repoChannel = RepositoryChannelMem()
    val repoUser = RepositoryUserMem()
    val repoMessage = RepositaryMessageMem()
    val repoInvitation = RepositaryInvitationMem()

    override fun <R> run(block: Transaction.() -> R): R = block(TransactionMem(repoChannel, repoMessage, repoInvitation, repoUser))
}
