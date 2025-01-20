package ch.ngns.or.vault.services.config

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@ConfigurationProperties("orvault.services")
data class ORVaultProperties(
    val encryptionSalt: String?,
    val encryptionKeyLength: Int?
)

@Configuration
@EnableConfigurationProperties(ORVaultProperties::class)
class ORVaultConfig {

    @Bean
    fun javaTimeModule() = JavaTimeModule()

    @Bean
    fun clock() = Clock.systemUTC()

}