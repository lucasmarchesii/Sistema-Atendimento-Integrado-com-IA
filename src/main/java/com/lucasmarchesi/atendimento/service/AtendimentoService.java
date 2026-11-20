package com.lucasmarchesi.atendimento.service;

import com.lucasmarchesi.atendimento.dto.AtendimentoRequest;
import com.lucasmarchesi.atendimento.ia.ClassificacaoException;
import com.lucasmarchesi.atendimento.ia.ClassificacaoIA;
import com.lucasmarchesi.atendimento.ia.Classificador;
import com.lucasmarchesi.atendimento.model.Atendimento;
import com.lucasmarchesi.atendimento.repository.AtendimentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AtendimentoService {

    private static final Logger log = LoggerFactory.getLogger(AtendimentoService.class);

    private final AtendimentoRepository repository;
    private final Classificador classificador;

    public AtendimentoService(AtendimentoRepository repository, Classificador classificador) {
        this.repository = repository;
        this.classificador = classificador;
    }

    public Atendimento registrar(AtendimentoRequest request) {
        Atendimento atendimento = repository.save(
                new Atendimento(request.nomeCliente(), request.mensagem()));

        try {
            ClassificacaoIA c = classificador.classificar(atendimento.getMensagem());
            atendimento.aplicarClassificacao(
                    c.categoria(), c.prioridade(), c.assunto(), c.respostaSugerida());
        } catch (ClassificacaoException e) {
            log.warn("Atendimento {} ficou pendente de classificação: {}",
                    atendimento.getId(), e.getMessage());
            atendimento.marcarPendenteClassificacao();
        }

        return repository.save(atendimento);
    }

    public List<Atendimento> listar() {
        return repository.findAll();
    }
}