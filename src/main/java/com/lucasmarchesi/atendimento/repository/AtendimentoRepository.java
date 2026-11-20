package com.lucasmarchesi.atendimento.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lucasmarchesi.atendimento.model.Atendimento;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {
    
}
