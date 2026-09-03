package com.example.demo.Controles;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.ConsultasBD.ItemChecklistRepository;
import com.example.demo.ConsultasBD.ListaRepository;
import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.Entidades.Checklist;
import com.example.demo.Entidades.ItemChecklist;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.ChecklistService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ChecklistController {

    @Autowired
    private ChecklistService checklistService;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private ItemChecklistRepository itemChecklistRepository;

    @Autowired
    private ListaRepository listaRepository;

    @Autowired
    private ParticipacaoAreaRepository participacaoAreaRepository;

    // Confere se o usuário autenticado participa da área dona da lista informada.
    private Long usuarioComAcessoALista(Long listaId, HttpServletRequest request) {
        String usuarioIdStr = SessaoUtil.getUsuarioId(request);
        if (usuarioIdStr == null) return null;
        Long usuarioId = Long.parseLong(usuarioIdStr);

        Lista lista = listaRepository.findById(listaId).orElse(null);
        if (lista == null) return null;

        boolean temAcesso = participacaoAreaRepository.existsByUsuarioIdAndAreaId(usuarioId, lista.getArea().getId());
        return temAcesso ? usuarioId : null;
    }

    // Confere se o usuário autenticado participa da área dona da tarefa informada.
    private Long usuarioComAcessoATarefa(Long tarefaId, HttpServletRequest request) {
        Tarefa tarefa = tarefaRepository.findById(tarefaId).orElse(null);
        if (tarefa == null) return null;
        return usuarioComAcessoALista(tarefa.getListaOrigem().getId(), request);
    }

    // Confere se o usuário autenticado participa da área dona da tarefa do item informado.
    private Long usuarioComAcessoAoItem(Long itemId, HttpServletRequest request) {
        ItemChecklist item = itemChecklistRepository.findById(itemId).orElse(null);
        if (item == null) return null;

        Tarefa tarefa = item.getChecklist().getTarefa();
        if (tarefa == null) return null;

        return usuarioComAcessoATarefa(tarefa.getId(), request);
    }

    // ITENS DA CHECKLIST DE UMA TAREFA

    @GetMapping("/tarefas/{tarefaId}/checklist")
    public ResponseEntity<?> getChecklist(@PathVariable Long tarefaId, HttpServletRequest request) {
        if (usuarioComAcessoATarefa(tarefaId, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Checklist checklist = checklistService.getChecklistDaTarefa(tarefaId);
        return ResponseEntity.ok(checklist != null ? checklist.getItens() : List.of());
    }

    @PostMapping("/tarefas/{tarefaId}/checklist/itens")
    public ResponseEntity<?> adicionarItem(
            @PathVariable Long tarefaId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        if (usuarioComAcessoATarefa(tarefaId, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String descricao = (String) body.get("descricao");
        ItemChecklist item = checklistService.adicionarItem(tarefaId, descricao);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/checklists/itens/{itemId}")
    public ResponseEntity<?> editarItem(
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        if (usuarioComAcessoAoItem(itemId, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String descricao = (String) body.get("descricao");
        Boolean concluido = body.get("concluido") != null ? Boolean.valueOf(body.get("concluido").toString()) : null;

        ItemChecklist item = checklistService.editarItem(itemId, descricao, concluido);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/checklists/itens/{itemId}")
    public ResponseEntity<Void> removerItem(@PathVariable Long itemId, HttpServletRequest request) {
        if (usuarioComAcessoAoItem(itemId, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        checklistService.removerItem(itemId);
        return ResponseEntity.ok().build();
    }
}
