package com.example.demo.Controles;

import com.example.demo.ConsultasBD.*;
import com.example.demo.Entidades.*;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.EnvioDeEmail.EmailService;
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




    /*
    @PostMapping("/notificacaoArea")
    @ResponseBody
    public void notificarCompartilhamentoArea(@ModelAttribute String email,
                                                @ModelAttribute("id_area") String id_area, Model model) {

        if (id_area.equals("") || id_area == null) {
            model.addAttribute("erro", "Id da area não capturado");
            System.out.println("Erro 1");
        } else {
            Long idArea = Long.parseLong(id_area);
            AreaTrabalho area = areaTrabalhoRepository.findById(idArea).get();
            Usuario usuario = usuarioRepository.findByEmail(email);
            if (area == null) {
                model.addAttribute("erro", "Area de Trabalho Selecionada não existe");
                System.out.println("Erro 2");
            } else if (usuario == null) {
                model.addAttribute("erro", "Não foi encontrado um usuário com este email no Banco de Dados");
                System.out.println("Erro 3");
            } else {
                AreaCompartilhamento areaCompartilhamento = new AreaCompartilhamento(idArea, area.getNome(), email, usuario.getNome());
                areaCompRepository.save(areaCompartilhamento);
                model.addAttribute("mensagem", "Notificação enviada com sucesso");
                System.out.println("Acerto");
            }
        }
    }
    */
    // Importe: org.springframework.http.ResponseEntity;
// Importe: org.springframework.web.bind.annotation.RequestBody;
// Importe: java.util.Map;

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

            AreaTrabalho area = areaTrabalhoRepository.findById(idArea).get();
            Usuario destinatario = usuarioRepository.findByEmail(email);
            if (area == null) {

                System.out.println("Erro 2");
            } else if (destinatario == null) {

                System.out.println("Erro 3");
            } else {

                // Só quem já participa da área pode convidar gente pra ela
                boolean remetenteParticipa = area.getParticipacoes().stream()
                        .anyMatch(p -> p.getUsuario().getId().equals(remetente.getId()));
                if (!remetenteParticipa) {
                    return ResponseEntity.status(403).body(Map.of("success", false, "message", "Você não participa desta área de trabalho"));
                }

                // -------- GERAR TOKEN --------
                String tokenString = UUID.randomUUID().toString();

                Token token = new Token();
                token.setToken(tokenString);
                token.setEmail(destinatario.getEmail());
                token.setAreaId(idArea);
                token.setExpiraEm(LocalDateTime.now().plusMinutes(1440)); //1 dia
                token.setUsado(false);

                tokenRepository.save(token);

                String nomeUsuario = remetente.getNome();

                // Enviar email com token
                emailService.enviarEmailCompartilharAreaTrabalho(destinatario, nomeUsuario, area ,tokenString);

                System.out.println("Acerto");
            }


            // --- SUA LÓGICA DE SALVAR NO BANCO AQUI ---
            // notificacaoService.criar(email, idArea);

            // Retorna sucesso para o JavaScript ler
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
                model.addAttribute("erro", "Destinatario e area não encontrados");
                return "login";
            }
            else if (!areaID.equals(token.getAreaId()) || !destinatario.getEmail().equalsIgnoreCase(token.getEmail())) {
                // Token válido, mas pra outra área/destinatário — impede reaproveitar um convite legítimo trocando os IDs na URL
                model.addAttribute("erro", "Link expirado ou inválido");
                return "login";
            }
            else{
                ParticipacaoArea participacaoArea = new ParticipacaoArea(destinatario, area, EDITOR);
                participacaoAreaRepository.save(participacaoArea);
                token.setUsado(true);
                tokenRepository.save(token);
                model.addAttribute("mensagem", "Area compartilhada com sucesso");
                return "login";
            }




        } catch (Exception e){

            System.out.println("Erro: " + e.getMessage());
            model.addAttribute("erro", "Erro ao processar");
            return "login";
        }

    }
}