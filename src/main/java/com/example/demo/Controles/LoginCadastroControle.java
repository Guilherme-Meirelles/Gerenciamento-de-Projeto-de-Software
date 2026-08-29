package com.example.demo.Controles;

import com.example.demo.Entidades.Token;
import com.example.demo.Entidades.Usuario;
import com.example.demo.ConsultasBD.TokenRepository;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.EnvioDeEmail.EmailService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

@Controller
public class LoginCadastroControle {

    @Autowired
    private UsuarioRepository ur;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/cadastro")
    public String cadastro() {
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastroUsuario(@ModelAttribute Usuario usuario, BindingResult result, Model model) {

        if (result.hasErrors()) {
            model.addAttribute("mensagem", "Erro ao cadastrar!");
            return "cadastro";
        }

        if (usuario.getSenha().length() < 8) {
            model.addAttribute("mensagem", "A senha deve ter pelo menos 8 caracteres!");
            return "cadastro";
        }

        if (Long.parseLong(usuario.getDataNascimento().substring(0, 4)) > Year.now().getValue()) {
            model.addAttribute("mensagem", "Ano inserido inválido");
            return "cadastro";

        }

        if (ur.findByEmail(usuario.getEmail()) != null) {
            model.addAttribute("mensagem", "Este e-mail já está cadastrado!");
            return "cadastro";
        }

        usuario.setEmailVerificado(false);
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        ur.save(usuario); // salva no banco

        try {
            String tokenString = UUID.randomUUID().toString();

            Token token = new Token();
            token.setToken(tokenString);
            token.setEmail(usuario.getEmail());
            token.setExpiraEm(LocalDateTime.now().plusMinutes(30));
            token.setUsado(false);
            tokenRepository.save(token);

            emailService.enviarEmailVerificacao(usuario.getEmail(), usuario.getNome(), tokenString);

            model.addAttribute("mensagem", "Cadastro realizado! Verifique seu e-mail para confirmar a conta.");
        } catch (Exception e) {
            model.addAttribute("mensagem", "Cadastro realizado, mas houve um erro ao enviar o e-mail de confirmação.");
        }

        return "login"; // redireciona para tela de login
    }

    // Página acessada via link do email de confirmação de cadastro
    @GetMapping("/verificarEmail")
    public String verificarEmail(@RequestParam("token") String token, Model model) {

        Token t = tokenRepository.findByToken(token);

        if (t == null || t.isUsado() || t.getExpiraEm().isBefore(LocalDateTime.now())) {
            model.addAttribute("erro", "Link de confirmação expirado ou inválido");
            return "login";
        }

        Usuario usuario = ur.findByEmail(t.getEmail());
        if (usuario == null) {
            model.addAttribute("erro", "Usuário não encontrado");
            return "login";
        }

        usuario.setEmailVerificado(true);
        ur.save(usuario);

        t.setUsado(true);
        tokenRepository.save(t);

        model.addAttribute("mensagem", "E-mail confirmado com sucesso! Faça login para continuar.");
        return "login";
    }

    @PostMapping("/login")
    public String loginUsuario(Usuario usuario, Model model, HttpServletRequest request) {
        Usuario usuarioLogado = this.ur.findByEmail(usuario.getEmail());
        boolean senhaValida = usuarioLogado != null
                && usuarioLogado.getSenha() != null
                && usuarioLogado.getSenha().startsWith("$2")
                && passwordEncoder.matches(usuario.getSenha(), usuarioLogado.getSenha());

        if (senhaValida) {
            if (!usuarioLogado.isEmailVerificado()) {
                model.addAttribute("erro", "Confirme seu e-mail antes de fazer login. Verifique sua caixa de entrada.");
                return "login";
            }

            SessaoUtil.autenticar(request, usuarioLogado.getId());
            return "redirect:/menu"; // ✅ Agora redireciona corretamente
        }

        model.addAttribute("erro", "Usuário Inválido");
        return "login";

    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        SessaoUtil.encerrar(request);
        return "redirect:/login"; // volta para a página de login
    }
}


