package com.lucasmarchesi.atendimento.ia;

import com.lucasmarchesi.atendimento.model.Categoria;
import com.lucasmarchesi.atendimento.model.Prioridade;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;



class AvaliacaoClassificadorTest {

    

    // Pisos de qualidade do conjunto de desenvolvimento (um pouco abaixo da 1ª medição).
    private static final double MINIMO_CATEGORIA = 0.80;
    private static final double MINIMO_PRIORIDADE = 0.80;

    record Exemplo(Categoria categoria, Prioridade prioridade, String mensagem) {
    }

    record Resultado(double categoria, double prioridade) {
    }

    @Test
    void avaliaConjuntoDeDesenvolvimento() throws IOException {
        Resultado r = avaliar("DESENVOLVIMENTO", "/avaliacao/mensagens.txt", true);
        assertThat(r.categoria()).isGreaterThanOrEqualTo(MINIMO_CATEGORIA);
        assertThat(r.prioridade()).isGreaterThanOrEqualTo(MINIMO_PRIORIDADE);
    }

    @Test
    void avaliaConjuntoCego() throws IOException {
        // Só mede e relata. Não use este conjunto para escolher palavras-chave.
        avaliar("TESTE CEGO", "/avaliacao/mensagens-teste.txt", false);    }

        private Resultado avaliar(String titulo, String caminho, boolean mostrarErros) throws IOException {
        Classificador classificador = new ClassificadorSimulado();
        List<Exemplo> exemplos = carregar(caminho);
        assertThat(exemplos).isNotEmpty();

        int total = exemplos.size();
        int acertosCategoria = 0;
        int acertosPrioridade = 0;
        List<String> errosCategoria = new ArrayList<>();
        List<String> errosPrioridade = new ArrayList<>();

        Map<Categoria, int[]> matriz = new EnumMap<>(Categoria.class);
        for (Categoria c : Categoria.values()) {
            matriz.put(c, new int[Categoria.values().length]);
        }

        for (Exemplo ex : exemplos) {
            ClassificacaoIA r = classificador.classificar(ex.mensagem());

            matriz.get(ex.categoria())[r.categoria().ordinal()]++;

            if (r.categoria() == ex.categoria()) {
                acertosCategoria++;
            } else {
                errosCategoria.add(String.format("esperado %s, obteve %s: %s",
                        ex.categoria(), r.categoria(), ex.mensagem()));
            }

            if (r.prioridade() == ex.prioridade()) {
                acertosPrioridade++;
            } else {
                errosPrioridade.add(String.format("esperado %s, obteve %s: %s",
                        ex.prioridade(), r.prioridade(), ex.mensagem()));
            }
        }

        double acuraciaCategoria = (double) acertosCategoria / total;
        double acuraciaPrioridade = (double) acertosPrioridade / total;

        System.out.printf("%n=== %s (%d mensagens) ===%n", titulo, total);
        System.out.printf("Acurácia de categoria:  %.1f%% (%d/%d)%n",
                acuraciaCategoria * 100, acertosCategoria, total);
        System.out.printf("Acurácia de prioridade: %.1f%% (%d/%d)%n",
                acuraciaPrioridade * 100, acertosPrioridade, total);

        System.out.println();
        System.out.println("Matriz de confusão (linhas = esperado, colunas = obtido):");
        System.out.printf("%-14s", "");
        for (Categoria c : Categoria.values()) {
            System.out.printf("%-14s", c);
        }
        System.out.println();
        for (Categoria esperado : Categoria.values()) {
            System.out.printf("%-14s", esperado);
            for (int n : matriz.get(esperado)) {
                System.out.printf("%-14d", n);
            }
            System.out.println();
        }
        if (mostrarErros) {
            System.out.println();
            System.out.println("Erros de categoria:");
            errosCategoria.forEach(e -> System.out.println(" - " + e));

            System.out.println();
            System.out.println("Erros de prioridade:");
            errosPrioridade.forEach(e -> System.out.println(" - " + e));
        } else {
            System.out.println();
            System.out.println("(erros individuais ocultos: este conjunto é cego)");
        }

        return new Resultado(acuraciaCategoria, acuraciaPrioridade);
    }

    private List<Exemplo> carregar(String caminho) throws IOException {
        List<Exemplo> exemplos = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream(caminho)) {
            assertThat(in).as("arquivo %s não encontrado", caminho).isNotNull();
            BufferedReader leitor = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linha;
            while ((linha = leitor.readLine()) != null) {
                if (linha.isBlank() || linha.startsWith("#")) {
                    continue;
                }
                String[] partes = linha.split("\\|", 3);
                exemplos.add(new Exemplo(
                        Categoria.valueOf(partes[0].trim()),
                        Prioridade.valueOf(partes[1].trim()),
                        partes[2].trim()));
            }
        }
        return exemplos;
    }
}