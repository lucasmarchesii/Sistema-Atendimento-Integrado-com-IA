package com.lucasmarchesi.atendimento;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() 
    {
        return "deu certo";
    }
    @GetMapping("/ola")
    public String second()
    {
        return "segunda tentativa";
    }
}
