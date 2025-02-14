package pt.isel.daw

import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("jdbi")
class UserControllerJdbi : AbstractUserControllerTest()
