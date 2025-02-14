package pt.isel.daw

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pt.isel.daw.model.*

@RestController
class UserController(
    private val userService: UserService,
) {
    // _________________________ GET USERS ______________________________
    @GetMapping("/api/users")
    fun getAllUsers(): ResponseEntity<*> {
        val result: Either<UserError, List<User>> = userService.getAllUsers()
        return when (result) {
            is Success -> ResponseEntity.ok(result.value)
            is Failure ->
                when (result.value) {
                    is UserError.UserNotFound ->
                        Problem.UserNotFound.response(
                            HttpStatus.NOT_FOUND,
                        )

                    else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
                }
        }
    }

    @GetMapping("/api/users/search")
    fun searchUsersByName(
        @RequestParam name: String,
    ): ResponseEntity<*> {
        val result: Either<UserError, User?> = userService.searchUsersByName(name)
        return when (result) {
            is Success -> ResponseEntity.ok(result.value)
            is Failure ->
                when (result.value) {
                    is UserError.UserNotFound ->
                        Problem.UserNotFound.response(
                            HttpStatus.NOT_FOUND,
                        )

                    else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
                }
        }
    }

    @GetMapping("/api/users/{id}")
    fun getUserById(

        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val result: Either<UserError, User> = userService.getUserById(id)
        return when (result) {
            is Success -> ResponseEntity.ok(result.value)
            is Failure ->
                when (result.value) {
                    is UserError.UserNotFound ->
                        Problem.UserNotFound.response(
                            HttpStatus.NOT_FOUND,
                        )

                    else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
                }
        }
    }

    @PostMapping("/api/users/login")
    fun login(
        @RequestBody userLogin: UserCreateTokenInputModel,
    ): ResponseEntity<*> {
        val result: Either<TokenCreationError, TokenExternalInfo> =
            userService.createToken(userLogin.email, userLogin.password)

        return when (result) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(UserCreateTokenOutputModel(result.value.tokenValue))
            is Failure ->
                when (result.value) {
                    is TokenCreationError.EmailOrPasswordAreInvalid ->
                        Problem.InvalidCredentials.response(
                            HttpStatus.UNAUTHORIZED,
                        )

                    else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
                }
        }
    }

    @PostMapping("/api/users/logout")
    fun logout(user: AuthenticatedUser) {
        userService.revokeToken(user.token)
    }

    @PostMapping("/api/users/create", consumes = ["application/json"])
    fun createUser(
        @RequestBody participantInput: UserInput,
    ): ResponseEntity<*> {
        val result: Either<UserError, User> =
            userService.createUser(
                participantInput.email,
                participantInput.username,
                participantInput.password,
                participantInput.registryToken,
            )

        return when (result) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/users/${result.value.id}")
                    .build<Unit>()

            is Failure ->
                when (result.value) {
                    is UserError.UserAlreadyExists ->
                        Problem.UserAlreadyAllocated.response(
                            HttpStatus.CONFLICT,
                        )

                    is UserError.EmailAlreadyExists ->
                        Problem.UserAlreadyAllocated.response(
                            HttpStatus.CONFLICT,
                        )

                    else -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
                }
        }
    }

    @GetMapping("/api/me")
    fun userHome(userAuthenticatedUser: AuthenticatedUser): UserHomeOutputModel =
        UserHomeOutputModel(
            id = userAuthenticatedUser.user.id,
            name = userAuthenticatedUser.user.username,
            email = userAuthenticatedUser.user.email,
        )
}
