package com.example.demo.Controles;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

import com.example.demo.Entidades.Tarefa;
import com.example.demo.Serviços.TarefaService;
import com.example.demo.ConsultasBD.TarefaRepository;

@RestController
@RequestMapping("/tarefas")
public class TarefaController {

    @Autowired
    private TarefaService tarefaService;

    // CRIAR TAREFA
    @PostMapping
    public ResponseEntity<Map<String, Object>> criarTarefa(@RequestBody Map<String, Object> body) {
        
        Long listaId = Long.valueOf(body.get("listaId").toString());
        String titulo = (String) body.get("titulo");
        String descricao = (String) body.get("descricao");
        Integer cor = body.get("cor") != null ? Integer.valueOf(body.get("cor").toString()) : null;
        String dataFim = (String) body.get("dataFim"); // parse para LocalDate
        Long responsavel = body.get("responsavelId") != null ? Long.valueOf(body.get("responsavelId").toString()) : null;
        Boolean notificacoes = body.get("notificacoes") != null ? Boolean.valueOf(body.get("notificacoes").toString()) : null;
        Long checklistId = body.get("checklistId") != null ? Long.valueOf(body.get("checklistId").toString()) : null;

        // Chama o service
        Tarefa tarefa = tarefaService.criarTarefa(listaId, titulo, descricao, cor, dataFim, responsavel, notificacoes, checklistId);

        Map<String, Object> tarefaJson = new HashMap<>();
        tarefaJson.put("id", tarefa.getId());
        tarefaJson.put("titulo", tarefa.getTitulo());
        tarefaJson.put("descricao", tarefa.getDescricao());
        tarefaJson.put("cor", tarefa.getCor());
        tarefaJson.put("dataFim", tarefa.getDataFim() != null ? tarefa.getDataFim().toString() : null);
        tarefaJson.put("listaId", tarefa.getListaOrigem() != null ? tarefa.getListaOrigem().getId() : null);
        tarefaJson.put("responsavelId", responsavel);
        tarefaJson.put("notificacoes", tarefa.getNotificacoes());

        return ResponseEntity.ok(tarefaJson);
    }

    // EDITAR TAREFA
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> editarTarefa(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {

        Long listaId = Long.valueOf(body.get("listaId").toString());
        String titulo = (String) body.get("titulo");
        String descricao = (String) body.get("descricao");
        Integer cor = body.get("cor") != null ? Integer.valueOf(body.get("cor").toString()) : null;
        String dataFim = (String) body.get("dataFim"); // parse para LocalDate
        Long responsavelId = body.get("responsavelId") != null ? Long.valueOf(body.get("responsavelId").toString()) : null;
        Boolean notificacoes = body.get("notificacoes") != null ? Boolean.valueOf(body.get("notificacoes").toString()) : null;
        Long checklistId = body.get("checklistId") != null ? Long.valueOf(body.get("checklistId").toString()) : null;

        Tarefa tarefa = tarefaService.editarTarefa(id, listaId, titulo, descricao, cor, dataFim, responsavelId, notificacoes, checklistId);

        Map<String, Object> tarefaJson = new HashMap<>();
        tarefaJson.put("id", tarefa.getId());
        tarefaJson.put("titulo", tarefa.getTitulo());
        tarefaJson.put("descricao", tarefa.getDescricao());
        tarefaJson.put("cor", tarefa.getCor());
        tarefaJson.put("dataFim", tarefa.getDataFim() != null ? tarefa.getDataFim().toString() : null);
        tarefaJson.put("listaId", tarefa.getListaOrigem() != null ? tarefa.getListaOrigem().getId() : null);
        tarefaJson.put("responsavelId", responsavelId);
        tarefaJson.put("notificacoes", tarefa.getNotificacoes());

        return ResponseEntity.ok(tarefaJson);
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleTarefa(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {

        Long responsavelId = body.get("responsavelId") != null ? Long.valueOf(body.get("responsavelId").toString()) : null;
        Boolean concluida = body.get("concluida") != null ? Boolean.valueOf(body.get("concluida").toString()) : null;

        Tarefa tarefa = tarefaService.toggleTarefa(id, concluida);

        Map<String, Object> tarefaJson = new HashMap<>();
        tarefaJson.put("id", tarefa.getId());
        tarefaJson.put("titulo", tarefa.getTitulo());
        tarefaJson.put("descricao", tarefa.getDescricao());
        tarefaJson.put("cor", tarefa.getCor());
        tarefaJson.put("dataFim", tarefa.getDataFim() != null ? tarefa.getDataFim().toString() : null);
        tarefaJson.put("listaId", tarefa.getListaOrigem() != null ? tarefa.getListaOrigem().getId() : null);
        tarefaJson.put("responsavelId", responsavelId);
        tarefaJson.put("notificacoes", tarefa.getNotificacoes());
        tarefaJson.put("concluida", tarefa.getStatus());

        return ResponseEntity.ok(tarefaJson);
    }

    // LISTAR TAREFAS DA LISTA
    @GetMapping("/lista/{listaId}")
    public ResponseEntity<List<Tarefa>> listarPorLista(@PathVariable Long listaId) {
        return ResponseEntity.ok(tarefaService.listarTarefasPorLista(listaId));
    }

    // DELETAR TAREFA
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        tarefaService.remover(id);
        return ResponseEntity.ok().build();
    }
}
