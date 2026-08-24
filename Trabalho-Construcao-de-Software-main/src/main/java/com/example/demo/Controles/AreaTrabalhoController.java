package com.example.demo.Controles;

import com.example.demo.ConsultasBD.*;
import com.example.demo.Entidades.*;
import com.example.demo.Serviços.CookieService;
import com.example.demo.Serviços.ListaService;
import com.example.demo.Serviços.TarefaService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

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

    @GetMapping("/areasTrabalho")
    public String areasTrabalho(Model model, HttpServletRequest request) {

        String usuarioId = CookieService.getCookie(request, "usuarioId");

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

        String usuarioId = CookieService.getCookie(request, "usuarioId");

        if (usuarioId == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);

        if (usuario == null) {
            return "redirect:/login";
        }

        AreaTrabalho area = areaTrabalhoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Área não encontrada"));

        List<Lista> listas = listaService.listarPorArea(id);

        model.addAttribute("nome", usuario.getNome());
        model.addAttribute("email", usuario.getEmail());
        model.addAttribute("dataNascimento", usuario.getDataNascimento());

        model.addAttribute("areaId", id);
        model.addAttribute("areaName", name);
        model.addAttribute("area", area);
        model.addAttribute("listas", listas);

        return "menu";
    }

    @PostMapping("/areasTrabalho/criar")
    @ResponseBody
    public String criarArea(@RequestParam String nome, HttpServletRequest request) {

        String usuarioId = CookieService.getCookie(request, "usuarioId");
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

        area.getParticipacoes().add(participacao); // adiciona na coleção da área
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

        String usuarioId = CookieService.getCookie(request, "usuarioId");
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
        String usuarioIdString = CookieService.getCookie(request, "usuarioId");
        if (usuarioIdString == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long usuarioId = Long.parseLong(usuarioIdString);
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Long> ids = body.get("ids");
        for (Long id : ids) {
            AreaTrabalho area = areaTrabalhoRepository.findById(id).orElse(null);
            if (area != null){
                List<ParticipacaoArea> participacoes = participacaoAreaRepository.findByArea(area);
                ParticipacaoArea participacaoArea = participacoes.stream().filter(e -> e.getUsuario().getId() == usuarioId).findFirst().orElse(null);
                if (participacaoArea.getPermissao().equals(PermissaoArea.ADMIN)) {
                    areaTrabalhoRepository.delete(area);
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

    @PostMapping("/areasTrabalho/compartilhar")
    @ResponseBody
    public ResponseEntity<?> compartilharArea(@RequestBody Map<String, String> body, HttpServletRequest request) {

        String usuarioId = CookieService.getCookie(request, "usuarioId");
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Usuário não logado"));

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Usuário não encontrado"));

        String areaIdStr = body.get("areaId");
        String metodo = body.get("metodo");

        if (areaIdStr == null || metodo == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Dados incompletos"));
        }

        Long areaId = Long.parseLong(areaIdStr);
        AreaTrabalho area = areaTrabalhoRepository.findById(areaId).orElse(null);

        if (area == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Área não encontrada"));
        }

        // Cria participação do usuário nessa área
        
        //ParticipacaoArea participacao = new ParticipacaoArea();
        //participacao.setArea(area);
        //participacao.setUsuario(usuario);
        //participacao.setPermissao(PermissaoArea.OBSERVADOR); // ou ADMIN se quiser dar controle

        //area.getParticipacoes().add(participacao);
        //areaTrabalhoRepository.save(area);

        return ResponseEntity.ok(Map.of("success", true, "message", "Área compartilhada com sucesso!"));
    }

    @GetMapping("/areasTrabalho/gerar-link/{id}")
    @ResponseBody
    public ResponseEntity<?> gerarLink(@PathVariable Long id, HttpServletRequest request) {

        String usuarioId = CookieService.getCookie(request, "usuarioId");
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Usuário não logado"));

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Usuário não encontrado"));

        AreaTrabalho area = areaTrabalhoRepository.findById(id).orElse(null);
        if (area == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Área não encontrada"));

        // Gera token único para compartilhamento
        String token = java.util.UUID.randomUUID().toString().substring(0, 8);

        String link = "https://todaily.app/shared/" + token;

        return ResponseEntity.ok(Map.of("success", true, "link", link));
    }

    @GetMapping("/areasTrabalho/{id}/membros")
    @ResponseBody
    public ResponseEntity<?> listarMembros(@PathVariable Long id, HttpServletRequest request) {
        String usuarioId = CookieService.getCookie(request, "usuarioId");
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        AreaTrabalho area = areaTrabalhoRepository.findById(id).orElse(null);
        if (area == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("AREA_NOT_FOUND");

        // Verifica se o usuário tem participação na área
        boolean temAcesso = area.getParticipacoes().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(usuario.getId()));
        if (!temAcesso) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("NO_ACCESS");

        // Mapeia membros para JSON
        List<Map<String, Object>> membros = area.getParticipacoes().stream()
            .map(p -> {
                Map<String, Object> mapa = new HashMap<>();
                mapa.put("id", p.getUsuario().getId());
                mapa.put("nome", p.getUsuario().getNome());
                mapa.put("email", p.getUsuario().getEmail());
                mapa.put("permissao", p.getPermissao() == PermissaoArea.ADMIN ? "editar" : "visualizar");
                return mapa;
            })
            .collect(Collectors.toList());


        return ResponseEntity.ok(membros);
    }

    @GetMapping("/areasTrabalho/{id}/listas")
    @ResponseBody
    public ResponseEntity<?> listarListas(@PathVariable Long id, HttpServletRequest request) {
        // Verifica se o usuário está logado
        String usuarioId = CookieService.getCookie(request, "usuarioId");
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        // Verifica se a área existe
        AreaTrabalho area = areaTrabalhoRepository.findById(id).orElse(null);
        if (area == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("AREA_NOT_FOUND");

        // Verifica se o usuário tem acesso à área
        boolean temAcesso = area.getParticipacoes().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(usuario.getId()));
        if (!temAcesso) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("NO_ACCESS");

        // Obtém as listas da área
        List<Lista> listas = listaService.listarPorArea(id);

        // Mapeia as listas para JSON simples
        List<Map<String, Object>> listasJson = listas.stream()
                .map(l -> {
                    Map<String, Object> mapa = new HashMap<>();
                    mapa.put("id", l.getId());
                    mapa.put("nome", l.getNome());
                    mapa.put("descricao", l.getDescricao());
                    return mapa;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(listasJson);
    }

    @GetMapping("/areasTrabalho/{areaId}/tarefas")
    @ResponseBody
    public ResponseEntity<?> listarTarefasPorArea(@PathVariable Long areaId, HttpServletRequest request) {
        // Verifica se o usuário está logado
        String usuarioId = CookieService.getCookie(request, "usuarioId");
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        // Verifica se a área existe
        AreaTrabalho area = areaTrabalhoRepository.findById(areaId).orElse(null);
        if (area == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("AREA_NOT_FOUND");

        // Verifica se o usuário tem acesso à área
        boolean temAcesso = area.getParticipacoes().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(usuario.getId()));
        if (!temAcesso) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("NO_ACCESS");

        // Obtém as listas da área
        List<Lista> listas = listaService.listarPorArea(areaId);

        //List<Tarefa> tarefas = tarefaService.listarTarefasPorLista(listaId);

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

    @GetMapping("/user/tarefas")
    @ResponseBody
    public ResponseEntity<?> listarTarefasPorUser(HttpServletRequest request) {
        
        // Verifica se o usuário está logado
        String usuarioId = CookieService.getCookie(request, "usuarioId");

        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        List<AreaTrabalho> areas = areaTrabalhoRepository.findByDono(usuario);

        
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

    @GetMapping("/user/listas")
    @ResponseBody
    public ResponseEntity<?> listarListasPorUser(HttpServletRequest request) {
        
        // Verifica se o usuário está logado
        String usuarioId = CookieService.getCookie(request, "usuarioId");

        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        List<AreaTrabalho> areas = areaTrabalhoRepository.findByDono(usuario);
        
        List<Lista> todasListas = new ArrayList<>();
        for (AreaTrabalho area : areas) {
            todasListas.addAll(area.getListas());
        }

        // Mapeia as listas para JSON simples
        List<Map<String, Object>> listasJson = todasListas.stream()
                .map(l -> {
                    Map<String, Object> mapa = new HashMap<>();
                    mapa.put("id", l.getId());
                    mapa.put("nome", l.getNome());
                    mapa.put("descricao", l.getDescricao());
                    return mapa;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(listasJson);
    }

    @GetMapping("/user/membros")
    @ResponseBody
    public ResponseEntity<?> listarUserMembros(HttpServletRequest request) {
        String usuarioId = CookieService.getCookie(request, "usuarioId");
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGGED");

        Usuario usuario = usuarioRepository.findById(Long.parseLong(usuarioId)).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");

        List<AreaTrabalho> areas = areaTrabalhoRepository.findByDono(usuario);
        
        List<ParticipacaoArea> todosMembros = new ArrayList<>();
        for (AreaTrabalho area : areas) {
            todosMembros.addAll(area.getParticipacoes());
        }

        // Mapeia membros para JSON
        List<Map<String, Object>> membros = todosMembros.stream()
            .map(p -> {
                Map<String, Object> mapa = new HashMap<>();
                mapa.put("id", p.getUsuario().getId());
                mapa.put("nome", p.getUsuario().getNome());
                mapa.put("email", p.getUsuario().getEmail());
                mapa.put("permissao", p.getPermissao() == PermissaoArea.ADMIN ? "editar" : "visualizar");
                return mapa;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(membros);
    }
    @PostMapping("/remover-membro") // Certifique-se que a classe não tem @RequestMapping("/api")
    @ResponseBody
    @Transactional // Necessário para operações de delete funcionarem corretamente
    public ResponseEntity<?> removerMembro(@RequestBody Map<String, Object> map) {
        try {
            // 1. Validação de segurança na entrada de dados
            if (map.get("usuarioId") == null || map.get("areaId") == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "IDs não fornecidos."));
            }

            // 2. Conversão segura (trata tanto se vier como String quanto Integer/Long do JSON)
            Long idUsuario = Long.parseLong(map.get("usuarioId").toString());
            Long idArea = Long.parseLong(map.get("areaId").toString());

            Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
            AreaTrabalho area = areaTrabalhoRepository.findById(idArea).orElse(null);

            // 3. Parar a execução se não encontrar (Return explícito)
            if (area == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Área não encontrada."));
            }

            if (usuario == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Usuário não encontrado."));
            }

            // 4. Remoção Otimizada
            // Em vez de trazer TODOS e fazer loop, tentamos achar a participação exata
            // Se você tiver um método findByAreaAndUsuario no repository, use-o.
            // Caso contrário, mantive sua lógica de loop mas com break para otimizar.

            boolean removeu = false;
            List<ParticipacaoArea> todosMembros = participacaoAreaRepository.findByArea(area);

            for (ParticipacaoArea participacao : todosMembros) {
                // Comparação segura de Longs usa .equals
                if (participacao.getUsuario().getId().equals(idUsuario)) {
                    participacaoAreaRepository.delete(participacao);
                    removeu = true;
                    break; // Importante: para o loop assim que encontrar
                }
            }

            if (!removeu) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Este usuário não faz parte desta área."));
            }

            // Retorna sucesso
            return ResponseEntity.ok(Map.of("success", true, "message", "Membro removido com sucesso!"));

        } catch (Exception e) {
            // 5. Log do erro para você ver no console do servidor o que houve
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Erro interno: " + e.getMessage()));
        }
    }
}