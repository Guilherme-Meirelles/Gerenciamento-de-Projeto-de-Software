package com.example.demo.Controles;

import com.example.demo.Serviços.Idioma.Mensagens;

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

    @GetMapping("/recuperarSenhaEmail")
    public String abrirPaginaRecuperarSenha() {
        return "recuperarSenhaEmail";
    }

    @PostMapping("/recuperar-senha")
    public String recuperarSenha(@RequestParam("email") String email, Model model, HttpServletResponse response) {
        try {
            Usuario usuario = this.ur.findByEmail(email);

            if (usuario == null) {
                model.addAttribute("erro", Mensagens.get("msg.recuperar.semConta"));
                return "recuperarSenhaEmail";
            }

            String tokenString = UUID.randomUUID().toString();

            Token token = new Token();
            token.setToken(tokenString);
            token.setEmail(usuario.getEmail());
            token.setExpiraEm(LocalDateTime.now().plusMinutes(10));
            token.setUsado(false);

            tokenRepository.save(token);

            emailService2.enviarEmailRecuperacaoSenha(usuario.getEmail(), usuario.getNome(), tokenString);

            model.addAttribute("mensagem", Mensagens.get("msg.recuperar.enviado"));

        } catch (Exception e) {
            model.addAttribute("erro", Mensagens.get("msg.recuperar.falhaEnvio"));
        }

        return "recuperarSenhaEmail";
    }

    @GetMapping("/redefinicaoSenha")
    public String redefinicaoSenha(@RequestParam("token") String token, Model model) {

        Token t = tokenRepository.findByToken(token);

        if (t == null || t.isUsado() || t.getExpiraEm().isBefore(LocalDateTime.now())) {
            model.addAttribute("erro", Mensagens.get("msg.recuperar.linkExpirado"));
            return "login";
        }

        model.addAttribute("token", token);
        return "redefinicaoSenha";
    }

    @PostMapping("/redefinirSenha")
    public String redefinirSenha(@RequestParam("senha") String senha,
                                 @RequestParam("token") String token,
                                 Model model, HttpServletRequest request) {

        try {
            Token t = tokenRepository.findByToken(token);

            if (t == null || t.isUsado() || t.getExpiraEm().isBefore(LocalDateTime.now())) {
                model.addAttribute("erro", Mensagens.get("msg.recuperar.linkInvalido"));
                return "redefinicaoSenha";
            }

            if (senha == null || senha.length() < 8) {
                model.addAttribute("token", token);
                model.addAttribute("erro", Mensagens.get("msg.senha.curtaPonto"));
                return "redefinicaoSenha";
            }
            if (LoginCadastroControle.senhaLonga(senha)) {
                model.addAttribute("token", token);
                model.addAttribute("erro", Mensagens.get("msg.senha.longa", Usuario.SENHA_MAX));
                return "redefinicaoSenha";
            }
            if (!LoginCadastroControle.senhaForte(senha)) {
                model.addAttribute("token", token);
                model.addAttribute("erro", Mensagens.get("msg.senha.fraca"));
                return "redefinicaoSenha";
            }

            Usuario usuario = this.ur.findByEmail(t.getEmail());
            usuario.setSenha(passwordEncoder.encode(senha));
            ur.save(usuario);

            t.setUsado(true);
            tokenRepository.save(t);

            SessaoUtil.encerrar(request);

            model.addAttribute("mensagem", Mensagens.get("msg.recuperar.sucesso"));
            return "login";

        } catch (Exception e) {
            model.addAttribute("erro", Mensagens.get("msg.recuperar.erro", e.getMessage()));
            return "redefinicaoSenha";
        }
    }
}
