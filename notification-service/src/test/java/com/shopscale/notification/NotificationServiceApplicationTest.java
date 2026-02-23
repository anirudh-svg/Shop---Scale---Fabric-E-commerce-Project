package com.shopscale.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.mail.MailHealthContributorAutoConfiguration"
})
@TestPropertySource(locations = "classpath:application-test.yml")
class NotificationServiceApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
    }
}
