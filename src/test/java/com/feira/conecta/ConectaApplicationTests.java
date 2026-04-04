package com.feira.conecta;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=3f8a2b1c9d4e7f6a0b5c8d2e1f4a7b3c9d6e0f2a5b8c1d4e7f0a3b6c9d2e5f8",
    "jwt.expiration=86400000"
})
class ConectaApplicationTests {

    @Test
    void contextLoads() {
    }
}