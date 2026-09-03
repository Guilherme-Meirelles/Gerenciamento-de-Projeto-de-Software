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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.ConsultasBD.ListaRepository;
import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.Entidades.Categoria;
import com.example.demo.Entidades.ItemChecklist;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.ListaService;
import com.example.demo.Serviços.TarefaService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/listas")
public class ListaController {

    @Autowired
    private ListaService listaService;

    @Autowired
    private TarefaService tarefaService;

    @Autowired
    private ListaRepository listaRepository;

    @Autowired
    private ParticipacaoAreaRepository participacaoAreaRepository;

    // Confere se o usuário autenticado participa da área dona da lista.
    // Evita que um usuário logado leia/edite/apague listas e tarefas de
    // áreas de trabalho às quais ele não pertence, só por adivinhar o ID.
    private Long usuarioAutenticadoComAcesso(Long listaId, HttpServletRequest request) {
        String usuarioIdStr = SessaoUtil.getUsuarioId(request);
        if (usuarioIdStr == null) return null;
        Long usuarioId = Long.parseLong(usuarioIdStr);

        Lista lista = listaRepository.findById(listaId).orElse(null);
        if (lista == null) return null;

        boolean temAcesso = participacaoAreaRepository.existsByUsuarioIdAndAreaId(usuarioId, lista.getArea().getId());
        return temAcesso ? usuarioId : null;
    }

    @PostMapping
    public ResponseEntity<?> criarLista(
            @RequestParam Long areaId,
            @RequestParam String nome,
            @RequestParam(required = false) String descricao,
            HttpServletRequest request
    ) {
        String usuarioIdStr = SessaoUtil.getUsuarioId(request);
        if (usuarioIdStr == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long usuarioId = Long.parseLong(usuarioIdStr);

        if (!participacaoAreaRepository.existsByUsuarioIdAndAreaId(usuarioId, areaId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Lista lista = listaService.criarLista(areaId, nome, descricao);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{listaId}/tarefas")
    @ResponseBody
    public ResponseEntity<?> listarTarefasPorLista(@PathVariable Long listaId, HttpServletRequest request) {
        if (usuarioAutenticadoComAcesso(listaId, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Tarefa> tarefas = tarefaService.listarTarefasPorLista(listaId);

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
            if (t.getChecklist() != null) {
                mapa.put("checklistId", t.getChecklist().getId());
                mapa.put("checklistTotal", t.getChecklist().getItens().size());
                mapa.put("checklistConcluidos", t.getChecklist().getItens().stream()
                        .filter(ItemChecklist::getConcluido).count());
            } else {
                mapa.put("checklistId", null);
                mapa.put("checklistTotal", 0);
                mapa.put("checklistConcluidos", 0);
            }
            mapa.put("categoriaIds", t.getCategorias().stream().map(Categoria::getId).collect(Collectors.toList()));
            return mapa;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(tarefasJson);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarLista(
            @PathVariable Long id,
            @RequestParam String nome,
            @RequestParam String descricao,
            HttpServletRequest request
    ) {
        if (usuarioAutenticadoComAcesso(id, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Lista lista = listaService.editarLista(id, nome, descricao);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLista(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        if (usuarioAutenticadoComAcesso(id, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Lista lista = listaService.getLista(id);
        return ResponseEntity.ok(lista);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarLista(@PathVariable Long id, HttpServletRequest request) {
        if (usuarioAutenticadoComAcesso(id, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        listaService.deletarLista(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/area/{areaId}")
    public ResponseEntity<?> listarPorArea(@PathVariable Long areaId, HttpServletRequest request) {
        String usuarioIdStr = SessaoUtil.getUsuarioId(request);
        if (usuarioIdStr == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long usuarioId = Long.parseLong(usuarioIdStr);

        if (!participacaoAreaRepository.existsByUsuarioIdAndAreaId(usuarioId, areaId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(listaService.listarPorArea(areaId));
    }
}
