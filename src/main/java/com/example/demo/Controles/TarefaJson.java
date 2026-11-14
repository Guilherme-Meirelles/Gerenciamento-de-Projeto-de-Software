package com.example.demo.Controles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;

// Monta os campos de responsáveis do JSON de uma tarefa. Uma tarefa pode ter vários responsáveis;
// os campos antigos de um só responsável continuam existindo (o primeiro, em ordem alfabética)
// para não quebrar quem ainda os lê.
final class TarefaJson {

    private TarefaJson() {}

    static void adicionarResponsaveis(Map<String, Object> mapa, Tarefa tarefa) {
        List<Usuario> ordenados = tarefa.getResponsaveis().stream()
                .sorted(Comparator
                        .comparing((Usuario u) -> u.getNome() == null ? "" : u.getNome().toLowerCase())
                        .thenComparing(Usuario::getId))
                .toList();

        List<Long> ids = new ArrayList<>();
        List<Map<String, Object>> pessoas = new ArrayList<>();
        for (Usuario u : ordenados) {
            ids.add(u.getId());
            pessoas.add(Map.of("id", u.getId(), "nome", u.getNome() == null ? "" : u.getNome()));
        }

        mapa.put("responsavelIds", ids);
        mapa.put("responsaveis", pessoas);
        // Compatibilidade e exibição: o primeiro id e os nomes juntos ("Ana, Beto")
        mapa.put("responsavel", ids.isEmpty() ? null : ids.get(0));
        mapa.put("responsavelId", ids.isEmpty() ? null : ids.get(0));
        mapa.put("responsavelNome", ordenados.isEmpty() ? null
                : ordenados.stream().map(Usuario::getNome).collect(Collectors.joining(", ")));
    }
}
