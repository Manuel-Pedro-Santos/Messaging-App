package pt.isel.daw

import jakarta.inject.Named
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

sealed class UserError {
    data object ChannelNotFound : UserError()

    data object UserNotFound : UserError()

    data object UserAlreadyExists : UserError()

    data object EmailAlreadyExists : UserError()

    data object InvitationNotFound : UserError()

    data object InvalidCredentials : UserError()

    data object HasToBeTheOwnerOfTheChannel : UserError()

    data object RegistrationTokenAlreadyInUse : UserError()

    data object RegistrationTokenNotRecognised : UserError()
}

sealed class MessageError {
    data object ChannelNotFound : MessageError()

    data object UserNotFound : MessageError()

    data object UserCannotSendMessages : MessageError()
}

sealed class TokenCreationError {
    data object EmailOrPasswordAreInvalid : TokenCreationError()
}

data class TokenExternalInfo(
    val tokenValue: String,
    val tokenExpiration: Instant,
)

@Named
class UserService(
    private val transactionManager: TransactionManager,
    private val userUtils: UserUtils,
    private val clock: Clock,
) {
    // __________________________ SEARCH USER __________________________
    fun getAllUsers(): Either<UserError, List<User>> =
        transactionManager.run {
            val users: List<User> = repoUser.findAll()
            success(users)
        }

    fun getUserById(userId: Int): Either<UserError, User> =
        transactionManager.run {
            val user: User = repoUser.findById(userId) ?: return@run failure(UserError.UserNotFound)
            success(user)
        }

    fun searchUsersByName(name: String): Either<UserError.UserNotFound, User> =
        transactionManager.run {
            val users: User? = repoUser.findByName(name)
            if (users == null) return@run failure(UserError.UserNotFound)
            success(users)
        }

    // __________________________ CREATE USER __________________________
    fun createUser(
        email: String,
        username: String,
        password: String,
        registryToken: String,
    ): Either<UserError, User> =
        transactionManager.run {
            val checkUsername = repoUser.findByUsername(username)
            if (checkUsername != null) return@run failure(UserError.UserAlreadyExists)

            val checkEmail = repoUser.findByUsername(email)
            if (checkEmail != null) return@run failure(UserError.EmailAlreadyExists)

            val hashPass = userUtils.createPasswordValidationInformation(password)
            val user: User =
                repoUser.createUser(email, username, hashPass, registryToken)
            success(user)
        }

    // __________________________ LOGIN __________________________
    fun createToken(
        email: String,
        password: String,
    ): Either<TokenCreationError, TokenExternalInfo> {
        if (email.isBlank() || password.isBlank()) {
            failure(TokenCreationError.EmailOrPasswordAreInvalid)
        }
        return transactionManager.run {
            val user: User =
                repoUser.findByEmail(email) ?: return@run failure(TokenCreationError.EmailOrPasswordAreInvalid)
            if (!userUtils.validatePassword(password, user.passwordValidationInfo)) {
                return@run failure(TokenCreationError.EmailOrPasswordAreInvalid)
            }
            val tokenValue = userUtils.generateTokenValue()
            val now = clock.now()
            val newToken =
                Token(
                    userUtils.createTokenValidationInformation(tokenValue),
                    user.id,
                    createdAt = now,
                    lastUsedAt = now,
                )
            repoUser.createToken(newToken, userUtils.maxNumberOfTokensPerUser)
            Either.Right(
                TokenExternalInfo(
                    tokenValue,
                    userUtils.getTokenExpiration(newToken),
                ),
            )
        }
    }

    fun revokeToken(token: String): Boolean {
        val tokenValidationInfo = userUtils.createTokenValidationInformation(token)
        return transactionManager.run {
            repoUser.removeTokenByValidationInfo(tokenValidationInfo)
            true
        }
    }

    fun getUserByToken(token: String): User? {
        if (!userUtils.canBeToken(token)) {
            return null
        }
        return transactionManager.run {
            val tokenValidationInfo = userUtils.createTokenValidationInformation(token)
            val userAndToken: Pair<User, Token>? = repoUser.getTokenByTokenValidationInfo(tokenValidationInfo)
            if (userAndToken != null && userUtils.isTokenTimeValid(clock, userAndToken.second)) {
                repoUser.updateTokenLastUsed(userAndToken.second, clock.now())
                userAndToken.first
            } else {
                null
            }
        }
    }

    // __________________________ INVITATIONS __________________________

    fun createRegistryToken(): String =
        // FOR TESTING PURPOSES
        transactionManager.run {
            repoInvitation.createRegistryToken()
        }

    fun sendInvitationToNewUser(email: String, senderEmail: String): Either<UserError, Boolean> =
        transactionManager.run {
            val user: User? = repoUser.findByEmail(email)
            if (user != null) return@run failure(UserError.UserAlreadyExists)
            val newTokenForUser = repoInvitation.createRegistryToken()
            val mailService = MailService()
            mailService.sendEmail(email, "TOKEN FOR G01 MESSAGING APP", newTokenForUser, senderEmail)
            success(true)
        }

    fun sendInvitationToAlreadyAuth(
        owner: Int,
        userId: Int,
        channelId: Int,
        invitationType: InvitationType,
    ): Either<UserError, Invitation> =
        transactionManager.run {
            val o = repoUser.findById(owner) ?: return@run failure(UserError.UserNotFound)
            val user: User = repoUser.findById(userId) ?: return@run failure(UserError.UserNotFound)
            val channel: Channel = repoChannel.findById(channelId) ?: return@run failure(UserError.ChannelNotFound)

            check(o == channel.owner) { return@run failure(UserError.HasToBeTheOwnerOfTheChannel) }
            val invitation: Invitation = repoInvitation.sendInvitationAuthenticated(channel, user, invitationType)
            success(invitation)
        }

    fun findAllInvitations(userId: Int): Either<UserError, List<Invitation>> =
        transactionManager.run {
            val user: User = repoUser.findById(userId) ?: return@run failure(UserError.UserNotFound)
            val invitations: List<Invitation> = repoInvitation.findInvitationsForUser(user) ?: emptyList()//@run failure(UserError.InvitationNotFound)
            success(invitations)
        }

    fun acceptInvitationAlreadyAuthenticated(
        invitationId: Int,
        userId: Int,
    ): Either<UserError, Channel> =
        transactionManager.run {
            val invitation: Invitation =
                repoInvitation.findById(invitationId) ?: return@run failure(UserError.InvitationNotFound)
            val user: User = repoUser.findById(userId) ?: return@run failure(UserError.UserNotFound)
            val channel: Channel =
                repoChannel.findById(invitation.channel.id) ?: return@run failure(UserError.ChannelNotFound)
            if (channel is SingleChannel) {
                repoChannel
                    .userJoinChannelSingle(channel, user)
                    .also { repoInvitation.removeInvitation(invitation) }
            } else {
                repoChannel
                    .userJoinChannelGroup(
                        channel,
                        user,
                        invitation.invitationType,
                    ).also { repoInvitation.removeInvitation(invitation) }
            }
            val retChannel = repoChannel.findById(channel.id) ?: return@run failure(UserError.ChannelNotFound)
            success(retChannel)
        }

    fun removeInvitation(invitationId: Int): Either<ChannelError, Unit> =
        transactionManager.run {
            val invitation =
                repoInvitation.findById(invitationId) ?: return@run failure(ChannelError.InvitationNotFound)
            repoInvitation.removeInvitation(invitation)
            success(Unit)
        }

    // ____________________________ MESSAGES RELATED _______________________________________



    fun sendMessage(
        channelId: Int,
        userId: Int,
        message: String,
    ): Either<MessageError, Message> =
        transactionManager.run {
            val channel: Channel = repoChannel.findById(channelId) ?: return@run failure(MessageError.ChannelNotFound)
            val user: User = repoUser.findById(userId) ?: return@run failure(MessageError.UserNotFound)
            val isPublicChannel = repoChannel.findPublicChannels()?.any { it.id == channel.id } == true

            if (!isPublicChannel && !repoChannel.isUserInChannelAndCanSendMessages(channel, user)) {
                return@run failure(MessageError.UserNotFound)
            }
            val msg: Message = repoMessage.createMessage(channel, user, message)
            success(msg)
        }


//    fun sendMessage(
//        channelId: Int,
//        userId: Int,
//        message: String,
//    ): Either<MessageError, Message> =
//        transactionManager.run {
//            val channel: Channel = repoChannel.findById(channelId) ?: return@run failure(MessageError.ChannelNotFound)
//            val user: User = repoUser.findById(userId) ?: return@run failure(MessageError.UserNotFound)
////            check(
////                repoChannel.isUserInChannelAndCanSendMessages(
////                    channel,
////                    user,
////                ),
////            ) { return@run failure(MessageError.UserNotFound) }
//            if (!repoChannel.isUserInChannelAndCanSendMessages(channel, user)) {
//                return@run failure(MessageError.UserNotFound)
//            }
//            val msg: Message = repoMessage.createMessage(channel, user, message)
//            success(msg)
//        }

    fun readMessages(
        channelId: Int,
        userId: Int,
    ): Either<MessageError, List<Message>> {
        return transactionManager.run {
            val user: User = repoUser.findById(userId) ?: return@run failure(MessageError.UserNotFound)
            val channel: Channel = repoChannel.findById(channelId) ?: return@run failure(MessageError.ChannelNotFound)

            val isPublicChannel = repoChannel.findPublicChannels()?.any { it.id == channel.id } == true

            val isUserInChannel = repoChannel.isUserInChannel(channel, user)

            if (!isPublicChannel && !isUserInChannel) {
                return@run failure(MessageError.ChannelNotFound)
            }

            val messages: List<Message> = repoMessage.findMessagesOfChannel(channel).orEmpty()

            success(messages)
        }
    }
}
//    fun readMessages(
//        channelId: Int,
//        userId: Int,
//    ): Either<MessageError, List<Message>> {
//        return transactionManager.run {
//            val user: User = repoUser.findById(userId) ?: return@run failure(MessageError.UserNotFound)
//            val channel: Channel = repoChannel.findById(channelId) ?: return@run failure(MessageError.ChannelNotFound)
//            //check(repoChannel.isUserInChannel(channel, user)) { return@run failure(MessageError.ChannelNotFound) }
//
//            if (!repoChannel.isUserInChannel(channel, user ) || repoChannel.findPublicChannels()?.firstOrNull{ it == channel } == null ) {
//                return@run failure(MessageError.ChannelNotFound)
//            }
//            val messages: List<Message> = repoMessage.findMessagesOfChannel(channel).orEmpty()
//
//            if (messages.isEmpty()) {
//                return@run success(emptyList())
//            }
////            val messages: List<Message> =
////                repoMessage.findMessagesOfChannel(channel) ?: return@run failure(MessageError.ChannelNotFound)
//
//            success(messages)
//        }
//    }
//}
