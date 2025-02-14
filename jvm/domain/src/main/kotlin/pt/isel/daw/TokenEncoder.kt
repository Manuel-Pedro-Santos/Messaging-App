package pt.isel.daw

interface TokenEncoder {
    fun createValidationInformation(token: String): TokenValidationInfo
}