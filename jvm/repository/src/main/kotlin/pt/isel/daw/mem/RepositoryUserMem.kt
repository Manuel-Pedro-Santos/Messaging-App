

package pt.isel.daw.mem

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import pt.isel.daw.*

class RepositoryUserMem : RepositoryUser {
    private val users = mutableListOf<User>()
    private val tokens = mutableListOf<Token>()

    override fun createUser(
        email: String,
        username: String,
        hashedPass: PasswordValidationInfo,
        registryToken: String,
    ): User {
        val user = User(id = users.size, email = email, username = username, passwordValidationInfo = hashedPass)
        users.add(user)
        return user
    }

    override fun findByUsername(username: String): User? = users.find { it.username == username }

    override fun findByEmail(email: String): User? = users.find { it.email == email }

    override fun saveToken(
        token: String,
        user: User,
    ) {
        val tokenValidationInfo = TokenValidationInfo(token)
        val tokenObj = Token(tokenValidationInfo, user.id, Clock.System.now(), Clock.System.now())
        tokens.add(tokenObj)
    }

    override fun findByName(name: String): User? {
        TODO("Not yet implemented")
    }

    override fun clear() {
        users.clear()
    }

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ) {
        val nrOfTokens = tokens.count { it.userId == token.userId }

        // Remove the oldest token if we have achieved the maximum number of tokens
        if (nrOfTokens >= maxTokens) {
            tokens
                .filter { it.userId == token.userId }
                .minByOrNull { it.lastUsedAt }!!
                .also { tk -> tokens.removeIf { it.tokenValidationInfo == tk.tokenValidationInfo } }
        }
        tokens.add(token)
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int =
        tokens.removeIf { it.tokenValidationInfo == tokenValidationInfo }.let { if (it) 1 else 0 }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? {
        val token = tokens.find { it.tokenValidationInfo == tokenValidationInfo } ?: return null
        val user = users.find { it.id == token.userId } ?: return null
        return user to token
    }

    override fun updateTokenLastUsed(
        token: Token,
        lastUsedAt: Instant,
    ) {
        tokens.find { it.tokenValidationInfo == token.tokenValidationInfo }?.lastUsedAt = lastUsedAt
    }

    override fun findById(id: Int): User? = users.find { it.id == id }

    override fun findAll(): List<User> = users

    override fun save(entity: User) {
        users.add(entity)
    }

    override fun deleteById(id: Int) {
        users.removeIf { it.id == id }
    }
}
