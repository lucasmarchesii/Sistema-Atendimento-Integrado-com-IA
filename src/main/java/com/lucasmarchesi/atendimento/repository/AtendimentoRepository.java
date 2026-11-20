package com.lucasmarchesi.atendimento.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lucasmarchesi.atendimento.model.Atendimento;
import com.lucasmarchesi.atendimento.model.StatusAtendimento;
import java.util.List;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {
     List<Atendimento> findTop20ByStatusOrderByCriadoEmAsc(StatusAtendimento status);
}
