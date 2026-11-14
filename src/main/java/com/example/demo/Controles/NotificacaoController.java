package com.example.demo.Controles;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.PermissaoAreaService;

import jakarta.servlet.http.HttpServletRequest;

// Notificações dentro do sistema (sino da barra lateral). Seguem a mesma regra do lembrete por
// e-mail (LembreteTarefaJob): tarefas em que o usuário é responsável, com "Notificações" ligadas
// e ainda não concluídas. Aqui também aparecem as já atrasadas, que o e-mail não cobre.
// Nada é gravado: a lista é calculada a cada consulta, então some sozinha quando a tarefa é
// concluída, tem a data alterada ou a notificação desligada.
@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private PermissaoAreaService permissaoAreaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<?> listar(HttpServletRequest request) {
        String usuarioIdStr = SessaoUtil.getUsuarioId(request);
        if (usuarioIdStr == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long usuarioId = Long.parseLong(usuarioIdStr);

        Optional<Usuario> usuario = usuarioRepository.findById(usuarioId);
        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Com o aviso no sistema desligado nas preferências, o sino fica vazio (a lista não é montada)
        if (!usuario.get().isNotificarNoSistema()) {
            Map<String, Object> vazia = new HashMap<>();
            vazia.put("total", 0);
            vazia.put("itens", List.of());
            vazia.put("preferencias", preferencias(usuario.get()));
            return ResponseEntity.ok(vazia);
        }

        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(1);

        List<Map<String, Object>> itens = tarefaRepository.findByResponsaveisId(usuarioId).stream()
                .filter(t -> Boolean.TRUE.equals(t.getNotificacoes()))
                .filter(t -> !Boolean.TRUE.equals(t.getStatus()))
                .filter(t -> t.getDataFim() != null && !t.getDataFim().isAfter(limite))
                // quem saiu da área (ou foi removido dela) não recebe mais avisos das tarefas dela
                .filter(t -> t.getListaOrigem() != null
                        && permissaoAreaService.podeVisualizar(usuarioId, t.getListaOrigem().getArea().getId()))
                .sorted(Comparator.comparing(Tarefa::getDataFim).thenComparing(Tarefa::getId))
                .map(t -> paraJson(t, hoje))
                .toList();

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("total", itens.size());
        resposta.put("itens", itens);
        resposta.put("preferencias", preferencias(usuario.get()));
        return ResponseEntity.ok(resposta);
    }

    // Preferências de notificação do usuário logado: { email: boolean, sistema: boolean }.
    // Campo ausente no corpo mantém o valor atual.
    @PutMapping("/preferencias")
    public ResponseEntity<?> salvarPreferencias(@RequestBody Map<String, Object> corpo, HttpServletRequest request) {
        String usuarioIdStr = SessaoUtil.getUsuarioId(request);
        if (usuarioIdStr == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Usuario> encontrado = usuarioRepository.findById(Long.parseLong(usuarioIdStr));
        if (encontrado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario usuario = encontrado.get();

        if (corpo.get("email") instanceof Boolean email) {
            usuario.setNotificarPorEmail(email);
        }
        if (corpo.get("sistema") instanceof Boolean sistema) {
            usuario.setNotificarNoSistema(sistema);
        }
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(preferencias(usuario));
    }

    private Map<String, Object> preferencias(Usuario usuario) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("email", usuario.isNotificarPorEmail());
        mapa.put("sistema", usuario.isNotificarNoSistema());
        return mapa;
    }

    private Map<String, Object> paraJson(Tarefa tarefa, LocalDate hoje) {
        long dias = ChronoUnit.DAYS.between(hoje, tarefa.getDataFim());
        String situacao = dias < 0 ? "ATRASADA" : dias == 0 ? "HOJE" : "AMANHA";

        Map<String, Object> json = new HashMap<>();
        json.put("tarefaId", tarefa.getId());
        json.put("titulo", tarefa.getTitulo());
        json.put("dataFim", tarefa.getDataFim().toString());
        json.put("situacao", situacao);
        json.put("diasAtraso", dias < 0 ? -dias : 0);
        json.put("listaNome", tarefa.getListaOrigem().getNome());
        json.put("areaId", tarefa.getListaOrigem().getArea().getId());
        json.put("areaNome", tarefa.getListaOrigem().getArea().getNome());
        return json;
    }
}
