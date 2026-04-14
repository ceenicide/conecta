package com.feira.conecta.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.feira.conecta.config.SecurityUtils;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.ProdutoRequest;
import com.feira.conecta.dto.ProdutoResponse;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock private ProdutoRepository repository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private ProdutoService service;

    private Usuario vendedor;
    private Usuario comprador;
    private Usuario outroVendedor;
    private Produto produto;
    private ProdutoRequest request;

    @BeforeEach
    void setup() {
        vendedor = Usuario.builder()
                .id(1L).nome("Maria").telefone("(11) 999999999")
                .senha("hash").tipo(TipoUsuario.VENDEDOR).build();

        comprador = Usuario.builder()
                .id(2L).nome("Carlos").telefone("(11) 888888888")
                .senha("hash").tipo(TipoUsuario.COMPRADOR).build();

        outroVendedor = Usuario.builder()
                .id(3L).nome("Pedro").telefone("(11) 777777777")
                .senha("hash").tipo(TipoUsuario.VENDEDOR).build();

        produto = Produto.builder()
                .id(1L).nome("Soja").descricao("Safra 2025")
                .usuario(vendedor).build();

        // Request: só nome e descrição — sem id ou usuarioId
        request = new ProdutoRequest("Soja", "Safra 2025");
    }

    @Test
    void deveCriarProdutoComSucesso() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(repository.save(any(Produto.class))).thenReturn(produto);

        ProdutoResponse resultado = service.criar(request);

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("Soja");
        assertThat(resultado.usuarioId()).isEqualTo(1L);
        assertThat(resultado.usuarioNome()).isEqualTo("Maria");
        verify(repository, times(1)).save(any(Produto.class));
    }

    @Test
    void deveCriarProdutoSemDescricao() {
        ProdutoRequest semDesc = new ProdutoRequest("Feijão", null);
        Produto salvo = Produto.builder().id(2L).nome("Feijão").usuario(vendedor).build();

        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(repository.save(any(Produto.class))).thenReturn(salvo);

        ProdutoResponse resultado = service.criar(semDesc);

        assertThat(resultado.id()).isEqualTo(2L);
        assertThat(resultado.descricao()).isNull();
    }

    @Test
    void deveSalvarProdutoUmaUnicaVez() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(repository.save(any(Produto.class))).thenReturn(produto);

        service.criar(request);

        verify(repository, times(1)).save(any(Produto.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void deveLancarExcecaoQuandoCompradorTentaCriarProduto() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas vendedores podem cadastrar produtos");

        verify(repository, never()).save(any());
    }

    @Test
    void deveListarTodosOsProdutos() {
        when(repository.findAll()).thenReturn(List.of(produto));

        List<ProdutoResponse> resultado = service.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nome()).isEqualTo("Soja");
    }

    @Test
    void deveListarMultiplosProdutos() {
        Produto outro = Produto.builder()
                .id(2L).nome("Milho").descricao("Milho verde").usuario(vendedor).build();

        when(repository.findAll()).thenReturn(List.of(produto, outro));

        List<ProdutoResponse> resultado = service.listarTodos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(ProdutoResponse::nome)
                .containsExactlyInAnyOrder("Soja", "Milho");
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverProdutos() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listarTodos()).isEmpty();
    }

    @Test
    void deveBuscarProdutoPorIdComSucesso() {
        when(repository.findById(1L)).thenReturn(Optional.of(produto));

        ProdutoResponse resultado = service.buscarPorId(1L);

        assertThat(resultado.nome()).isEqualTo("Soja");
        assertThat(resultado.usuarioNome()).isEqualTo("Maria");
    }

    @Test
    void deveLancarExcecaoQuandoProdutoNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado com id: 99");
    }

    @Test
    void deveAtualizarProdutoComSucesso() {
        ProdutoRequest atualizado = new ProdutoRequest("Soja Premium", "Safra 2026");
        Produto salvo = Produto.builder()
                .id(1L).nome("Soja Premium").descricao("Safra 2026").usuario(vendedor).build();

        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(repository.findById(1L)).thenReturn(Optional.of(produto));
        when(repository.save(any(Produto.class))).thenReturn(salvo);

        ProdutoResponse resultado = service.atualizar(1L, atualizado);

        assertThat(resultado.nome()).isEqualTo("Soja Premium");
        assertThat(resultado.descricao()).isEqualTo("Safra 2026");
    }

    @Test
    void deveLancarExcecaoAoAtualizarProdutoInexistente() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado com id: 99");
    }

    @Test
    void deveLancarExcecaoQuandoOutroVendedorTentaEditar() {
        when(securityUtils.getUsuarioLogado()).thenReturn(outroVendedor);
        when(repository.findById(1L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> service.atualizar(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para editar este produto");

        verify(repository, never()).save(any());
    }

    @Test
    void deveDeletarProdutoComSucesso() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(repository.findById(1L)).thenReturn(Optional.of(produto));

        assertThatCode(() -> service.deletar(1L)).doesNotThrowAnyException();
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void deveLancarExcecaoAoDeletarProdutoInexistente() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado com id: 99");
    }

    @Test
    void deveLancarExcecaoQuandoOutroVendedorTentaDeletar() {
        when(securityUtils.getUsuarioLogado()).thenReturn(outroVendedor);
        when(repository.findById(1L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> service.deletar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para deletar este produto");

        verify(repository, never()).deleteById(any());
    }

    @Test
    void deveLancarExcecaoQuandoCompradorTentaDeletar() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(repository.findById(1L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> service.deletar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para deletar este produto");
    }

    @Test
    void deveListarProdutosLegadosSemUsuarioSemErro() {
        Produto legado = Produto.builder()
                .id(99L).nome("Milho Velho").descricao("Pre-migração")
                .usuario(null).build();

        when(repository.findAll()).thenReturn(List.of(legado));

        List<ProdutoResponse> resultado = service.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).usuarioId()).isNull();
        assertThat(resultado.get(0).usuarioNome()).isNull();
    }
}