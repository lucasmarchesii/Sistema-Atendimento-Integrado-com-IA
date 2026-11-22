package com.lucasmarchesi.atendimento.ia;

import com.lucasmarchesi.atendimento.model.Categoria;
import com.lucasmarchesi.atendimento.model.Prioridade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClassificadorIATest {

    private static final String JSON_VALIDO = """
            {"categoria":"FINANCEIRO","prioridade":"ALTA","assunto":"Cobrança duplicada","respostaSugerida":"Vamos verificar."}""";

    private ClaudeClient claude;
    private ClassificadorIA classificador;

    @BeforeEach
    void preparar() {
        claude = mock(ClaudeClient.class);
        classificador = new ClassificadorIA(claude, JsonMapper.builder().build());
    }

    @Test
    void interpretaJsonValido() {
        ClassificacaoIA r = classificador.interpretar(JSON_VALIDO);

        assertThat(r.categoria()).isEqualTo(Categoria.FINANCEIRO);
        assertThat(r.prioridade()).isEqualTo(Prioridade.ALTA);
        assertThat(r.assunto()).isEqualTo("Cobrança duplicada");
    }

    @Test
    void toleraTextoEBlocoDeCodigoAoRedorDoJson() {
        String resposta = "Claro! Aqui está:\n```json\n" + JSON_VALIDO + "\n```";

        assertThat(classificador.interpretar(resposta).categoria()).isEqualTo(Categoria.FINANCEIRO);
    }

    @Test
    void rejeitaCategoriaInventada() {
        String json = JSON_VALIDO.replace("FINANCEIRO", "COBRANCA");

        assertThatThrownBy(() -> classificador.interpretar(json))
                .isInstanceOf(ClassificacaoException.class);
    }

    @Test
    void rejeitaPrioridadeAusente() {
        String json = "{\"categoria\":\"ACESSO\",\"assunto\":\"x\",\"respostaSugerida\":\"y\"}";

        assertThatThrownBy(() -> classificador.interpretar(json))
                .isInstanceOf(ClassificacaoException.class);
    }

    @Test
    void rejeitaAssuntoVazio() {
        String json = JSON_VALIDO.replace("Cobrança duplicada", "");

        assertThatThrownBy(() -> classificador.interpretar(json))
                .isInstanceOf(ClassificacaoException.class);
    }

    @Test
    void rejeitaRespostaSugeridaGrandeDemais() {
        String json = JSON_VALIDO.replace("Vamos verificar.", "x".repeat(2001));

        assertThatThrownBy(() -> classificador.interpretar(json))
                .isInstanceOf(ClassificacaoException.class);
    }

    @Test
    void rejeitaTextoSemJson() {
        assertThatThrownBy(() -> classificador.interpretar("Não sei classificar isso."))
                .isInstanceOf(ClassificacaoException.class);
    }

    @Test
    void convertesFalhaDeRedeEmClassificacaoException() {
        when(claude.enviar(any(), any())).thenThrow(new RestClientException("sem rede"));

        assertThatThrownBy(() -> classificador.classificar("oi"))
                .isInstanceOf(ClassificacaoException.class);
    }
}