package com.lucasmarchesi.atendimento.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record AtendimentoRequest(@NotBlank(message = "O nome do cliente é obrigatório")
    String nomeCliente,

    @NotBlank(message = "A mensagem é obrigatória") 
    @Size(max = 2000, message = "A mensagem pode ter no máximo 2000 caracteres")
    String mensagem
) {
}
