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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.feira.conecta.domain.Produto;
import com.feira.conecta.dto.ProdutoDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository repository;

    @InjectMocks
    private ProdutoService service;

    private Produto produto;
    private ProdutoDTO dto;

    @BeforeEach
    void setup() {
        produto = Produto.builder()
                .id(1L)
                .nome("Soja")
                .descricao("Soja grão safra 2025")
                .build();

        dto = ProdutoDTO.builder()
                .nome("Soja")
                .descricao("Soja grão safra 2025")
                .build();
    }

    // ========================
    // CENÁRIOS FELIZES
    // ========================

    @Test
    void deveCriarProdutoComSucesso() {
        when(repository.save(any(Produto.class))).thenReturn(produto);

        ProdutoDTO resultado = service.criar(dto);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNome()).isEqualTo("Soja");
        verify(repository, times(1)).save(any(Produto.class));
    }

    @Test
    void deveListarTodosOsProdutos() {
        when(repository.findAll()).thenReturn(List.of(produto));

        List<ProdutoDTO> resultado = service.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNome()).isEqualTo("Soja");
    }

    @Test
    void deveBuscarProdutoPorIdComSucesso() {
        when(repository.findById(1L)).thenReturn(Optional.of(produto));

        ProdutoDTO resultado = service.buscarPorId(1L);

        assertThat(resultado.getNome()).isEqualTo("Soja");
    }

    @Test
    void deveDeletarProdutoComSucesso() {
        when(repository.existsById(1L)).thenReturn(true); // ← adicionado
        doNothing().when(repository).deleteById(1L);
    
        assertThatCode(() -> service.deletar(1L)).doesNotThrowAnyException();
        verify(repository, times(1)).deleteById(1L);
    }

    // ========================
    // CENÁRIOS INFELIZES
    // ========================
    @Test
    void deveLancarExcecaoQuandoProdutoNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
    
        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado com id: 99"); // ← atualizado
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverProdutos() {
        when(repository.findAll()).thenReturn(List.of());

        List<ProdutoDTO> resultado = service.listarTodos();

        assertThat(resultado).isEmpty();
    }


    // ========================
// CENÁRIOS DE VALIDAÇÃO
// ========================

@Test
void deveCriarProdutoSemDescricao() {
    ProdutoDTO dtoSemDescricao = ProdutoDTO.builder()
            .nome("Feijão")
            .build();

    Produto produtoSalvo = Produto.builder()
            .id(2L)
            .nome("Feijão")
            .build();

    when(repository.save(any(Produto.class))).thenReturn(produtoSalvo);

    ProdutoDTO resultado = service.criar(dtoSemDescricao);

    assertThat(resultado.getId()).isEqualTo(2L);
    assertThat(resultado.getDescricao()).isNull();
}

@Test
void deveSalvarProdutoUmaUnicaVez() {
    when(repository.save(any(Produto.class))).thenReturn(produto);

    service.criar(dto);

    verify(repository, times(1)).save(any(Produto.class));
    verifyNoMoreInteractions(repository);
}

@Test
void deveListarMultiplosProdutos() {
    Produto outro = Produto.builder()
            .id(2L).nome("Milho").descricao("Milho verde").build();

    when(repository.findAll()).thenReturn(List.of(produto, outro));

    List<ProdutoDTO> resultado = service.listarTodos();

    assertThat(resultado).hasSize(2);
    assertThat(resultado).extracting("nome")
            .containsExactlyInAnyOrder("Soja", "Milho");
}

@Test
void deveLancarExcecaoAoDeletarProdutoInexistente() {
    when(repository.existsById(99L)).thenReturn(false);

    assertThatThrownBy(() -> service.deletar(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Produto não encontrado com id: 99"); // ← atualizado
}
@Test
void deveAtualizarProdutoComSucesso() {
    ProdutoDTO dtoAtualizado = ProdutoDTO.builder()
            .nome("Soja Premium")
            .descricao("Safra 2026")
            .build();

    Produto produtoAtualizado = Produto.builder()
            .id(1L).nome("Soja Premium").descricao("Safra 2026").build();

    when(repository.findById(1L)).thenReturn(Optional.of(produto));
    when(repository.save(any(Produto.class))).thenReturn(produtoAtualizado);

    ProdutoDTO resultado = service.atualizar(1L, dtoAtualizado);

    assertThat(resultado.getNome()).isEqualTo("Soja Premium");
    assertThat(resultado.getDescricao()).isEqualTo("Safra 2026");
}

@Test
void deveLancarExcecaoAoAtualizarProdutoInexistente() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.atualizar(99L, dto))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Produto não encontrado com id: 99");
}

    
}