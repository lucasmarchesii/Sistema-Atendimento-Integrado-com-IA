package com.lucasmarchesi.atendimento.ia;

import com.lucasmarchesi.atendimento.model.Categoria;
import com.lucasmarchesi.atendimento.model.Prioridade;

public record ClassificacaoIA(
        Categoria categoria,
        Prioridade prioridade,
        String assunto,
        String respostaSugerida
) {
}