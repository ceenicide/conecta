
***********Documentação de Testes: ProdutoController***********

Esta documentação detalha a suíte de testes unitários desenvolvida para a camada de controle (Controller) do projeto Conecta. O foco aqui é garantir que os pontos de entrada (endpoints) da API respondam corretamente às requisições externas.
 --------------Tecnologias Utilizadas--------------------
JUnit 5: Framework principal para execução dos testes.
Mockito: Utilizado para criar "mocks" (objetos simulados) do ProdutoService.
AssertJ: Biblioteca para asserções (verificações) mais legíveis e fluídas.
Spring Web: Para manipulação dos objetos ResponseEntity.
 Estrutura da Classe de Teste
A classe utiliza a anotação @ExtendWith(MockitoExtension.class), o que permite o uso de Mockito sem a necessidade de carregar todo o contexto do Spring, tornando os testes extremamente rápidos.
Componente
Anotação
Descrição
ProdutoService
@Mock
Simula o comportamento da camada de serviço.
ProdutoController
@InjectMocks
Instancia o controller e injeta o mock do serviço nele.


 ----------------Cenários de Sucesso (Happy Paths)-------------------
1. Criar Produto (POST)
Método: deveCriarProdutoERetornar200
Objetivo: Verificar se ao enviar um produto válido, o sistema retorna o objeto salvo.
Validações:
O código de status HTTP deve ser 200 OK.
O corpo da resposta deve conter o nome "Soja".
2. Listar Todos os Produtos (GET)
Método: deveListarProdutosERetornar200
Objetivo: Garantir que a listagem de produtos está funcionando e retornando uma coleção.
Validações:
O status code deve ser 200 OK.
A lista deve conter 1 item (conforme configurado no mock).
O primeiro item da lista deve se chamar "Milho".
3. Buscar Produto por ID (GET /{id})
Método: deveBuscarProdutoPorIdERetornar200
Objetivo: Validar a recuperação de um único registro específico através de seu identificador único.
Validações:
O status code deve ser 200 OK.
O nome do produto retornado deve ser "Arroz".
4. Deletar Produto (DELETE /{id})
Método: deveDeletarProdutoERetornar204
Objetivo: Confirmar que a exclusão de um produto processa corretamente e não retorna conteúdo.
Validações:
O status code deve ser 204 No Content.
Verificação de Chamada: Garante que o método service.deletar(1L) foi acionado exatamente 1 vez.

 --------------Cenários de Exceção (Edge Cases)--------------------
1. Produto Não Encontrado
Método: deveLancarExcecaoQuandoProdutoNaoEncontrado
Cenário: Busca por um ID inexistente (ex: ID 99).
Comportamento Esperado:
O mock do serviço é configurado para lançar uma RuntimeException.
O teste utiliza assertThatThrownBy para capturar essa exceção.
Valida se a mensagem de erro é exatamente: "Produto não encontrado".

 Glossário de Comandos Utilizados
when(...).thenReturn(...): Define o comportamento do mock (Entrada -> Saída).
assertThat(...).isEqualTo(...): Verifica se o resultado real é igual ao esperado.
verify(..., times(1)): Checa se uma função específica foi chamada a quantidade certa de vezes.
ResponseEntity: Objeto do Spring que encapsula toda a resposta HTTP (Status, Headers e Body).

