package com.lucasmarchesi.atendimento.ia;

public class ClassificacaoException extends RuntimeException {

    public ClassificacaoException(String mensagem) {
        super(mensagem);
    }

    public ClassificacaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}