package com.example.demo.Controles;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.ConsultasBD.AnexoRepository;
import com.example.demo.ConsultasBD.ListaRepository;
import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.Entidades.Anexo;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.AnexoService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class AnexoController {

    @Autowired
    private AnexoService anexoService;

    @Autowired
    private AnexoRepository anexoRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

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

    // Confere se o usuário autenticado participa da área dona da tarefa do anexo informado.
    private Long usuarioComAcessoAoAnexo(Long anexoId, HttpServletRequest request) {
        Anexo anexo = anexoRepository.findById(anexoId).orElse(null);
        if (anexo == null) return null;
        return usuarioComAcessoATarefa(anexo.getTarefa().getId(), request);
    }

    private Map<String, Object> anexoParaJson(Anexo anexo) {
        return Map.of(
                "id", anexo.getId(),
                "nomeArquivo", anexo.getNomeArquivo(),
                "tipoConteudo", anexo.getTipoConteudo() != null ? anexo.getTipoConteudo() : "",
                "tamanho", anexo.getTamanho()
        );
    }

    // ANEXOS DE UMA TAREFA

    @GetMapping("/tarefas/{tarefaId}/anexos")
    public ResponseEntity<?> listar(@PathVariable Long tarefaId, HttpServletRequest request) {
        if (usuarioComAcessoATarefa(tarefaId, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Map<String, Object>> anexos = anexoService.listarPorTarefa(tarefaId).stream()
                .map(this::anexoParaJson)
                .collect(Collectors.toList());

        return ResponseEntity.ok(anexos);
    }

    @PostMapping("/tarefas/{tarefaId}/anexos")
    public ResponseEntity<?> adicionar(
            @PathVariable Long tarefaId,
            @RequestParam("arquivo") MultipartFile arquivo,
            HttpServletRequest request
    ) {
        if (usuarioComAcessoATarefa(tarefaId, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (arquivo == null || arquivo.isEmpty()) {
            return ResponseEntity.badRequest().body("Nenhum arquivo enviado.");
        }

        try {
            Anexo anexo = anexoService.adicionar(tarefaId, arquivo);
            return ResponseEntity.ok(anexoParaJson(anexo));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao salvar o arquivo.");
        }
    }

    @GetMapping("/anexos/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id, HttpServletRequest request) {
        if (usuarioComAcessoAoAnexo(id, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Anexo anexo = anexoService.buscarPorId(id);

        MediaType tipo;
        try {
            tipo = anexo.getTipoConteudo() != null && !anexo.getTipoConteudo().isBlank()
                    ? MediaType.parseMediaType(anexo.getTipoConteudo())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            tipo = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + anexo.getNomeArquivo() + "\"")
                .body(anexo.getDados());
    }

    @DeleteMapping("/anexos/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id, HttpServletRequest request) {
        if (usuarioComAcessoAoAnexo(id, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        anexoService.remover(id);
        return ResponseEntity.ok().build();
    }
}
