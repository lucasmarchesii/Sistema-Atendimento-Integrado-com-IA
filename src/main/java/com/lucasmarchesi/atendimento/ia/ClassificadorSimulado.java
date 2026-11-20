package com.lucasmarchesi.atendimento.ia;

import com.lucasmarchesi.atendimento.model.Categoria;
import com.lucasmarchesi.atendimento.model.Prioridade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
@ConditionalOnProperty(name = "classificador.provedor", havingValue = "simulado", matchIfMissing = true)
public class ClassificadorSimulado implements Classificador {

    @Override
    public ClassificacaoIA classificar(String mensagem) {
        String texto = normalizar(mensagem);
        Categoria categoria = definirCategoria(texto);
        Prioridade prioridade = definirPrioridade(texto);
        return new ClassificacaoIA(categoria, prioridade, assuntoPara(categoria), respostaPara(categoria));
    }

    private Categoria definirCategoria(String texto) {
        if (contem(texto, "cancel", "encerrar")) return Categoria.CANCELAMENTO;
        if (contem(texto, "cobr", "fatura", "pagamento", "estorno", "reembolso", "cartao", "boleto"))
            return Categoria.FINANCEIRO;
        if (contem(texto, "senha", "login", "entrar", "acessar", "acesso", "bloquead"))
            return Categoria.ACESSO;
        if (contem(texto, "erro", "bug", "travou", "nao funciona", "lento", "falha"))
            return Categoria.TECNICO;
        return Categoria.OUTROS;
    }

    private Prioridade definirPrioridade(String texto) {
        if (contem(texto, "urgente", "imediat", "fraude", "indevid", "duas vezes", "bloquead"))
            return Prioridade.ALTA;
        if (contem(texto, "duvida", "informacao", "sugestao"))
            return Prioridade.BAIXA;
        return Prioridade.MEDIA;
    }

    private String assuntoPara(Categoria categoria) {
        return switch (categoria) {
            case FINANCEIRO -> "Questão de cobrança ou pagamento";
            case ACESSO -> "Problema de acesso à conta";
            case CANCELAMENTO -> "Pedido de cancelamento";
            case TECNICO -> "Problema técnico";
            case OUTROS -> "Assunto geral";
        };
    }

    private String respostaPara(Categoria categoria) {
        return switch (categoria) {
            case FINANCEIRO -> "Olá! Vamos verificar a sua cobrança e retornamos em breve com a solução.";
            case ACESSO -> "Olá! Vamos ajudar você a recuperar o acesso à sua conta.";
            case CANCELAMENTO -> "Olá! Recebemos o seu pedido de cancelamento e vamos orientar os próximos passos.";
            case TECNICO -> "Olá! Lamentamos o problema. Nossa equipe técnica vai analisar o caso.";
            case OUTROS -> "Olá! Recebemos a sua mensagem e vamos responder em breve.";
        };
    }

    private boolean contem(String texto, String... termos) {
        for (String termo : termos) {
            if (texto.contains(termo)) return true;
        }
        return false;
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}