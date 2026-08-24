package com.example.demo.Controles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.ConsultasBD.AreaTrabalhoRepository;
import com.example.demo.ConsultasBD.ListaRepository;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Serviços.ListaService;
import com.example.demo.Serviços.TarefaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/listas")
public class ListaController {

    @Autowired
    private ListaService listaService;

    @Autowired
    private TarefaService tarefaService;

    @PostMapping
    public ResponseEntity<Lista> criarLista(
            @RequestParam Long areaId,
            @RequestParam String nome,
            @RequestParam(required = false) String descricao
    ) {
        Lista lista = listaService.criarLista(areaId, nome, descricao);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{listaId}/tarefas")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarTarefasPorLista(@PathVariable Long listaId) {
        List<Tarefa> tarefas = tarefaService.listarTarefasPorLista(listaId); // ou via service

        List<Map<String, Object>> tarefasJson = tarefas.stream().map(t -> {
            Map<String, Object> mapa = new HashMap<>();
            mapa.put("id", t.getId());
            mapa.put("titulo", t.getTitulo());
            mapa.put("descricao", t.getDescricao());
            mapa.put("cor", t.getCor());
            mapa.put("dataFim", t.getDataFim() != null ? t.getDataFim().toString() : null);
            mapa.put("listaId", t.getListaOrigem().getId());
            mapa.put("concluida", t.getStatus());
            if (!t.getResponsaveis().isEmpty()) {
                mapa.put("responsavel", t.getResponsaveis().iterator().next().getId());
                mapa.put("responsavelNome", t.getResponsaveis().iterator().next().getNome());
            } else {
                mapa.put("responsavel", null);
                mapa.put("responsavelNome", null);
            }
            mapa.put("notificacoes", t.getNotificacoes());
            return mapa;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(tarefasJson);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Lista> editarLista(
            @PathVariable Long id,
            @RequestParam String nome,
            @RequestParam String descricao
    ) {
        Lista lista = listaService.editarLista(id, nome, descricao);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lista> getLista(
            @PathVariable Long id
    ) {
        Lista lista = listaService.getLista(id);
        return ResponseEntity.ok(lista);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarLista(@PathVariable Long id) {
        listaService.deletarLista(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/area/{areaId}")
    public ResponseEntity<List<Lista>> listarPorArea(@PathVariable Long areaId) {
        return ResponseEntity.ok(listaService.listarPorArea(areaId));
    }
}