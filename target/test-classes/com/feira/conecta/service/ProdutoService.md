=======================Documentação de Testes: ProdutoService======================

Esta documentação descreve os testes unitários aplicados à camada de serviço (ProdutoService) do projeto Conecta. O foco destes testes é validar as regras de negócio e a integração com o repositório de dados.

Estrutura Técnica
Framework: JUnit 5.
Biblioteca de Mocking: Mockito.
Biblioteca de Asserções: AssertJ.
Isolamento: A classe utiliza @ExtendWith(MockitoExtension.class) para simular o comportamento do ProdutoRepository sem necessidade de conexão com banco de dados real.
Componentes do Teste
Componente
Tipo
Descrição
ProdutoRepository
@Mock
Dublê de teste que simula as operações de banco de dados.
ProdutoService
@InjectMocks
Classe alvo do teste onde as dependências mockadas são injetadas.
setup()
@BeforeEach
Método executado antes de cada teste para instanciar objetos base (Produto e ProdutoDTO).


Cenários de Sucesso
1. Criar Produto
Método: deveCriarProdutoComSucesso
Descrição: Valida se o serviço converte o DTO para entidade, salva e retorna o objeto com ID.
Verificações: Confirma se o ID gerado é 1 e se o nome corresponde ao enviado.
2. Listagem de Produtos
Métodos: deveListarTodosOsProdutos e deveListarMultiplosProdutos
Descrição: Garante que o serviço retorna a lista de produtos corretamente, independentemente da quantidade de itens.
Verificações: Valida o tamanho da lista e se os nomes contidos (ex: "Soja", "Milho") estão corretos.
3. Busca por ID
Método: deveBuscarProdutoPorIdComSucesso
Descrição: Valida o retorno de um produto quando um ID válido é fornecido.
Verificações: Confirma se o nome do produto retornado é o esperado.
4. Exclusão de Produto
Método: deveDeletarProdutoComSucesso
Descrição: Garante que a chamada para exclusão não gera erros quando o ID é válido.

Cenários de Validação e Regras de Negócio
1. Integridade de Dados (Descrição Nula)
Método: deveCriarProdutoSemDescricao
Descrição: Verifica se o sistema se comporta corretamente ao salvar um produto sem o campo opcional de descrição.
2. Garantia de Operação Única
Método: deveSalvarProdutoUmaUnicaVez
Descrição: Utiliza verify e verifyNoMoreInteractions para assegurar que o banco de dados não seja acionado mais de uma vez na mesma operação de criação, otimizando performance e evitando duplicidade.

Cenários de Exceção
1. Produto Inexistente na Busca
Método: deveLancarExcecaoQuandoProdutoNaoEncontrado
Descrição: Valida se o sistema interrompe o fluxo e lança uma RuntimeException ao buscar por um ID que não consta no repositório.
2. Produto Inexistente na Exclusão
Método: deveLancarExcecaoAoDeletarProdutoInexistente
Descrição: Garante que o sistema reporta o erro corretamente caso o usuário tente deletar um registro já inexistente.

Resumo de Comandos de Verificação
when(...).thenReturn(...): Configura o comportamento esperado do banco de dados.
assertThat(...): Realiza a comparação entre o resultado obtido e o esperado.
verify(..., times(1)): Assegura que a persistência foi chamada exatamente uma vez.
doThrow(...).when(...): Simula falhas de infraestrutura ou de busca no repositório.
