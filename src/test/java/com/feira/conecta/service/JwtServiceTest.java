package com.feira.conecta.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.feira.conecta.config.JwtService;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setup() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "3f8a2b1c9d4e7f6a0b5c8d2e1f4a7b3c9d6e0f2a5b8c1d4e7f0a3b6c9d2e5f8");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
    }

    @Test
    void deveGerarTokenValido() {
        String token = jwtService.gerarToken("11111111111");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void deveExtrairTelefoneDoToken() {
        String token = jwtService.gerarToken("11111111111");
        String telefone = jwtService.extrairTelefone(token);
        assertThat(telefone).isEqualTo("11111111111");
    }

    @Test
    void deveValidarTokenValido() {
        String token = jwtService.gerarToken("11111111111");
        assertThat(jwtService.tokenValido(token)).isTrue();
    }

    @Test
    void deveRetornarFalsoParaTokenInvalido() {
        assertThat(jwtService.tokenValido("token.invalido.qualquer")).isFalse();
    }

    @Test
    void deveRetornarFalsoParaTokenVazio() {
        assertThat(jwtService.tokenValido("")).isFalse();
    }

    @Test
    void deveGerarTokensDiferentesParaTelefonesDiferentes() {
        String token1 = jwtService.gerarToken("11111111111");
        String token2 = jwtService.gerarToken("22222222222");
        assertThat(token1).isNotEqualTo(token2);
    }
}