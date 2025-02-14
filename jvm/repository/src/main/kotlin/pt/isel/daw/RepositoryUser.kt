package pt.isel.daw

import kotlinx.datetime.Instant

interface RepositoryUser : Repository<User> {
    fun createUser(
        email: String,
        username: String,
        hashedPass: PasswordValidationInfo,
        registryToken: String,
    ): User

    fun findByUsername(username: String): User?

    fun findByEmail(email: String): User?

    fun saveToken(
        token: String,
        user: User,
    )

    fun findByName(name: String): User?
    fun clear()

    fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>?

    fun createToken(
        token: Token,
        maxTokens: Int,
    )

    fun updateTokenLastUsed(
        token: Token,
        lastUsedAt: Instant,
    )

    fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int
}
