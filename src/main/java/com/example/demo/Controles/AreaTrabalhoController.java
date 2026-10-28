package com.example.demo.Controles;

import com.example.demo.ConsultasBD.*;
import com.example.demo.Entidades.*;
import com.example.demo.Serviços.AreaTrabalhoService;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.ConviteAreaService;
import com.example.demo.Serviços.ListaService;
import com.example.demo.Serviços.PermissaoAreaService;
import com.example.demo.Serviços.TarefaService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AreaTrabalhoController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AreaTrabalhoRepository areaTrabalhoRepository;

    @Autowired
    private ParticipacaoAreaRepository participacaoAreaRepository;

    @Autowired
    private ListaRepository listaRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private ListaService listaService;

    @Autowired
    private TarefaService tarefaService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private ConviteAreaService conviteAreaService;

    @Autowired
    private PermissaoAreaService permissaoAreaService;

    @Autowired
    private AreaTrabalhoService areaTrabalhoService;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final String ATRIBUTO_CONVITE_PENDENTE = "conviteAreaPendente";

    @GetMapping("/areasTrabalho")
    public String areasTrabalho(Model model, HttpServletRequest request) {

        String usuarioId = SessaoUtil.getUsuarioId(request);

        if (usuarioId == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);

        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuarioId", usuarioId);
        model.addAttribute("nome", usuario.getNome());
        model.addAttribute("email", usuario.getEmail());
        model.addAttribute("dataNascimento", usuario.getDataNascimento());

        List<ParticipacaoArea> participacaoAreas = participacaoAreaRepository.findByUsuario(usuario);

        model.addAttribute("usuario", usuario);
        model.addAttribute("isMenu", true);
        model.addAttribute("participacoes", participacaoAreas);

        return "areasTrabalho";
    }

    @GetMapping("/areasTrabalho/{id}/{name}")
    public String areasTrabalhoId(@PathVariable Long id, @PathVariable String name, Model model, HttpServletRequest request) {

        String usuarioId = SessaoUtil.getUsuarioId(request);

        if (usuarioId == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);

        if (usuario == null) {
            return "redirect:/login";
        }

        AreaTrabalho area = areaTrabalhoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Área não encontrada"));

        boolean temAcesso = area.getParticipacoes().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(usuario.getId()));
        if (!temAcesso) {
            return "redirect:/areasTrabalho";
        }

        List<Lista> listas = listaService.listarPorArea(id);

        model.addAttribute("nome", usuario.getNome());
        model.addAttribute("email", usuario.getEmail());
        model.addAttribute("dataNascimento", usuario.getDataNascimento());

        model.addAttribute("areaId", id);
        model.addAttribute("areaName", name);
        model.addAttribute("area", area);
        model.addAttribute("listas", listas);

        model.addAttribute("podeEditar", permissaoAreaService.podeEditar(usuario.getId(), id));

        return "menu";
    }

    @PostMapping("/areasTrabalho/criar")
    @ResponseBody
    public String criarArea(@RequestParam String nome, HttpServletRequest request) {

        String usuarioId = SessaoUtil.getUsuarioId(request);
        if (usuarioId == null) {
            return "NOT_LOGGED";
        }

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) {
            return "USER_NOT_FOUND";
        }

        AreaTrabalho area = new AreaTrabalho();
        area.setNome(nome);
        area.setDono(usuario);

        areaTrabalhoRepository.save(area);

        ParticipacaoArea participacao = new ParticipacaoArea();
        participacao.setArea(area);
        participacao.setUsuario(usuario);
        participacao.setPermissao(PermissaoArea.ADMIN);

        area.getParticipacoes().add(participacao);
        areaTrabalhoRepository.save(area);

        return area.getId().toString(); // o JS recebe esse ID e adiciona no HTML
    }

    @PutMapping("/areasTrabalho/{id}/renomear")
    public ResponseEntity<String> renomearArea(@PathVariable Long id,
                                            @RequestBody(required = false) String body,
                                            HttpServletRequest request) {

        String novoNome = request.getParameter("nome");
        if (novoNome == null || novoNome.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("INVALID_NAME");
        }

        String usuarioId = SessaoUtil.getUsuarioId(request);
        if (usuarioId == null) return ResponseEntity.status(403).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(403).body("NOT_LOGGED");

        AreaTrabalho area = areaTrabalhoRepository.findById(id).orElse(null);
        if (area == null) return ResponseEntity.status(404).body("NOT_FOUND");

        if (!area.getDono().getId().equals(usuario.getId())) {
            return ResponseEntity.status(403).body("NOT_OWNER");
        }

        area.setNome(novoNome);
        areaTrabalhoRepository.save(area);
        return ResponseEntity.ok("OK");
    }

    @DeleteMapping("/areasTrabalho/excluir")
    public ResponseEntity<?> excluirAreas(@RequestBody Map<String, List<Long>> body, HttpServletRequest request) {
        String usuarioIdString = SessaoUtil.getUsuarioId(request);
        if (usuarioIdString == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long usuarioId = Long.parseLong(usuarioIdString);
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Long> ids = body.get("ids");
        for (Long id : ids) {
            AreaTrabalho area = areaTrabalhoRepository.findById(id).orElse(null);
            if (area != null){
                List<ParticipacaoArea> participacoes = participacaoAreaRepository.findByArea(area);
                ParticipacaoArea participacaoArea = participacoes.stream().filter(e -> e.getUsuario().getId().equals(usuarioId)).findFirst().orElse(null);
                if (participacaoArea == null) {
                    continue;
                }
                if (participacaoArea.getPermissao().equals(PermissaoArea.ADMIN)) {
                    areaTrabalhoService.excluirArea(area);
                    for (ParticipacaoArea p : participacoes) {
                        participacaoAreaRepository.delete(p);
                    }
                }
                else{
                    participacaoAreaRepository.delete(participacaoArea);
                }
            }


        }
        return ResponseEntity.ok().body(Map.of("status", "success"));
    }

    @GetMapping("/areasTrabalho/gerar-link/{id}")
    @ResponseBody
    public ResponseEntity<?> gerarLink(
            @PathVariable Long id,
            @RequestParam(defaultValue = "EDITOR") String permissao,
            HttpServletRequest request
    ) {

        String usuarioId = SessaoUtil.getUsuarioId(request);
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Usuário não logado"));

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Usuário não encontrado"));

        AreaTrabalho area = areaTrabalhoRepository.findById(id).orElse(null);
        if (area == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Área não encontrada"));

        // Observador não pode gerar convite (mesmo que fosse pra um nível menor que o dele).
        if (!permissaoAreaService.podeEditar(usuario.getId(), id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Você não tem permissão para convidar pessoas nesta área"));
        }

        PermissaoArea nivel;
        try {
            nivel = PermissaoArea.valueOf(permissao.toUpperCase());
        } catch (IllegalArgumentException e) {
            nivel = PermissaoArea.EDITOR;
        }

        // Só Admin pode gerar um link que concede nível Admin; Editor gerando link cai pra Editor.
        if (nivel == PermissaoArea.ADMIN && !permissaoAreaService.podeAdministrar(usuario.getId(), id)) {
            nivel = PermissaoArea.EDITOR;
        }

        // Gera e salva um token de convite real (link genérico: sem e-mail associado,
        // reutilizável por qualquer um que abrir o link até expirar).
        String tokenString = UUID.randomUUID().toString();

        Token token = new Token();
        token.setToken(tokenString);
        token.setEmail(null);
        token.setAreaId(id);
        token.setPermissao(nivel);
        token.setExpiraEm(LocalDateTime.now().plusDays(7));
        token.setUsado(false);
        tokenRepository.save(token);

        String link = baseUrl + "/areasTrabalho/entrar/" + tokenString;

        return ResponseEntity.ok(Map.of("success", true, "link", link));
    }

    // Endpoint que o link de "Copiar Link" abre. Se o usuário não estiver logado,
    // guarda o convite na sessão e manda pro login — ele é retomado automaticamente
    // assim que o login (ou cadastro + verificação) for concluído.
    @GetMapping("/areasTrabalho/entrar/{token}")
    public String entrarPorLink(@PathVariable String token, HttpServletRequest request, Model model) {

        String usuarioIdStr = SessaoUtil.getUsuarioId(request);
        if (usuarioIdStr == null) {
            request.getSession(true).setAttribute(ATRIBUTO_CONVITE_PENDENTE, token);
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioIdStr)).orElse(null);
        if (usuario == null) {
            return "redirect:/login";
        }

        ConviteAreaService.Resultado resultado = conviteAreaService.aceitarLinkGenerico(token, usuario);
        if (!resultado.sucesso()) {
            model.addAttribute("erro", resultado.mensagem());
            return "login";
        }

        return "redirect:/areasTrabalho/" + resultado.area().getId() + "/"
                + UriUtils.encodePathSegment(resultado.area().getNome(), StandardCharsets.UTF_8);
    }

    @GetMapping("/areasTrabalho/{id}/membros")
    @ResponseBody
    public ResponseEntity<?> listarMembros(@PathVariable Long id, HttpServletRequest request) {
        String usuarioId = SessaoUtil.getUsuarioId(request);
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        AreaTrabalho area = areaTrabalhoRepository.findById(id).orElse(null);
        if (area == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("AREA_NOT_FOUND");

        boolean temAcesso = area.getParticipacoes().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(usuario.getId()));
        if (!temAcesso) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("NO_ACCESS");

        List<Map<String, Object>> membros = area.getParticipacoes().stream()
            .map(p -> {
                Map<String, Object> mapa = new HashMap<>();
                mapa.put("id", p.getUsuario().getId());
                mapa.put("nome", p.getUsuario().getNome());
                mapa.put("email", p.getUsuario().getEmail());
                mapa.put("permissao", p.getPermissao().name().toLowerCase());
                return mapa;
            })
            .collect(Collectors.toList());


        return ResponseEntity.ok(membros);
    }

    @GetMapping("/areasTrabalho/{id}/listas")
    @ResponseBody
    public ResponseEntity<?> listarListas(@PathVariable Long id, HttpServletRequest request) {
        String usuarioId = SessaoUtil.getUsuarioId(request);
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        AreaTrabalho area = areaTrabalhoRepository.findById(id).orElse(null);
        if (area == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("AREA_NOT_FOUND");

        boolean temAcesso = area.getParticipacoes().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(usuario.getId()));
        if (!temAcesso) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("NO_ACCESS");

        List<Lista> listas = listaService.listarPorArea(id);

        List<Map<String, Object>> listasJson = listas.stream()
                .map(l -> {
                    Map<String, Object> mapa = new HashMap<>();
                    mapa.put("id", l.getId());
                    mapa.put("nome", l.getNome());
                    mapa.put("descricao", l.getDescricao());
                    mapa.put("areaId", l.getArea().getId());
                    return mapa;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(listasJson);
    }

    @GetMapping("/areasTrabalho/{id}/categorias")
    @ResponseBody
    public ResponseEntity<?> listarCategorias(@PathVariable Long id, HttpServletRequest request) {
        String usuarioId = SessaoUtil.getUsuarioId(request);
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        AreaTrabalho area = areaTrabalhoRepository.findById(id).orElse(null);
        if (area == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("AREA_NOT_FOUND");

        boolean temAcesso = area.getParticipacoes().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(usuario.getId()));
        if (!temAcesso) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("NO_ACCESS");

        List<Categoria> categorias = categoriaRepository.findByAreaId(id);

        List<Map<String, Object>> categoriasJson = categorias.stream()
                .map(c -> {
                    Map<String, Object> mapa = new HashMap<>();
                    mapa.put("id", c.getId());
                    mapa.put("nome", c.getNome());
                    mapa.put("cor", c.getCor());
                    return mapa;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(categoriasJson);
    }

    @GetMapping("/areasTrabalho/{areaId}/tarefas")
    @ResponseBody
    public ResponseEntity<?> listarTarefasPorArea(@PathVariable Long areaId, HttpServletRequest request) {
        String usuarioId = SessaoUtil.getUsuarioId(request);
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        AreaTrabalho area = areaTrabalhoRepository.findById(areaId).orElse(null);
        if (area == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("AREA_NOT_FOUND");

        boolean temAcesso = area.getParticipacoes().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(usuario.getId()));
        if (!temAcesso) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("NO_ACCESS");

        List<Lista> listas = listaService.listarPorArea(areaId);

        List<Tarefa> todasTarefas = new ArrayList<>();
        for (Lista lista : listas) {
            todasTarefas.addAll(lista.getTarefas());
        }

        List<Map<String, Object>> tarefasJson = todasTarefas.stream().map(t -> {
            Map<String, Object> mapa = new HashMap<>();
            mapa.put("id", t.getId());
            mapa.put("titulo", t.getTitulo());
            mapa.put("descricao", t.getDescricao());
            mapa.put("cor", t.getCor());
            mapa.put("dataFim", t.getDataFim() != null ? t.getDataFim().toString() : null);
            mapa.put("listaId", t.getListaOrigem().getId());
            mapa.put("listaNome", t.getListaOrigem().getNome());
            mapa.put("areaNome", t.getListaOrigem().getArea().getNome());
            mapa.put("concluida", t.getStatus());
            if (!t.getResponsaveis().isEmpty()) {
                mapa.put("responsavel", t.getResponsaveis().iterator().next().getId());
                mapa.put("responsavelNome", t.getResponsaveis().iterator().next().getNome());
            } else {
                mapa.put("responsavel", null);
                mapa.put("responsavelNome", null);
            }
            mapa.put("notificacoes", t.getNotificacoes());
            mapa.put("repeticao", t.getRepeticao() != null ? t.getRepeticao().name() : "NENHUMA");
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
            mapa.put("anexoTotal", t.getAnexos().size());
            return mapa;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(tarefasJson);
    }

    @GetMapping("/user/tarefas")
    @ResponseBody
    public ResponseEntity<?> listarTarefasPorUser(HttpServletRequest request) {

        String usuarioId = SessaoUtil.getUsuarioId(request);

        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        List<AreaTrabalho> areas = participacaoAreaRepository.findByUsuario(usuario).stream()
                .map(ParticipacaoArea::getArea)
                .collect(Collectors.toList());


        List<Lista> todasListas = new ArrayList<>();
        for (AreaTrabalho area : areas) {
            todasListas.addAll(area.getListas());
        }

        List<Tarefa> todasTarefas = new ArrayList<>();
        for (Lista lista : todasListas) {
            todasTarefas.addAll(lista.getTarefas());
        }

        List<Map<String, Object>> tarefasJson = todasTarefas.stream().map(t -> {
            Map<String, Object> mapa = new HashMap<>();
            mapa.put("id", t.getId());
            mapa.put("titulo", t.getTitulo());
            mapa.put("descricao", t.getDescricao());
            mapa.put("cor", t.getCor());
            mapa.put("dataFim", t.getDataFim() != null ? t.getDataFim().toString() : null);
            mapa.put("listaId", t.getListaOrigem().getId());
            mapa.put("listaNome", t.getListaOrigem().getNome());
            mapa.put("areaNome", t.getListaOrigem().getArea().getNome());
            mapa.put("concluida", t.getStatus());
            if (!t.getResponsaveis().isEmpty()) {
                mapa.put("responsavel", t.getResponsaveis().iterator().next().getId());
                mapa.put("responsavelNome", t.getResponsaveis().iterator().next().getNome());
            } else {
                mapa.put("responsavel", null);
                mapa.put("responsavelNome", null);
            }
            mapa.put("notificacoes", t.getNotificacoes());
            mapa.put("repeticao", t.getRepeticao() != null ? t.getRepeticao().name() : "NENHUMA");
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
            mapa.put("anexoTotal", t.getAnexos().size());
            return mapa;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(tarefasJson);
    }

    @GetMapping("/user/listas")
    @ResponseBody
    public ResponseEntity<?> listarListasPorUser(HttpServletRequest request) {

        String usuarioId = SessaoUtil.getUsuarioId(request);

        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        List<AreaTrabalho> areas = participacaoAreaRepository.findByUsuario(usuario).stream()
                .map(ParticipacaoArea::getArea)
                .collect(Collectors.toList());

        List<Lista> todasListas = new ArrayList<>();
        for (AreaTrabalho area : areas) {
            todasListas.addAll(area.getListas());
        }

        List<Map<String, Object>> listasJson = todasListas.stream()
                .map(l -> {
                    Map<String, Object> mapa = new HashMap<>();
                    mapa.put("id", l.getId());
                    mapa.put("nome", l.getNome());
                    mapa.put("descricao", l.getDescricao());
                    mapa.put("areaId", l.getArea().getId());
                    return mapa;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(listasJson);
    }

    @GetMapping("/user/membros")
    @ResponseBody
    public ResponseEntity<?> listarUserMembros(HttpServletRequest request) {
        String usuarioId = SessaoUtil.getUsuarioId(request);
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        List<AreaTrabalho> areas = participacaoAreaRepository.findByUsuario(usuario).stream()
                .map(ParticipacaoArea::getArea)
                .collect(Collectors.toList());

        List<ParticipacaoArea> todosMembros = new ArrayList<>();
        for (AreaTrabalho area : areas) {
            todosMembros.addAll(area.getParticipacoes());
        }

        List<Map<String, Object>> membros = todosMembros.stream()
            .map(p -> {
                Map<String, Object> mapa = new HashMap<>();
                mapa.put("id", p.getUsuario().getId());
                mapa.put("nome", p.getUsuario().getNome());
                mapa.put("email", p.getUsuario().getEmail());
                mapa.put("permissao", p.getPermissao().name().toLowerCase());
                return mapa;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(membros);
    }
    // Altera o nível de permissão (EDITOR/OBSERVADOR) de um membro da área.
    // Só ADMIN da área pode chamar, e o nível ADMIN não pode ser alterado por aqui,
    // para a área nunca ficar sem administrador.
    @PutMapping("/areasTrabalho/{areaId}/membros/{usuarioId}/permissao")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> alterarPermissaoMembro(@PathVariable Long areaId,
                                                    @PathVariable Long usuarioId,
                                                    @RequestBody Map<String, String> corpo,
                                                    HttpServletRequest request) {
        String solicitanteId = SessaoUtil.getUsuarioId(request);
        if (solicitanteId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Não autenticado."));
        }

        if (!permissaoAreaService.podeAdministrar(Long.parseLong(solicitanteId), areaId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Apenas administradores podem alterar permissões."));
        }

        PermissaoArea nova;
        try {
            nova = PermissaoArea.valueOf(String.valueOf(corpo.get("permissao")).toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Permissão inválida."));
        }
        if (nova == PermissaoArea.ADMIN) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Use EDITOR ou OBSERVADOR."));
        }

        ParticipacaoArea alvo = participacaoAreaRepository.findByUsuarioIdAndAreaId(usuarioId, areaId);
        if (alvo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Este usuário não faz parte desta área."));
        }
        if (alvo.getPermissao() == PermissaoArea.ADMIN) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "A permissão de um administrador não pode ser alterada."));
        }

        alvo.setPermissao(nova);
        participacaoAreaRepository.save(alvo);
        return ResponseEntity.ok(Map.of("success", true, "permissao", nova.name().toLowerCase()));
    }

    @PostMapping("/remover-membro")
    @ResponseBody
    @Transactional // Necessário para operações de delete funcionarem corretamente
    public ResponseEntity<?> removerMembro(@RequestBody Map<String, Object> map, HttpServletRequest request) {
        try {
            // 0. Só quem está logado pode remover membro
            String solicitanteId = SessaoUtil.getUsuarioId(request);
            if (solicitanteId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Não autenticado."));
            }

            if (map.get("usuarioId") == null || map.get("areaId") == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "IDs não fornecidos."));
            }

            // Trata tanto se vier como String quanto Integer/Long do JSON
            Long idUsuario = Long.parseLong(map.get("usuarioId").toString());
            Long idArea = Long.parseLong(map.get("areaId").toString());
            Long idSolicitante = Long.parseLong(solicitanteId);

            Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
            AreaTrabalho area = areaTrabalhoRepository.findById(idArea).orElse(null);

            if (area == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Área não encontrada."));
            }

            if (usuario == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Usuário não encontrado."));
            }

            List<ParticipacaoArea> todosMembros = participacaoAreaRepository.findByArea(area);

            // Só ADMIN da área pode remover membro
            ParticipacaoArea participacaoSolicitante = todosMembros.stream()
                    .filter(p -> p.getUsuario().getId().equals(idSolicitante))
                    .findFirst().orElse(null);

            if (participacaoSolicitante == null || participacaoSolicitante.getPermissao() != PermissaoArea.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Apenas administradores podem remover membros."));
            }

            boolean removeu = false;

            for (ParticipacaoArea participacao : todosMembros) {
                if (participacao.getUsuario().getId().equals(idUsuario)) {
                    participacaoAreaRepository.delete(participacao);
                    removeu = true;
                    break;
                }
            }

            if (!removeu) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Este usuário não faz parte desta área."));
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Membro removido com sucesso!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Erro interno: " + e.getMessage()));
        }
    }
}