

package com.lucasmarchesi.atendimento.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "atendimentos")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomeCliente;

    @Column(nullable = false, length = 2000)
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAtendimento status;

    @Column(nullable = false)
    private Instant criadoEm;

    protected Atendimento() {
    }

    public Atendimento(String nomeCliente, String mensagem) {
        this.nomeCliente = nomeCliente;
        this.mensagem = mensagem;
        this.status = StatusAtendimento.RECEBIDO;
        this.criadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public String getNomeCliente() { return nomeCliente; }
    public String getMensagem() { return mensagem; }
    public StatusAtendimento getStatus() { return status; }
    public Instant getCriadoEm() { return criadoEm; }
}
