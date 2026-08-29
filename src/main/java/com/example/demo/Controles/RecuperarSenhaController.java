package com.example.demo.Controles;

import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Token;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.ConsultasBD.TokenRepository;
import com.example.demo.Serviços.EnvioDeEmail.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class RecuperarSenhaController {

    @Autowired
    private EmailService emailService2;

    @Autowired
    private UsuarioRepository ur;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Abre página que solicita o email
    @GetMapping("/recuperarSenhaEmail")
    public String abrirPaginaRecuperarSenha() {
        return "recuperarSenhaEmail";
    }

    // 🔥 Quando clica em "CONFIRMAR" e envia o email
    @PostMapping("/recuperar-senha")
    public String recuperarSenha(@RequestParam("email") String email, Model model, HttpServletResponse response) {
        try {
            Usuario usuario = this.ur.findByEmail(email);

            if (usuario == null) {
                model.addAttribute("erro", "Este email não está cadastrado");
                return "recuperarSenhaEmail";
            }

            // -------- GERAR TOKEN --------
            String tokenString = UUID.randomUUID().toString();

            Token token = new Token();
            token.setToken(tokenString);
            token.setEmail(usuario.getEmail());
            token.setExpiraEm(LocalDateTime.now().plusMinutes(10)); // 🔥 expira em 10 minutos
            token.setUsado(false);

            tokenRepository.save(token);

            // Enviar email com token
            emailService2.enviarEmailRecuperacaoSenha(usuario.getEmail(), usuario.getNome(), tokenString);

            model.addAttribute("mensagem", "Email enviado com sucesso!");

        } catch (Exception e) {
            model.addAttribute("erro", "Erro ao enviar email! \n" + e.getMessage());
        }

        return "recuperarSenhaEmail";
    }

    // 🔥 Página acessada via link do email
    @GetMapping("/redefinicaoSenha")
    public String redefinicaoSenha(@RequestParam("token") String token, Model model) {

        Token t = tokenRepository.findByToken(token);

        if (t == null || t.isUsado() || t.getExpiraEm().isBefore(LocalDateTime.now())) {
            model.addAttribute("erro", "Link de recuperação de senha expirado");
            return "login";
        }

        model.addAttribute("token", token);
        return "redefinicaoSenha";
    }

    // 🔥 Quando o usuário envia a nova senha
    @PostMapping("/redefinirSenha")
    public String redefinirSenha(@RequestParam("senha") String senha,
                                 @RequestParam("token") String token,
                                 Model model, HttpServletRequest request) {

        try {
            Token t = tokenRepository.findByToken(token);

            if (t == null || t.isUsado() || t.getExpiraEm().isBefore(LocalDateTime.now())) {
                model.addAttribute("erro", "Link expirado ou inválido");
                return "redefinicaoSenha";
            }

            Usuario usuario = this.ur.findByEmail(t.getEmail());
            usuario.setSenha(passwordEncoder.encode(senha));
            ur.save(usuario);

            // invalidar token
            t.setUsado(true);
            tokenRepository.save(t);

            SessaoUtil.encerrar(request);

            model.addAttribute("mensagem", "Senha redefinida com sucesso!");
            return "login";

        } catch (Exception e) {
            model.addAttribute("erro", "Erro ao redefinir senha! \n" + e.getMessage());
            return "redefinicaoSenha";
        }
    }
}
