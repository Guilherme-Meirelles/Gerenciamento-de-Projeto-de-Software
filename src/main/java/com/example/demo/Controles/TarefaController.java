package com.example.demo.Controles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Entidades.Categoria;
import com.example.demo.Entidades.ItemChecklist;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.PermissaoAreaService;
import com.example.demo.Serviços.TarefaService;
import com.example.demo.ConsultasBD.ListaRepository;
import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.ConsultasBD.TarefaRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/tarefas")
public class TarefaController {

    @Autowired
    private TarefaService tarefaService;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private ListaRepository listaRepository;

    @Autowired
    private ParticipacaoAreaRepository participacaoAreaRepository;

    @Autowired
    private PermissaoAreaService permissaoAreaService;

    // Confere se o usuário autenticado participa da área dona da lista informada.
    // exigeEdicao=true barra Observador (só Editor/Admin podem alterar dados).
    private Long usuarioComAcessoALista(Long listaId, HttpServletRequest request, boolean exigeEdicao) {
        String usuarioIdStr = SessaoUtil.getUsuarioId(request);
        if (usuarioIdStr == null) return null;
        Long usuarioId = Long.parseLong(usuarioIdStr);

        Lista lista = listaRepository.findById(listaId).orElse(null);
        if (lista == null) return null;

        Long areaId = lista.getArea().getId();
        boolean temAcesso = exigeEdicao
                ? permissaoAreaService.podeEditar(usuarioId, areaId)
                : permissaoAreaService.podeVisualizar(usuarioId, areaId);
        return temAcesso ? usuarioId : null;
    }

    // Confere se o usuário autenticado participa da área dona da tarefa informada.
    private Long usuarioComAcessoATarefa(Long tarefaId, HttpServletRequest request, boolean exigeEdicao) {
        Tarefa tarefa = tarefaRepository.findById(tarefaId).orElse(null);
        if (tarefa == null) return null;
        return usuarioComAcessoALista(tarefa.getListaOrigem().getId(), request, exigeEdicao);
    }

    // CRIAR TAREFA
    @PostMapping
    public ResponseEntity<?> criarTarefa(@RequestBody Map<String, Object> body, HttpServletRequest request) {

        Long listaId = Long.valueOf(body.get("listaId").toString());

        if (usuarioComAcessoALista(listaId, request, true) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String titulo = (String) body.get("titulo");
        String descricao = (String) body.get("descricao");
        Integer cor = body.get("cor") != null ? Integer.valueOf(body.get("cor").toString()) : null;
        String dataFim = (String) body.get("dataFim"); // parse para LocalDate
        Long responsavel = body.get("responsavelId") != null ? Long.valueOf(body.get("responsavelId").toString()) : null;
        Boolean notificacoes = body.get("notificacoes") != null ? Boolean.valueOf(body.get("notificacoes").toString()) : null;
        List<Long> categoriaIds = extrairCategoriaIds(body);
        String repeticao = (String) body.get("repeticao");

        // Chama o service
        Tarefa tarefa = tarefaService.criarTarefa(listaId, titulo, descricao, cor, dataFim, responsavel, notificacoes, categoriaIds, repeticao);

        return ResponseEntity.ok(tarefaParaJson(tarefa, responsavel));
    }

    // EDITAR TAREFA
    @PutMapping("/{id}")
    public ResponseEntity<?> editarTarefa(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {

        if (usuarioComAcessoATarefa(id, request, true) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long listaId = Long.valueOf(body.get("listaId").toString());

        if (usuarioComAcessoALista(listaId, request, true) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String titulo = (String) body.get("titulo");
        String descricao = (String) body.get("descricao");
        Integer cor = body.get("cor") != null ? Integer.valueOf(body.get("cor").toString()) : null;
        String dataFim = (String) body.get("dataFim"); // parse para LocalDate
        Long responsavelId = body.get("responsavelId") != null ? Long.valueOf(body.get("responsavelId").toString()) : null;
        Boolean notificacoes = body.get("notificacoes") != null ? Boolean.valueOf(body.get("notificacoes").toString()) : null;
        List<Long> categoriaIds = extrairCategoriaIds(body);
        String repeticao = (String) body.get("repeticao");

        Tarefa tarefa = tarefaService.editarTarefa(id, listaId, titulo, descricao, cor, dataFim, responsavelId, notificacoes, categoriaIds, repeticao);

        return ResponseEntity.ok(tarefaParaJson(tarefa, responsavelId));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggleTarefa(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {

        if (usuarioComAcessoATarefa(id, request, true) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long responsavelId = body.get("responsavelId") != null ? Long.valueOf(body.get("responsavelId").toString()) : null;
        Boolean concluida = body.get("concluida") != null ? Boolean.valueOf(body.get("concluida").toString()) : null;

        Tarefa tarefa = tarefaService.toggleTarefa(id, concluida);

        Map<String, Object> tarefaJson = tarefaParaJson(tarefa, responsavelId);
        tarefaJson.put("concluida", tarefa.getStatus());

        return ResponseEntity.ok(tarefaJson);
    }

    // LISTAR TAREFAS DA LISTA
    @GetMapping("/lista/{listaId}")
    public ResponseEntity<?> listarPorLista(@PathVariable Long listaId, HttpServletRequest request) {
        if (usuarioComAcessoALista(listaId, request, false) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(tarefaService.listarTarefasPorLista(listaId));
    }

    // DELETAR TAREFA
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id, HttpServletRequest request) {
        if (usuarioComAcessoATarefa(id, request, true) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        tarefaService.remover(id);
        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("unchecked")
    private List<Long> extrairCategoriaIds(Map<String, Object> body) {
        Object bruto = body.get("categoriaIds");
        if (!(bruto instanceof List)) return null;
        return ((List<Object>) bruto).stream()
                .map(o -> Long.valueOf(o.toString()))
                .collect(Collectors.toList());
    }

    private Map<String, Object> tarefaParaJson(Tarefa tarefa, Long responsavelId) {
        Map<String, Object> tarefaJson = new HashMap<>();
        tarefaJson.put("id", tarefa.getId());
        tarefaJson.put("titulo", tarefa.getTitulo());
        tarefaJson.put("descricao", tarefa.getDescricao());
        tarefaJson.put("cor", tarefa.getCor());
        tarefaJson.put("dataFim", tarefa.getDataFim() != null ? tarefa.getDataFim().toString() : null);
        tarefaJson.put("listaId", tarefa.getListaOrigem() != null ? tarefa.getListaOrigem().getId() : null);
        tarefaJson.put("responsavelId", responsavelId);
        tarefaJson.put("notificacoes", tarefa.getNotificacoes());
        tarefaJson.put("repeticao", tarefa.getRepeticao() != null ? tarefa.getRepeticao().name() : "NENHUMA");
        if (tarefa.getChecklist() != null) {
            tarefaJson.put("checklistId", tarefa.getChecklist().getId());
            tarefaJson.put("checklistTotal", tarefa.getChecklist().getItens().size());
            tarefaJson.put("checklistConcluidos", tarefa.getChecklist().getItens().stream()
                    .filter(ItemChecklist::getConcluido).count());
        } else {
            tarefaJson.put("checklistId", null);
            tarefaJson.put("checklistTotal", 0);
            tarefaJson.put("checklistConcluidos", 0);
        }
        tarefaJson.put("categoriaIds", tarefa.getCategorias().stream().map(Categoria::getId).collect(Collectors.toList()));
        tarefaJson.put("anexoTotal", tarefa.getAnexos().size());
        return tarefaJson;
    }
}
