package com.lucasmarchesi.atendimento.controller;

import com.lucasmarchesi.atendimento.dto.AtendimentoRequest;
import com.lucasmarchesi.atendimento.dto.ResumoReprocessamento;
import com.lucasmarchesi.atendimento.model.Atendimento;
import com.lucasmarchesi.atendimento.service.AtendimentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Atendimentos", description = "Registro, consulta e reprocessamento de atendimentos")
@RestController
@RequestMapping("/atendimentos")
public class AtendimentoController {

    private final AtendimentoService service;

    public AtendimentoController(AtendimentoService service) {
        this.service = service;
    }

    @Operation(
            summary = "Registra um atendimento",
            description = "Guarda a mensagem e tenta classificá-la. Se a classificação falhar, "
                    + "o atendimento é guardado como PENDENTE_CLASSIFICACAO.")
    @ApiResponse(responseCode = "201", description = "Atendimento registrado (classificado ou pendente)")
    @ApiResponse(responseCode = "400", description = "Dados inválidos (nome ou mensagem em branco, mensagem acima de 2000 caracteres)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Atendimento criar(@Valid @RequestBody AtendimentoRequest request) {
        return service.registrar(request);
    }

    @Operation(summary = "Lista os atendimentos")
    @GetMapping
    public List<Atendimento> listar() {
        return service.listar();
    }

    @Operation(
            summary = "Reprocessa atendimentos pendentes",
            description = "Tenta classificar de novo até 20 atendimentos PENDENTE_CLASSIFICACAO, "
                    + "do mais antigo ao mais novo. Pode ser chamado várias vezes: quem já foi "
                    + "classificado não é processado de novo.")
    @PostMapping("/reprocessar")
    public ResumoReprocessamento reprocessar() {
        return service.reprocessarPendentes();
    }
}