package com.lucasmarchesi.atendimento.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record AtendimentoRequest(
        @Schema(description = "Nome do cliente", example = "Maria Souza")
        @NotBlank(message = "O nome do cliente é obrigatório")
        String nomeCliente,

        @Schema(description = "Mensagem enviada pelo cliente",
                example = "Fui cobrada duas vezes no cartão este mês e preciso do estorno urgente")
        @NotBlank(message = "A mensagem é obrigatória")
        @Size(max = 2000, message = "A mensagem pode ter no máximo 2000 caracteres")
        String mensagem
) {
}