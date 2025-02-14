import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pt.isel.daw.Sha256TokenEncoder

class Sha256TokenTests {

    @Test
    fun `test token creation and hashing`() {
        val encoder = Sha256TokenEncoder()
        val rawToken = "test-token"

        val validationInfo = encoder.createValidationInformation(rawToken)
        val hashedToken = validationInfo.validationInfo

        assertNotNull(hashedToken)
        assertTrue(hashedToken.isNotBlank())
    }

    @Test
    fun `test token validation`() {

        val encoder = Sha256TokenEncoder()
        val rawToken = "another-test-token"

        val validationInfo = encoder.createValidationInformation(rawToken)
        val hashedToken = validationInfo.validationInfo

        val validationInfoAgain = encoder.createValidationInformation(rawToken)
        val hashedTokenAgain = validationInfoAgain.validationInfo
        assertEquals(hashedToken, hashedTokenAgain)
    }

    @Test
    fun `test different tokens produce different hashes`() {
        val encoder = Sha256TokenEncoder()
        val token1 = "token-one"
        val token2 = "token-two"

        val hash1 = encoder.createValidationInformation(token1).validationInfo
        val hash2 = encoder.createValidationInformation(token2).validationInfo

        assertNotEquals(hash1, hash2)
    }
}
