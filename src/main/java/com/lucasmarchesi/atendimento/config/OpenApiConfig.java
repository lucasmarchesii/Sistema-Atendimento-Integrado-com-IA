package com.lucasmarchesi.atendimento.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI documentacaoDaApi() {
        return new OpenAPI().info(new Info()
                .title("Sistema de Atendimento com IA")
                .version("0.1.0")
                .description("""
                        Recebe mensagens de clientes, guarda cada uma e as classifica \
                        (categoria, prioridade, assunto e resposta sugerida). \
                        Se a classificação falhar, o atendimento fica PENDENTE_CLASSIFICACAO \
                        e é reprocessado depois, então nenhuma mensagem se perde."""));
    }
}