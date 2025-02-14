package pt.isel.daw

data class User(
    val id: Int,
    val email: String,
    val username: String,
    val passwordValidationInfo: PasswordValidationInfo,
    val invitations: List<Invitation> = emptyList(),
    val channels: List<Channel> = emptyList(),
) {
    init {
        require(email.isNotBlank()) { "email can't be blank" }
        require(username.isNotBlank()) { "username can't be blank" }
        require(passwordValidationInfo.validationInfo.isNotBlank()) { "password can't be blank" }
        require(Regex("^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+\$").matches(email)) {
            "email is not in a valid format"
        }
    }
}
