package com.feira.conecta.dto.auth;

import com.feira.conecta.domain.TipoUsuario;

/**
 * DTO de saída para autenticação (registro e login).
 *
 * Expõe apenas o necessário ao frontend:
 * id, nome e tipo para personalizar a UI, token para autenticar requisições.
 * A senha NUNCA aparece aqui — separação request/response garante isso.
 */
public record LoginResponse(
        Long id,
        String nome,
        String telefone,
        TipoUsuario tipo,
        String token
) {}