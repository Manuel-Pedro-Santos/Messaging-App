package pt.isel.daw

interface TransactionManager {
    fun <T> run(block: Transaction.() -> T): T
}
