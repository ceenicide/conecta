# Conecta Feira — Documentação Completa do Projeto

## Visão geral

Sistema de conexão entre vendedores e compradores com dois mercados:
- **Mercado imediato** — compra e venda de produtos disponíveis agora
- **Mercado futuro** — planejamento antecipado com matching automático entre ofertas e demandas

**Stack:** Java 25 · Spring Boot 4 · MySQL 8 · Docker · JWT · Swagger

---

## Estrutura de pacotes

```
src/main/java/com/feira/conecta/
├── config/          configurações da aplicação (segurança, JWT, Swagger)
├── controller/      entrada HTTP — recebe requisições e devolve respostas
├── domain/          entidades do banco de dados
├── dto/             objetos de transferência de dados (entrada e saída da API)
├── exception/       tratamento centralizado de erros
├── repository/      acesso ao banco de dados
└── service/         regras de negócio
```

---

## Como os pacotes se comunicam

```
Cliente HTTP (Swagger / Postman / App)
        ↓  requisição com JSON + token JWT
[config/JwtFilter]         intercepta e valida o token
        ↓  se válido, passa adiante
[controller/]              recebe a requisição, valida o DTO com @Valid
        ↓  chama o service
[service/]                 aplica as regras de negócio
        ↓  chama o repository
[repository/]              executa o SQL via Hibernate
        ↓  retorna a entidade
[service/]                 converte entidade → DTO (método toDTO)
        ↑  retorna DTO
[controller/]              encapsula em ResponseEntity e devolve
        ↑  resposta JSON
Cliente HTTP
```

**Regra fundamental:** cada camada só conhece a camada imediatamente abaixo.
- Controller não acessa repository diretamente
- Repository não tem regra de negócio
- Service não conhece HTTP

---

## Pacote `config/`

Contém a infraestrutura de segurança e documentação. Não contém regras de negócio.

---

### `JwtService.java`

```java
@Service
public class JwtService {
    @Value("${jwt.secret}")   // lê do application.yml
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;
}
```

**`@Service`** — registra a classe como bean do Spring, permitindo injeção via `@RequiredArgsConstructor` em qualquer outro componente.

**`@Value`** — injeta valores do `application.yml` diretamente nos campos. Sem isso, o Spring não saberia de onde vir o secret e o expiration.

| Método | O que faz |
|---|---|
| `gerarToken(telefone)` | Cria um JWT assinado com HMAC-SHA384, com `subject = telefone` e expiração de 24h |
| `extrairTelefone(token)` | Lê o campo `subject` dentro do payload do token |
| `tokenValido(token)` | Verifica assinatura e expiração — retorna `true` ou `false` sem lançar exceção |

**Por que `telefone` como subject?** Porque é o identificador único do usuário no sistema — mais simples que usar `id` que exigiria mais uma consulta ao banco.

---

### `JwtFilter.java`

```java
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) { ... }
}
```

**`@Component`** — registra como bean do Spring. O Spring Boot detecta automaticamente que é um filtro e o registra na cadeia HTTP.

**`extends OncePerRequestFilter`** — garante que o filtro executa exatamente uma vez por requisição, mesmo em redirecionamentos internos.

**`@RequiredArgsConstructor`** — Lombok gera um construtor com todos os campos `final`. O Spring injeta `JwtService` automaticamente.

**Fluxo interno:**
```
requisição chega
    → lê header "Authorization"
    → se começa com "Bearer ": extrai o token
    → chama jwtService.tokenValido(token)
    → se válido: extrai telefone e registra no SecurityContextHolder
    → filterChain.doFilter() — passa para o próximo filtro/controller
```

O `SecurityContextHolder` é onde o Spring Security guarda quem está autenticado na requisição atual. Sem registrar aqui, o Spring não saberia que o usuário está autenticado.

---

### `SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { ... }

    @Bean
    public AuthenticationProvider authenticationProvider() { ... }

    @Bean
    public PasswordEncoder passwordEncoder() { ... }

    @Bean
    public AuthenticationManager authenticationManager(...) { ... }
}
```

**`@Configuration`** — diz ao Spring que essa classe define beans. Todos os métodos anotados com `@Bean` são registrados no contexto.

**`@EnableWebSecurity`** — ativa o módulo de segurança do Spring. Sem isso, nenhuma regra de acesso é aplicada.

**`@Bean`** — cada método anotado devolve um objeto gerenciado pelo Spring. O `filterChain` é o mais importante: define quais rotas são públicas e quais exigem token.

**Decisões de segurança no `filterChain`:**

| Configuração | Por quê |
|---|---|
| `csrf().disable()` | API REST stateless não usa CSRF — sem cookies de sessão, o ataque não é possível |
| `SessionCreationPolicy.STATELESS` | Sem sessão no servidor — cada requisição precisa trazer o token |
| `addFilterBefore(jwtFilter, ...)` | JwtFilter executa antes do filtro padrão do Spring Security |

**Rotas públicas:**
```
/auth/**              → registro e login
GET /produtos/**      → qualquer um pode ver produtos
GET /anuncios/**      → qualquer um pode ver anúncios
GET /ofertas-futuras/** → qualquer um pode ver ofertas
GET /demandas/**      → qualquer um pode ver demandas
GET /matchings/**     → qualquer um pode ver matchings
/swagger-ui/**        → documentação pública
/v3/api-docs/**       → spec pública
```

---

### `CustomUserDetailsService.java`

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String telefone) { ... }
}
```

**`implements UserDetailsService`** — contrato do Spring Security. Quando o Spring precisa verificar quem é um usuário, chama este método. O "username" aqui é o telefone.

**O que retorna:** um `UserDetails` com `username = telefone`, `password = ""` (sem senha no MVP) e `roles = VENDEDOR` ou `COMPRADOR`. O Spring Security usa isso para popular o contexto de segurança.

---

### `SwaggerConfig.java`

```java
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() { ... }
}
```

Configura o Swagger para mostrar o campo de autenticação JWT. Sem isso, o Swagger não saberia que a API usa Bearer token e não exibiria o botão "Authorize".

---

## Pacote `domain/`

Espelho do banco de dados. Cada classe é uma tabela. O Hibernate lê as anotações e gera o SQL automaticamente.

---

### Anotações comuns a todas as entidades

```java
@Entity                           // essa classe é uma tabela
@Table(name = "nome_da_tabela")   // nome exato da tabela no banco
@Getter @Setter                   // Lombok gera getters e setters
@NoArgsConstructor                // Lombok gera construtor sem argumentos (exigido pelo JPA)
@AllArgsConstructor               // Lombok gera construtor com todos os argumentos
@Builder                          // Lombok permite criar objetos com padrão builder
```

**Por que `@NoArgsConstructor` é obrigatório?** O JPA instancia as entidades via reflexão usando o construtor sem argumentos. Sem ele, o Hibernate não consegue criar objetos ao fazer SELECT.

**Por que `@Builder`?** Permite criar objetos de forma legível sem construtores enormes:
```java
// sem builder
new Usuario(null, "Maria", "11999999999", TipoUsuario.VENDEDOR, null);

// com builder
Usuario.builder().nome("Maria").telefone("11999999999").tipo(TipoUsuario.VENDEDOR).build();
```

---

### Campos de ID

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**`@Id`** — marca como chave primária.
**`@GeneratedValue(IDENTITY)`** — o banco gera o valor automaticamente (AUTO_INCREMENT no MySQL).

---

### Campos simples

```java
@Column(nullable = false)
private String nome;

@Column(nullable = false, unique = true)
private String telefone;

@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;
```

**`nullable = false`** — gera `NOT NULL` no banco. Se passar `null`, o banco rejeita.
**`unique = true`** — gera `UNIQUE INDEX`. Se tentar inserir duplicado, o banco rejeita.
**`updatable = false`** — o Hibernate nunca inclui esse campo em `UPDATE`. Garante que `createdAt` nunca muda após a criação.

---

### Campos enum

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private TipoUsuario tipo;
```

**`@Enumerated(EnumType.STRING)`** — salva o nome do enum (`"VENDEDOR"`, `"COMPRADOR"`) em vez do índice numérico (`0`, `1`). Importante: se usar `EnumType.ORDINAL` e adicionar um valor no meio do enum, todos os registros antigos ficam com valores errados.

---

### Relacionamentos

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "usuario_id", nullable = false)
private Usuario usuario;
```

**`@ManyToOne`** — muitos `Anuncio` para um `Usuario`. O Hibernate cria a coluna `usuario_id` como chave estrangeira.

**`FetchType.LAZY`** — não carrega o `Usuario` do banco automaticamente quando busca um `Anuncio`. Só carrega quando você acessar `anuncio.getUsuario()`. Sem isso (`EAGER`), cada SELECT de anúncio geraria JOINs desnecessários.

**`@JoinColumn(name = "usuario_id")`** — define o nome exato da coluna de chave estrangeira no banco.

---

### `@Builder.Default`

```java
@Builder.Default
@Column(nullable = false, updatable = false)
private LocalDateTime createdAt = LocalDateTime.now();
```

**Por que é necessário?** O Lombok `@Builder` ignora valores padrão de campos. Sem `@Builder.Default`, o builder passa `null` mesmo que você tenha escrito `= LocalDateTime.now()`. O compilador avisa disso com um warning — e o banco rejeita com `Column 'created_at' cannot be null`.

---

## Pacote `dto/`

DTOs são objetos de transferência. A API nunca expõe as entidades diretamente.

**Por que separar entidade de DTO?**
- A entidade tem campos internos que não devem ser expostos (ex: `createdAt` não deve ser enviável)
- Você pode mudar a estrutura do banco sem quebrar o contrato da API
- Validações ficam no DTO, não na entidade

---

### Anotações de validação nos DTOs

```java
@NotNull(message = "Campo é obrigatório")
@NotBlank(message = "Campo não pode ser vazio")
@Size(min = 2, max = 100, message = "Deve ter entre 2 e 100 caracteres")
@DecimalMin(value = "0.01", message = "Deve ser maior que zero")
@Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Telefone inválido")
@Future(message = "Data deve ser futura")
```

Essas anotações só funcionam quando o controller usa `@Valid` no parâmetro. Sem `@Valid`, as validações são ignoradas silenciosamente.

**Diferença entre `@NotNull` e `@NotBlank`:**
- `@NotNull` — rejeita `null` mas aceita `""`
- `@NotBlank` — rejeita `null`, `""` e `"   "` (só espaços)

---

## Pacote `exception/`

---

### `ResourceNotFoundException.java`

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String mensagem) {
        super(mensagem);
    }
}
```

Exceção customizada para "recurso não encontrado". Estende `RuntimeException` para não precisar de `throws` na assinatura dos métodos — o Spring captura automaticamente no handler global.

---

### `ErrorResponse.java`

```java
public record ErrorResponse(
    int status,
    String erro,
    String mensagem,
    LocalDateTime timestamp
) {}
```

**`record`** — sintaxe do Java 16+. Gera automaticamente construtor, getters, `equals`, `hashCode` e `toString`. Imutável por padrão — ideal para objetos de resposta que nunca mudam após criação.

---

### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) { ... }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) { ... }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) { ... }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) { ... }
}
```

**`@RestControllerAdvice`** — intercepta exceções de todos os controllers. Sem isso, cada controller precisaria de try/catch próprio e as respostas de erro seriam inconsistentes.

**`@ExceptionHandler`** — define qual tipo de exceção esse método trata. O Spring verifica a hierarquia de classes — `ResourceNotFoundException` é tratada antes de `Exception` genérica.

**Mapeamento de exceções para HTTP:**

| Exceção | HTTP | Quando ocorre |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entidade não encontrada no banco |
| `IllegalArgumentException` | 400 | Regra de negócio violada (ex: comprador tentou criar anúncio) |
| `MethodArgumentNotValidException` | 400 | Validação de DTO falhou (`@Valid`) |
| `Exception` | 500 | Qualquer erro não previsto |

---

## Pacote `repository/`

```java
@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    // métodos gerados pelo Spring Data
}
```

**`@Repository`** — marca como componente de acesso a dados. O Spring cria uma implementação em tempo de execução — você nunca implementa a interface manualmente.

**`extends JpaRepository<Produto, Long>`** — herda automaticamente: `save`, `findById`, `findAll`, `deleteById`, `existsById` e mais 10+ métodos.

**Métodos por nome do método (query methods):**

```java
Optional<Usuario> findByTelefone(String telefone);
// → SELECT * FROM usuarios WHERE telefone = ?

List<Anuncio> findByUsuarioIdAndStatus(Long usuarioId, StatusAnuncio status);
// → SELECT * FROM anuncios WHERE usuario_id = ? AND status = ?

boolean existsByTelefone(String telefone);
// → SELECT COUNT(*) > 0 FROM usuarios WHERE telefone = ?

List<OfertaFutura> findByProdutoAndStatusAndDataDisponivelLessThanEqual(
    Produto produto, StatusOferta status, LocalDate dataLimite);
// → SELECT * FROM ofertas_futuras
//   WHERE produto_id = ? AND status = ? AND data_disponivel <= ?
```

O Spring Data analisa o nome do método e gera o SQL automaticamente. As palavras-chave são: `findBy`, `And`, `Or`, `LessThan`, `LessThanEqual`, `GreaterThan`, `Like`, `Between`, `existsBy`, `countBy`, `deleteBy`.

---

## Pacote `service/`

Toda regra de negócio vive aqui. Os services nunca conhecem HTTP — não sabem que existe um controller acima deles.

---

### Anotações comuns

```java
@Service
@RequiredArgsConstructor
public class ProdutoService {
    private final ProdutoRepository repository;
}
```

**`@Service`** — especialização de `@Component`. Indica semanticamente que é uma classe de serviço. Registra como bean do Spring.

**`@RequiredArgsConstructor`** — Lombok gera um construtor com todos os campos `final`. O Spring injeta as dependências automaticamente. É a forma recomendada de injeção de dependência em vez de `@Autowired`.

---

### `@Transactional`

```java
@Transactional
public ProdutoDTO criar(UsuarioDTO dto) { ... }

@Transactional(readOnly = true)
public List<ProdutoDTO> listarTodos() { ... }
```

**`@Transactional`** — o Spring abre uma transação no banco antes do método e faz `COMMIT` ao final. Se qualquer exceção for lançada, faz `ROLLBACK`. Garante que operações complexas são atômicas.

**`readOnly = true`** — avisa o banco que não haverá escrita. O MySQL pode otimizar leituras e o Hibernate não rastreia mudanças nos objetos carregados. Sempre use em métodos de consulta.

**Exemplo do por que importa:**
```java
// sem @Transactional — PERIGOSO
public MatchingDTO aceitar(Long id) {
    matching.setStatus(ACEITO);    // salvo
    oferta.setStatus(FECHADA);     // salvo
    demanda.setStatus(ATENDIDA);   // falha aqui → matching e oferta já foram salvos
    // banco ficou inconsistente
}

// com @Transactional — SEGURO
@Transactional
public MatchingDTO aceitar(Long id) {
    matching.setStatus(ACEITO);    // não salvo ainda
    oferta.setStatus(FECHADA);     // não salvo ainda
    demanda.setStatus(ATENDIDA);   // falha → ROLLBACK de tudo
    // banco continua consistente
}
```

---

### Método `toDTO` em todos os services

```java
private ProdutoDTO toDTO(Produto produto) {
    return ProdutoDTO.builder()
            .id(produto.getId())
            .nome(produto.getNome())
            .descricao(produto.getDescricao())
            .build();
}
```

Método privado responsável por converter entidade em DTO. Centraliza a conversão — se o DTO mudar, só altera aqui. Nunca retorna a entidade diretamente para o controller.

---

## Pacote `controller/`

Camada HTTP. Não tem regra de negócio — só recebe, delega e responde.

---

### Anotações comuns

```java
@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Gerenciamento de produtos")
public class ProdutoController {
    private final ProdutoService service;
}
```

**`@RestController`** — combinação de `@Controller` + `@ResponseBody`. Indica que todos os métodos retornam JSON automaticamente, sem precisar anotar cada um.

**`@RequestMapping("/produtos")`** — prefixo de todos os endpoints deste controller.

**`@Tag`** — anotação do Swagger. Define o nome do grupo na documentação.

---

### Anotações de endpoints

```java
@PostMapping
public ResponseEntity<ProdutoDTO> criar(@RequestBody @Valid ProdutoDTO dto) { ... }

@GetMapping
public ResponseEntity<List<ProdutoDTO>> listarTodos() { ... }

@GetMapping("/{id}")
public ResponseEntity<ProdutoDTO> buscarPorId(@PathVariable Long id) { ... }

@PutMapping("/{id}")
public ResponseEntity<ProdutoDTO> atualizar(@PathVariable Long id,
                                            @RequestBody @Valid ProdutoDTO dto) { ... }

@PatchMapping("/{id}/vendido")
public ResponseEntity<AnuncioDTO> marcarComoVendido(@PathVariable Long id) { ... }

@DeleteMapping("/{id}")
public ResponseEntity<Void> deletar(@PathVariable Long id) { ... }
```

**`@PostMapping`** — mapeia requisições HTTP POST.
**`@GetMapping`** — mapeia HTTP GET.
**`@PutMapping`** — mapeia HTTP PUT (substitui o recurso inteiro).
**`@PatchMapping`** — mapeia HTTP PATCH (altera apenas parte do recurso — ideal para mudar status).
**`@DeleteMapping`** — mapeia HTTP DELETE.

**`@RequestBody`** — desserializa o JSON do corpo da requisição para o objeto Java.
**`@Valid`** — ativa as validações do DTO (`@NotNull`, `@NotBlank`, etc.). Sem isso, as anotações de validação são ignoradas.
**`@PathVariable`** — extrai um valor da URL. Em `GET /produtos/5`, o `id = 5`.

**`ResponseEntity`** — encapsula a resposta HTTP com status code, headers e body. Permite controle fino sobre o que é retornado:
```java
ResponseEntity.ok(dto)              // 200 OK com body
ResponseEntity.noContent().build()  // 204 No Content sem body
ResponseEntity.status(404).body()   // 404 com body customizado
```

---

## Fluxo completo de uma requisição

**Exemplo: `POST /anuncios` com token JWT**

```
1. Cliente envia:
   POST /anuncios
   Authorization: Bearer eyJhbGci...
   Body: {"usuarioId": 1, "produtoId": 1, "quantidade": 100, "preco": 50.00}

2. JwtFilter intercepta:
   - lê o header Authorization
   - extrai o token
   - chama jwtService.tokenValido(token) → true
   - extrai telefone do token
   - registra no SecurityContextHolder

3. SecurityConfig verifica:
   - /anuncios não está na lista pública
   - usuário está autenticado → deixa passar

4. AnuncioController.criar() recebe:
   - @RequestBody deserializa o JSON para AnuncioDTO
   - @Valid dispara as validações → passa
   - chama anuncioService.criar(dto)

5. AnuncioService.criar() executa:
   - @Transactional abre transação
   - busca Usuario pelo usuarioId → encontra
   - verifica se é VENDEDOR → é
   - busca Produto pelo produtoId → encontra
   - cria Anuncio com status ATIVO e createdAt preenchido
   - chama anuncioRepository.save(anuncio)
   - converte para AnuncioDTO via toDTO()
   - @Transactional faz COMMIT
   - retorna AnuncioDTO

6. AnuncioController recebe o DTO:
   - encapsula em ResponseEntity.ok(dto)
   - retorna 200 com o JSON

7. Cliente recebe:
   200 OK
   {"id": 1, "usuarioNome": "Maria", "produtoNome": "Soja", "status": "ATIVO", ...}
```

---

## Fluxo do Matching automático

**Exemplo: criação de Demanda que encontra uma OfertaFutura compatível**

```
1. Comprador cria Demanda via POST /demandas
   → DemandaService.criar() é chamado

2. DemandaService:
   - valida que é COMPRADOR
   - salva a Demanda com status PROCURANDO
   - chama matchingService.buscarMatchesPorDemanda(demanda)

3. MatchingService.buscarMatchesPorDemanda():
   - busca OfertasFuturas com:
     → mesmo produto
     → status ABERTA
     → dataDisponivel <= dataLimite da demanda
   - filtra: quantidade da oferta >= quantidade da demanda
   - filtra: não existe matching já criado para esse par
   - para cada oferta compatível: cria Matching com status PENDENTE

4. Matching criado automaticamente em banco

5. Vendedor consulta: GET /matchings/oferta/{id}
   → vê o matching PENDENTE

6. Vendedor aceita: PATCH /matchings/{id}/aceitar
   → matching vira ACEITO
   → oferta vira FECHADA
   → demanda vira ATENDIDA
   (tudo na mesma @Transactional)
```

---

## Regras de negócio por entidade

| Entidade | Regra | Onde está |
|---|---|---|
| `Anuncio` | Só VENDEDOR cria | `AnuncioService.criar()` |
| `Anuncio` | Status inicial = ATIVO | `AnuncioService.criar()` |
| `Pedido` | Só COMPRADOR faz pedido | `PedidoService.criar()` |
| `Pedido` | Anúncio deve estar ATIVO | `PedidoService.criar()` |
| `Pedido` | Não pode comprar do próprio anúncio | `PedidoService.criar()` |
| `Pedido` | Quantidade não pode superar o disponível | `PedidoService.criar()` |
| `Pedido` | Confirmar → anúncio vira VENDIDO | `PedidoService.confirmar()` |
| `Usuario` | Telefone único | `UsuarioService.criar()` + `@Column(unique)` |
| `OfertaFutura` | Só VENDEDOR cria | `OfertaFuturaService.criar()` |
| `Demanda` | Só COMPRADOR cria | `DemandaService.criar()` |
| `Matching` | Sem duplicatas | `MatchingRepository.existsByOfertaIdAndDemandaId()` |
| `Matching` | Aceitar → oferta FECHADA + demanda ATENDIDA | `MatchingService.aceitar()` |

---

## Tabelas e relacionamentos no banco

```
produtos
    id, nome, descricao

usuarios
    id, nome, telefone (unique), tipo (enum), created_at

anuncios
    id, usuario_id (FK → usuarios), produto_id (FK → produtos),
    quantidade, preco, status (enum), created_at

pedidos
    id, comprador_id (FK → usuarios), anuncio_id (FK → anuncios),
    quantidade, status (enum), created_at

ofertas_futuras
    id, usuario_id (FK → usuarios), produto_id (FK → produtos),
    quantidade, data_disponivel, status (enum), created_at

demandas
    id, comprador_id (FK → usuarios), produto_id (FK → produtos),
    quantidade, data_limite, status (enum), created_at

matchings
    id, oferta_id (FK → ofertas_futuras), demanda_id (FK → demandas),
    status (enum), created_at
```

---

## Endpoints disponíveis

### Autenticação — `/auth`
| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/auth/registrar` | Público | Cria usuário e retorna token |
| POST | `/auth/login` | Público | Autentica e retorna token |

### Produtos — `/produtos`
| Método | Rota | Acesso |
|---|---|---|
| POST | `/produtos` | Token |
| GET | `/produtos` | Público |
| GET | `/produtos/{id}` | Público |
| PUT | `/produtos/{id}` | Token |
| DELETE | `/produtos/{id}` | Token |

### Usuários — `/usuarios`
| Método | Rota | Acesso |
|---|---|---|
| POST | `/usuarios` | Token |
| GET | `/usuarios` | Token |
| GET | `/usuarios/{id}` | Token |
| GET | `/usuarios/tipo/{tipo}` | Token |
| PUT | `/usuarios/{id}` | Token |
| DELETE | `/usuarios/{id}` | Token |

### Anúncios — `/anuncios`
| Método | Rota | Acesso |
|---|---|---|
| POST | `/anuncios` | Token |
| GET | `/anuncios` | Público |
| GET | `/anuncios/{id}` | Público |
| GET | `/anuncios/usuario/{id}` | Token |
| PATCH | `/anuncios/{id}/vendido` | Token |
| DELETE | `/anuncios/{id}` | Token |

### Pedidos — `/pedidos`
| Método | Rota | Acesso |
|---|---|---|
| POST | `/pedidos` | Token |
| GET | `/pedidos/{id}` | Token |
| GET | `/pedidos/comprador/{id}` | Token |
| PATCH | `/pedidos/{id}/confirmar` | Token |
| PATCH | `/pedidos/{id}/finalizar` | Token |

### Mercado futuro — Ofertas
| Método | Rota | Acesso |
|---|---|---|
| POST | `/ofertas-futuras` | Token |
| GET | `/ofertas-futuras` | Público |
| GET | `/ofertas-futuras/usuario/{id}` | Token |

### Mercado futuro — Demandas
| Método | Rota | Acesso |
|---|---|---|
| POST | `/demandas` | Token |
| GET | `/demandas` | Público |
| GET | `/demandas/comprador/{id}` | Token |

### Mercado futuro — Matchings
| Método | Rota | Acesso |
|---|---|---|
| GET | `/matchings/oferta/{id}` | Token |
| GET | `/matchings/demanda/{id}` | Token |
| PATCH | `/matchings/{id}/aceitar` | Token |
| PATCH | `/matchings/{id}/recusar` | Token |

---

## Cobertura de testes

**157 testes, 0 falhas**

| Arquivo de teste | Testes | O que cobre |
|---|---|---|
| `JwtServiceTest` | 6 | Geração, validação e leitura de tokens |
| `CustomUserDetailsServiceTest` | 4 | Carregamento de usuário para o Spring Security |
| `ProdutoServiceTest` | 12 | CRUD, validações, exceções |
| `UsuarioServiceTest` | 11 | CRUD, telefone único, tipo |
| `AnuncioServiceTest` | 15 | CRUD, regras de vendedor, status |
| `PedidoServiceTest` | 14 | Ciclo de vida, regras cruzadas |
| `OfertaFuturaServiceTest` | 8 | Criação, matching automático |
| `DemandaServiceTest` | 7 | Criação, matching automático |
| `MatchingServiceTest` | 11 | Matching, aceitar, recusar, duplicatas |
| `AuthControllerTest` | 4 | Registro e login |
| `ProdutoControllerTest` | 8 | Endpoints HTTP |
| `UsuarioControllerTest` | 11 | Endpoints HTTP |
| `AnuncioControllerTest` | 11 | Endpoints HTTP |
| `PedidoControllerTest` | 11 | Endpoints HTTP |
| `OfertaFuturaControllerTest` | 6 | Endpoints HTTP |
| `DemandaControllerTest` | 6 | Endpoints HTTP |
| `MatchingControllerTest` | 7 | Endpoints HTTP |
| `GlobalExceptionHandlerTest` | 4 | 404, 400, 500 |
| `ConectaApplicationTests` | 1 | Contexto do Spring sobe sem erros |