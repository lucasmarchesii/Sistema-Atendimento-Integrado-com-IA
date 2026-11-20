package com.lucasmarchesi.atendimento.service;

import com.lucasmarchesi.atendimento.dto.AtendimentoRequest;
import com.lucasmarchesi.atendimento.model.Atendimento;
import com.lucasmarchesi.atendimento.repository.AtendimentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AtendimentoService {

    private final AtendimentoRepository repository;

    public AtendimentoService(AtendimentoRepository repository) {
        this.repository = repository;
    }

    public Atendimento registrar(AtendimentoRequest request) {
        Atendimento atendimento = new Atendimento(request.nomeCliente(), request.mensagem());
        return repository.save(atendimento);
    }

    public List<Atendimento> listar() {
        return repository.findAll();
    }
}