package com.lucasmarchesi.atendimento.service;

import com.lucasmarchesi.atendimento.dto.ResumoReprocessamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "reprocessamento.automatico", havingValue = "true")
public class ReprocessamentoAgendado {

    private static final Logger log = LoggerFactory.getLogger(ReprocessamentoAgendado.class);

    private final AtendimentoService service;

    public ReprocessamentoAgendado(AtendimentoService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${reprocessamento.intervalo-ms:60000}")
    public void executar() {
        ResumoReprocessamento resumo = service.reprocessarPendentes();
        if (resumo.total() > 0) {
            log.info("Reprocessamento automático: {}", resumo);
        }
    }
}