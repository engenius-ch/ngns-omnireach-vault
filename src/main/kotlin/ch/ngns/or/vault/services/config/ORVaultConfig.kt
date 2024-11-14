package ch.ngns.or.vault.services.config

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

const val CORRELATION_ID_HEADER= "x-correlation-id"
const val TENANT_ID_HEADER = "x-tenant-id"

@ConfigurationProperties("orvault.services")
data class ORVaultProperties(
    var encryptionSalt: String?,
    var encryptionKeyLength: Int?
)

@Configuration
@EnableConfigurationProperties(ORVaultProperties::class)
class ORVaultConfig {

    private val logger = LoggerFactory.getLogger(ORVaultConfig::class.java)

    @Bean
    fun javaTimeModule() = JavaTimeModule()

    @Bean
    fun clock() = Clock.systemUTC()

}