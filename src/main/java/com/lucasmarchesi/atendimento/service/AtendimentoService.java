package com.lucasmarchesi.atendimento.service;

import com.lucasmarchesi.atendimento.dto.ResumoReprocessamento;
import com.lucasmarchesi.atendimento.model.StatusAtendimento;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final AtomicBoolean reprocessando = new AtomicBoolean(false);

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

        public ResumoReprocessamento reprocessarPendentes() {
        if (!reprocessando.compareAndSet(false, true)) {
            log.info("Reprocessamento já em andamento; ignorando nova execução");
            return new ResumoReprocessamento(0, 0, 0);
        }

        try {
            List<Atendimento> pendentes = repository
                    .findTop20ByStatusOrderByCriadoEmAsc(StatusAtendimento.PENDENTE_CLASSIFICACAO);

            int classificados = 0;
            int falhas = 0;

            for (Atendimento atendimento : pendentes) {
                try {
                    ClassificacaoIA c = classificador.classificar(atendimento.getMensagem());
                    atendimento.aplicarClassificacao(
                            c.categoria(), c.prioridade(), c.assunto(), c.respostaSugerida());
                    repository.save(atendimento);
                    classificados++;
                } catch (ClassificacaoException e) {
                    falhas++;
                    log.warn("Atendimento {} continua pendente: {}", atendimento.getId(), e.getMessage());
                }
            }

            return new ResumoReprocessamento(pendentes.size(), classificados, falhas);
        } finally {
            reprocessando.set(false);
        }
    }

}