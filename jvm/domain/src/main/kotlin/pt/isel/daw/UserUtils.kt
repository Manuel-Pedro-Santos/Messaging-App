package pt.isel.daw

import jakarta.inject.Named
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.springframework.security.crypto.password.PasswordEncoder
import java.security.SecureRandom
import java.util.Base64.getUrlDecoder
import java.util.Base64.getUrlEncoder

@Named
class UserUtils(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UsersDomainConfig,
) {
    fun validatePassword(password: String, validationInfo: PasswordValidationInfo): Boolean {
        println("Password to validate: $password")
        println("Encoded password (validationInfo): ${validationInfo.validationInfo}")

        val doesMatch = passwordEncoder.matches(password, validationInfo.validationInfo)
        println("Does it match? $doesMatch")
        return doesMatch
    }


    fun createPasswordValidationInformation(password: String) =
        PasswordValidationInfo(
            validationInfo = passwordEncoder.encode(password),
        )

    fun generateTokenValue(): String =
        ByteArray(config.tokenSizeInBytes).let { byteArray ->
            SecureRandom.getInstanceStrong().nextBytes(byteArray)
            getUrlEncoder().encodeToString(byteArray)
        }

    fun canBeToken(token: String): Boolean =
        try {
            getUrlDecoder().decode(token).size == config.tokenSizeInBytes
        } catch (ex: IllegalArgumentException) {
            false
        }

    fun getTokenExpiration(token: Token): Instant {
        val absoluteExpiration = token.createdAt + config.tokenTtl
        val rollingExpiration = token.lastUsedAt + config.tokenRollingTtl
        return if (absoluteExpiration < rollingExpiration) {
            absoluteExpiration
        } else {
            rollingExpiration
        }
    }

    fun createTokenValidationInformation(token: String) = tokenEncoder.createValidationInformation(token)

    fun isTokenTimeValid(
        clock: Clock,
        token: Token,
    ): Boolean {
        val now = clock.now()
        return token.createdAt <= now && (now - token.createdAt) <= config.tokenTtl && (now - token.lastUsedAt) <= config.tokenRollingTtl
    }

    val maxNumberOfTokensPerUser = config.maxTokensPerUser
}
