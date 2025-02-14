package pt.isel.daw

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import pt.isel.daw.mem.TransactionManagerMem

@Component
class TestConfig {
    @Bean
    @Profile("inMem")
    fun trxManagerInMem(): TransactionManager = TransactionManagerMem()
}
