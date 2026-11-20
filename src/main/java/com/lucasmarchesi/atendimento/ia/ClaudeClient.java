package com.lucasmarchesi.atendimento.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class ClaudeClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public ClaudeClient(@Value("${anthropic.api-key}") String apiKey,
                        @Value("${anthropic.model}") String model) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .requestFactory(factory)
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @SuppressWarnings("null")
public String enviar(String system, String mensagemUsuario) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ClassificacaoException("Chave da API não configurada");
        }

        Map<String, Object> corpo = Map.of(
                "model", model,
                "max_tokens", 1024,
                "system", system,
                "messages", List.of(Map.of("role", "user", "content", mensagemUsuario))
        );

        MessagesResponse resposta = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .body(MessagesResponse.class);

        if (resposta == null || resposta.content() == null) {
            throw new ClassificacaoException("Resposta vazia da IA");
        }

        return resposta.content().stream()
                .filter(bloco -> "text".equals(bloco.type()))
                .map(ContentBlock::text)
                .findFirst()
                .orElseThrow(() -> new ClassificacaoException("A IA não devolveu texto"));
    }

    record MessagesResponse(List<ContentBlock> content) {
    }

    record ContentBlock(String type, String text) {
    }
}