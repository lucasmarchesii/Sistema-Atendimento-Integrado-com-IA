package com.lucasmarchesi.atendimento.service;

import com.lucasmarchesi.atendimento.dto.AtendimentoRequest;
import com.lucasmarchesi.atendimento.dto.ResumoReprocessamento;
import com.lucasmarchesi.atendimento.ia.ClassificacaoException;
import com.lucasmarchesi.atendimento.ia.ClassificacaoIA;
import com.lucasmarchesi.atendimento.ia.Classificador;
import com.lucasmarchesi.atendimento.model.Atendimento;
import com.lucasmarchesi.atendimento.model.Categoria;
import com.lucasmarchesi.atendimento.model.Prioridade;
import com.lucasmarchesi.atendimento.model.StatusAtendimento;
import com.lucasmarchesi.atendimento.repository.AtendimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtendimentoServiceTest {

    private static final ClassificacaoIA CLASSIFICACAO = new ClassificacaoIA(
            Categoria.FINANCEIRO, Prioridade.ALTA, "Cobrança duplicada", "Vamos verificar.");

    @Mock
    private AtendimentoRepository repository;

    @Mock
    private Classificador classificador;

    private AtendimentoService service;

    @BeforeEach
    void preparar() {
        service = new AtendimentoService(repository, classificador);
    }

    @Test
    void registraEClassificaComSucesso() {
        when(repository.save(any(Atendimento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(classificador.classificar(anyString())).thenReturn(CLASSIFICACAO);

        Atendimento r = service.registrar(new AtendimentoRequest("Maria", "Fui cobrada duas vezes"));

        assertThat(r.getStatus()).isEqualTo(StatusAtendimento.CLASSIFICADO);
        assertThat(r.getCategoria()).isEqualTo(Categoria.FINANCEIRO);
        assertThat(r.getRespostaSugerida()).isEqualTo("Vamos verificar.");
    }

    @Test
    void guardaAMensagemAntesDeChamarAIA() {
        when(repository.save(any(Atendimento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(classificador.classificar(anyString())).thenReturn(CLASSIFICACAO);

        service.registrar(new AtendimentoRequest("Maria", "Fui cobrada duas vezes"));

        InOrder ordem = inOrder(repository, classificador);
        ordem.verify(repository).save(any(Atendimento.class));
        ordem.verify(classificador).classificar("Fui cobrada duas vezes");
        ordem.verify(repository).save(any(Atendimento.class));
    }

    @Test
    void naoPerdeAMensagemQuandoAIAFalha() {
        when(repository.save(any(Atendimento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(classificador.classificar(anyString())).thenThrow(new ClassificacaoException("IA fora do ar"));

        Atendimento r = service.registrar(new AtendimentoRequest("Maria", "Fui cobrada duas vezes"));

        assertThat(r.getStatus()).isEqualTo(StatusAtendimento.PENDENTE_CLASSIFICACAO);
        assertThat(r.getCategoria()).isNull();
        verify(repository, times(2)).save(any(Atendimento.class));
    }

    @Test
    void reprocessaPendentesEContaSucessosEFalhas() {
        Atendimento ok = pendente("Maria", "mensagem ok");
        Atendimento falha = pendente("Joao", "mensagem problema");
        when(repository.findTop20ByStatusOrderByCriadoEmAsc(StatusAtendimento.PENDENTE_CLASSIFICACAO))
                .thenReturn(List.of(ok, falha));
        when(classificador.classificar("mensagem ok")).thenReturn(CLASSIFICACAO);
        when(classificador.classificar("mensagem problema"))
                .thenThrow(new ClassificacaoException("falhou"));

        ResumoReprocessamento resumo = service.reprocessarPendentes();

        assertThat(resumo).isEqualTo(new ResumoReprocessamento(2, 1, 1));
        assertThat(ok.getStatus()).isEqualTo(StatusAtendimento.CLASSIFICADO);
        assertThat(falha.getStatus()).isEqualTo(StatusAtendimento.PENDENTE_CLASSIFICACAO);
        verify(repository).save(ok);
        verify(repository, never()).save(falha);
    }

    @Test
    void reprocessamentoSemPendentesNaoFazNada() {
        when(repository.findTop20ByStatusOrderByCriadoEmAsc(StatusAtendimento.PENDENTE_CLASSIFICACAO))
                .thenReturn(List.of());

        assertThat(service.reprocessarPendentes()).isEqualTo(new ResumoReprocessamento(0, 0, 0));
        verifyNoInteractions(classificador);
    }

    private Atendimento pendente(String nome, String mensagem) {
        Atendimento a = new Atendimento(nome, mensagem);
        a.marcarPendenteClassificacao();
        return a;
    }
}