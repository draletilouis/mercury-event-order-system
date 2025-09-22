package com.mercury.orders.orders.config

// Temporarily commented out due to actuator dependency issues
/*
import org.springframework.boot.actuator.health.Health
import org.springframework.boot.actuator.health.HealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource
import org.springframework.jdbc.core.JdbcTemplate

@Configuration
class HealthConfiguration {

    @Bean
    fun databaseHealthIndicator(dataSource: DataSource): HealthIndicator {
        return HealthIndicator { 
            try {
                val jdbcTemplate = JdbcTemplate(dataSource)
                jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
                
                Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connected")
                    .build()
            } catch (e: Exception) {
                Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Disconnected")
                    .withDetail("error", e.message)
                    .build()
            }
        }
    }
}
*/
