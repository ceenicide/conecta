# Segurança JWT — Documentação

## Visão geral

O sistema usa **JWT (JSON Web Token)** para autenticar usuários. O fluxo é:

1. Usuário se registra ou faz login em `/auth/**`
2. Sistema devolve um token assinado
3. Nas próximas requisições, o usuário envia o token no header `Authorization: Bearer <token>`
4. O filtro valida o token antes de qualquer controller ser executado

---

## Dependências adicionadas (`pom.xml`)

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

---

## Propriedades (`application.yml`)

```yaml
jwt:
  secret: "3f8a2b1c9d4e7f6a0b5c8d2e1f4a7b3c9d6e0f2a5b8c1d4e7f0a3b6c9d2e5f8"
  expiration: 86400000  # 24 horas em milissegundos
```

> **Atenção:** em produção, o `jwt.secret` nunca fica no arquivo — vai em variável de ambiente.

---

## Classes implementadas

### `@Service` — `config/JwtService.java`

Responsável por **gerar, validar e ler tokens JWT**.

| Método | O que faz |
|---|---|
| `gerarToken(telefone)` | Cria um token assinado com o `secret`, válido por 24h |
| `extrairTelefone(token)` | Lê o `subject` dentro do token (identificador do usuário) |
| `tokenValido(token)` | Verifica assinatura e expiração — retorna `true` ou `false` |

**Por que `@Service`?** Porque contém lógica de negócio reutilizável. Pode ser injetado em qualquer outro componente via `@RequiredArgsConstructor`.

**Por que `@Value`?** As anotações `@Value("${jwt.secret}")` e `@Value("${jwt.expiration}")` injetam os valores do `application.yml` diretamente nos campos — sem precisar ler manualmente o arquivo.

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // gera token, extrai telefone, valida token
}
```

---

### `@Component` — `config/JwtFilter.java`

**Intercepta toda requisição HTTP** antes de chegar nos controllers. Estende `OncePerRequestFilter`, garantindo que o filtro executa exatamente uma vez por requisição.

**Fluxo interno:**
1. Lê o header `Authorization`
2. Se começa com `"Bearer "`, extrai o token
3. Valida o token via `JwtService`
4. Se válido, registra o usuário no `SecurityContextHolder`
5. Passa a requisição adiante (`filterChain.doFilter`)

**Por que `@Component`?** Para o Spring registrar e gerenciar o filtro automaticamente no ciclo de vida da aplicação.

```java
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(...) {
        // lê header → valida token → registra no SecurityContext
    }
}
```

---

### `@Configuration` `@EnableWebSecurity` — `config/SecurityConfig.java`

Define **quais rotas são públicas e quais exigem token**. Também registra o `JwtFilter` na cadeia de filtros do Spring Security.

**Regras de acesso configuradas:**

| Rota | Acesso |
|---|---|
| `POST /auth/registrar` | Público — qualquer um |
| `POST /auth/login` | Público — qualquer um |
| `GET /produtos/**` | Público — qualquer um |
| `GET /anuncios/**` | Público — qualquer um |
| Qualquer outra rota | Exige token JWT válido |

**Decisões técnicas:**

- `csrf().disable()` — desabilitado porque a API é stateless (sem sessão, sem formulários HTML)
- `SessionCreationPolicy.STATELESS` — o Spring não cria sessões. Cada requisição precisa trazer o token
- `addFilterBefore(jwtFilter, ...)` — garante que o `JwtFilter` executa **antes** do filtro padrão de autenticação do Spring

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/produtos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/anuncios/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

---

### DTO — `dto/AuthDTO.java`

Objeto de entrada e saída dos endpoints de autenticação.

| Campo | Entrada | Saída |
|---|---|---|
| `telefone` | Obrigatório | Retornado |
| `nome` | Obrigatório no registro | Retornado |
| `tipo` | Obrigatório no registro (`VENDEDOR` / `COMPRADOR`) | Retornado |
| `token` | Não enviado | Retornado com o JWT gerado |

---

### `@RestController` — `controller/AuthController.java`

Dois endpoints públicos — não exigem token.

#### `POST /auth/registrar`

Cria o usuário e devolve o token imediatamente.

```json
// Request
{
  "nome": "Maria",
  "telefone": "11999990000",
  "tipo": "VENDEDOR"
}

// Response
{
  "nome": "Maria",
  "telefone": "11999990000",
  "tipo": "VENDEDOR",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### `POST /auth/login`

Busca o usuário pelo telefone e devolve o token.

```json
// Request
{
  "telefone": "11999990000"
}

// Response
{
  "nome": "Maria",
  "telefone": "11999990000",
  "tipo": "VENDEDOR",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

## Como usar o token nas requisições

Após registrar ou fazer login, copie o `token` da resposta e adicione no header de todas as requisições protegidas:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Exemplo com curl:**
```bash
# 1. Registrar e obter token
curl -X POST http://localhost:8080/auth/registrar \
  -H "Content-Type: application/json" \
  -d '{"nome": "Maria", "telefone": "11999990000", "tipo": "VENDEDOR"}'

# 2. Usar o token em uma rota protegida
curl -X POST http://localhost:8080/anuncios \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token_aqui>" \
  -d '{"usuarioId": 1, "produtoId": 1, "quantidade": 100, "preco": 50.00}'
```

---

## Ajuste nos testes — `ConectaApplicationTests.java`

O `@SpringBootTest` sobe o contexto completo da aplicação, incluindo o `JwtService`. Como ele depende das propriedades `jwt.secret` e `jwt.expiration`, o `@TestPropertySource` injeta esses valores diretamente no contexto de teste.

```java
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
```

**Por que isso é necessário?** Os testes de service e controller usam `@ExtendWith(MockitoExtension.class)` — eles não sobem o Spring, então não precisam das propriedades. O `ConectaApplicationTests` é o único que sobe o contexto real, por isso precisa do `@TestPropertySource`.

---

## Fluxo completo de uma requisição autenticada

```
Cliente
  ↓  Authorization: Bearer <token>
JwtFilter (OncePerRequestFilter)
  ↓  tokenValido() → extrairTelefone()
  ↓  registra no SecurityContextHolder
SecurityConfig (verifica se rota exige autenticação)
  ↓  autenticado → segue
Controller
  ↓  chama Service
  ↓  retorna resposta
Cliente
  ↑  200 OK + dados
```

Se o token for inválido ou ausente em uma rota protegida:
```
Cliente
  ↑  401 Unauthorized
```