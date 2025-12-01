package org.example.localy;

import org.example.localy.service.Chat.TranslationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
//@TestPropertySource(properties = {
//        "spring.cloud.gcp.project-id=test-project",
//        "spring.cloud.gcp.credentials.enabled=false"
//})
@ActiveProfiles("test")
class LocalyApplicationTests {

    @MockBean
    private TranslationService translationService;

    @Test
    void contextLoads() {
    }

}
