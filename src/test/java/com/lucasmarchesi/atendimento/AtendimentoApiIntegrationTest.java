package com.lucasmarchesi.atendimento;

import com.lucasmarchesi.atendimento.repository.AtendimentoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"classificador.provedor=simulado", "reprocessamento.automatico=false"})
class AtendimentoApiIntegrationTest {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() {
            };

    @Value("${local.server.port}")
    private int porta;

    @Autowired
    private AtendimentoRepository repository;

    private final List<Long> idsCriados = new ArrayList<>();
    private RestClient http;

    @BeforeEach
    void preparar() {
        http = RestClient.create("http://localhost:" + porta);
    }

    @AfterEach
    void limpar() {
        repository.deleteAllById(idsCriados);
    }

    @Test
    void criaEClassificaUmAtendimento() {
        ResponseEntity<Map<String, Object>> resposta =
                criar("Fui cobrada duas vezes no cartão e preciso do estorno urgente");

        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
        assertThat(resposta.getBody())
                .containsEntry("status", "CLASSIFICADO")
                .containsEntry("categoria", "FINANCEIRO")
                .containsEntry("prioridade", "ALTA");
    }

    @Test
    void rejeitaMensagemVazia() {
        assertThatThrownBy(() -> http.post()
                .uri("/atendimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nomeCliente", "Teste", "mensagem", ""))
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    void listaOsAtendimentosCriados() {
        Long id = ((Number) criar("Não consigo acessar minha conta").getBody().get("id")).longValue();

        List<Map<String, Object>> lista = http.get().uri("/atendimentos").retrieve().body(LISTA);

        assertThat(lista).anySatisfy(item ->
                assertThat(((Number) item.get("id")).longValue()).isEqualTo(id));
    }

    @Test
    void publicaADocumentacaoOpenApi() {
        String json = http.get().uri("/v3/api-docs").retrieve().body(String.class);

        assertThat(json)
                .contains("\"/atendimentos\"")
                .contains("/atendimentos/reprocessar");
    }

    private ResponseEntity<Map<String, Object>> criar(String mensagem) {
        ResponseEntity<Map<String, Object>> resposta = http.post()
                .uri("/atendimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nomeCliente", "Teste", "mensagem", mensagem))
                .retrieve()
                .toEntity(MAPA);
        idsCriados.add(((Number) resposta.getBody().get("id")).longValue());
        return resposta;
    }
}