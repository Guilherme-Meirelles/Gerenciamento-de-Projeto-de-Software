package com.example.demo.Controles;

import com.example.demo.ConsultasBD.*;
import com.example.demo.Entidades.*;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.EnvioDeEmail.EmailService;
import com.example.demo.Serviços.PermissaoAreaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.example.demo.Entidades.PermissaoArea.*;

@Controller
public class CompartilharAreadeTrabalho {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AreaTrabalhoRepository areaTrabalhoRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private ParticipacaoAreaRepository participacaoAreaRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PermissaoAreaService permissaoAreaService;




    @PostMapping("/notificacaoArea")
    @ResponseBody // Garante que o retorno é dado puro, não uma página HTML
    public ResponseEntity<?> enviarNotificacao(@RequestBody Map<String, Object> dados, HttpServletRequest request) {

        try {
            String email = (String) dados.get("email");

            // O ID pode vir como Integer ou String do JSON, converta com segurança
            String idAreaStr = String.valueOf(dados.get("id_area"));
            Long idArea = Long.parseLong(idAreaStr);


            String remetenteId = SessaoUtil.getUsuarioId(request);
            if (remetenteId == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Não autenticado"));
            }
            Usuario remetente = usuarioRepository.findById(Long.parseLong(remetenteId)).orElse(null);
            if (remetente == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Não autenticado"));
            }

            // Nível concedido pelo convite (Observador ou Editor); sem valor, assume Editor.
            Object permissaoBruta = dados.get("permissao");
            PermissaoArea nivel = permissaoBruta == null ? EDITOR : PermissaoArea.doConvite(String.valueOf(permissaoBruta));
            if (nivel == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Nível de permissão inválido. Use OBSERVADOR ou EDITOR."));
            }

            AreaTrabalho area = areaTrabalhoRepository.findById(idArea).get();
            Usuario destinatario = usuarioRepository.findByEmail(email);
            if (area == null) {

                System.out.println("Erro 2");
            } else if (destinatario == null) {

                System.out.println("Erro 3");
            } else {

                // Só Editor/Admin da área pode convidar gente pra ela; Observador é read-only.
                if (!permissaoAreaService.podeEditar(remetente.getId(), idArea)) {
                    return ResponseEntity.status(403).body(Map.of("success", false, "message", "Você não tem permissão para convidar pessoas nesta área"));
                }

                String tokenString = UUID.randomUUID().toString();

                Token token = new Token();
                token.setToken(tokenString);
                token.setEmail(destinatario.getEmail());
                token.setAreaId(idArea);
                token.setPermissao(nivel);
                token.setExpiraEm(LocalDateTime.now().plusMinutes(1440)); //1 dia
                token.setUsado(false);

                tokenRepository.save(token);

                String nomeUsuario = remetente.getNome();

                emailService.enviarEmailCompartilharAreaTrabalho(destinatario, nomeUsuario, area, tokenString, nivel);

                System.out.println("Acerto");
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Convite enviado!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Erro ao processar."));
        }
    }

    @GetMapping("/compartilhamentoArea")
    public String compartilhamentoArea(@RequestParam("token") String tokenString, @RequestParam("destID") String destString, @RequestParam("areaID") String areaString, Model model) {

        try{

            Long destID = Long.parseLong(destString);
            Long areaID = Long.parseLong(areaString);

            Token token = tokenRepository.findByToken(tokenString);
            Usuario destinatario = usuarioRepository.findById(destID).get();
            AreaTrabalho area = areaTrabalhoRepository.findById(areaID).get();

            if (token == null || token.isUsado() || token.getExpiraEm().isBefore(LocalDateTime.now())) {
                model.addAttribute("erro", "Link expirado ou inválido");
                return "login";
            }
            else if (destinatario == null || area == null) {
                model.addAttribute("erro", "Destinatário e área não encontrados");
                return "login";
            }
            else if (!areaID.equals(token.getAreaId()) || !destinatario.getEmail().equalsIgnoreCase(token.getEmail())) {
                // Token válido, mas pra outra área/destinatário — impede reaproveitar um convite legítimo trocando os IDs na URL
                model.addAttribute("erro", "Link expirado ou inválido");
                return "login";
            }
            else{
                // Quem já participa da área mantém o nível que tem (sem duplicar a participação).
                if (!participacaoAreaRepository.existsByUsuarioIdAndAreaId(destinatario.getId(), areaID)) {
                    // Aplica o nível escolhido no convite; tokens antigos (sem nível) viram Editor.
                    PermissaoArea nivelConvite = token.getPermissao() == null || token.getPermissao() == ADMIN
                            ? EDITOR : token.getPermissao();
                    participacaoAreaRepository.save(new ParticipacaoArea(destinatario, area, nivelConvite));
                }
                token.setUsado(true);
                tokenRepository.save(token);
                model.addAttribute("mensagem", "Área compartilhada com sucesso");
                return "login";
            }




        } catch (Exception e){

            System.out.println("Erro: " + e.getMessage());
            model.addAttribute("erro", "Erro ao processar");
            return "login";
        }

    }
}