package com.example.demo.Controles;

import com.example.demo.Serviços.Idioma.Idioma;
import com.example.demo.Serviços.Idioma.IdiomaLocaleResolver;
import com.example.demo.Serviços.Idioma.Mensagens;

import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;
import com.example.demo.Entidades.Token;
import com.example.demo.Entidades.Usuario;
import com.example.demo.ConsultasBD.TokenRepository;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.ConviteAreaService;
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

    @Autowired
    private ConviteAreaService conviteAreaService;

    private static final String ATRIBUTO_CONVITE_PENDENTE = "conviteAreaPendente";

    // Maiúscula, minúscula, número e caractere especial (o tamanho mínimo é checado à parte)
    static boolean senhaForte(String senha) {
        return senha.matches("(?s)(?=.*\\p{Lu})(?=.*\\p{Ll})(?=.*\\d)(?=.*[^\\p{L}\\p{N}\\s]).*");
    }

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
            model.addAttribute("mensagem", Mensagens.get("msg.cadastro.erro"));
            return "cadastro";
        }

        if (usuario.getSenha().length() < 8) {
            model.addAttribute("mensagem", Mensagens.get("msg.senha.curta"));
            return "cadastro";
        }

        if (!senhaForte(usuario.getSenha())) {
            model.addAttribute("mensagem", Mensagens.get("msg.senha.fraca"));
            return "cadastro";
        }

        if (Long.parseLong(usuario.getDataNascimento().substring(0, 4)) > Year.now().getValue()) {
            model.addAttribute("mensagem", Mensagens.get("msg.ano.invalido"));
            return "cadastro";

        }

        if (ur.findByEmail(usuario.getEmail()) != null) {
            model.addAttribute("mensagem", Mensagens.get("msg.email.jaCadastrado"));
            return "cadastro";
        }

        usuario.setEmailVerificado(false);
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        ur.save(usuario);

        try {
            String tokenString = UUID.randomUUID().toString();

            Token token = new Token();
            token.setToken(tokenString);
            token.setEmail(usuario.getEmail());
            token.setExpiraEm(LocalDateTime.now().plusMinutes(30));
            token.setUsado(false);
            tokenRepository.save(token);

            emailService.enviarEmailVerificacao(usuario.getEmail(), usuario.getNome(), tokenString);

            model.addAttribute("mensagem", Mensagens.get("msg.cadastro.sucesso"));
        } catch (Exception e) {
            model.addAttribute("mensagem", Mensagens.get("msg.cadastro.erroEmail"));
        }

        return "login";
    }

    @GetMapping("/verificarEmail")
    public String verificarEmail(@RequestParam("token") String token, Model model) {

        Token t = tokenRepository.findByToken(token);

        if (t == null || t.isUsado() || t.getExpiraEm().isBefore(LocalDateTime.now())) {
            model.addAttribute("erro", Mensagens.get("msg.link.confirmacaoInvalido"));
            return "login";
        }

        Usuario usuario = ur.findByEmail(t.getEmail());
        if (usuario == null) {
            model.addAttribute("erro", Mensagens.get("msg.usuario.naoEncontrado"));
            return "login";
        }

        usuario.setEmailVerificado(true);
        ur.save(usuario);

        t.setUsado(true);
        tokenRepository.save(t);

        model.addAttribute("mensagem", Mensagens.get("msg.email.confirmado"));
        return "login";
    }

    // Reenvia o e-mail de confirmação sem pedir o email de novo (usado a partir da tela de login,
    // depois de uma tentativa de login que falhou por falta de confirmação)
    @PostMapping("/reenviarVerificacao")
    public String reenviarVerificacao(@RequestParam("email") String email, Model model) {
        try {
            Usuario usuario = ur.findByEmail(email);

            if (usuario == null) {
                model.addAttribute("erro", Mensagens.get("msg.email.naoCadastrado"));
                return "login";
            }

            if (usuario.isEmailVerificado()) {
                model.addAttribute("mensagem", Mensagens.get("msg.email.jaVerificado"));
                return "login";
            }

            String tokenString = UUID.randomUUID().toString();

            Token token = new Token();
            token.setToken(tokenString);
            token.setEmail(usuario.getEmail());
            token.setExpiraEm(LocalDateTime.now().plusMinutes(30));
            token.setUsado(false);
            tokenRepository.save(token);

            emailService.enviarEmailVerificacao(usuario.getEmail(), usuario.getNome(), tokenString);

            model.addAttribute("mensagem", Mensagens.get("msg.email.reenviado"));

        } catch (Exception e) {
            model.addAttribute("erro", Mensagens.get("msg.email.erroEnvio", e.getMessage()));
        }

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
                model.addAttribute("erro", Mensagens.get("msg.login.confirmeEmail"));
                model.addAttribute("emailNaoVerificado", usuarioLogado.getEmail());
                return "login";
            }

            // O idioma que a pessoa está usando neste navegador (escolhido na tela de login ou lembrado do último uso)
            // vale a partir do login e é guardado no perfil. Só em um navegador sem escolha nenhuma vale o do perfil.
            Idioma escolhido = IdiomaLocaleResolver.sessao(request);
            if (escolhido == null) escolhido = IdiomaLocaleResolver.cookie(request);
            SessaoUtil.autenticar(request, usuarioLogado.getId());
            Idioma doPerfil = Idioma.de(usuarioLogado.getIdioma());
            Idioma valido = escolhido != null ? escolhido : (doPerfil != null ? doPerfil : Idioma.PADRAO);
            if (doPerfil != valido) {
                usuarioLogado.setIdioma(valido.codigo());
                ur.save(usuarioLogado);
            }
            request.getSession().setAttribute(IdiomaLocaleResolver.ATRIBUTO_SESSAO, valido.codigo());

            // Se o login veio de um link de convite de área de trabalho, aceita agora
            // (guardado na sessão por AreaTrabalhoController.entrarPorLink).
            Object convitePendente = request.getSession().getAttribute(ATRIBUTO_CONVITE_PENDENTE);
            if (convitePendente != null) {
                request.getSession().removeAttribute(ATRIBUTO_CONVITE_PENDENTE);
                ConviteAreaService.Resultado resultado = conviteAreaService.aceitarLinkGenerico((String) convitePendente, usuarioLogado);
                if (resultado.sucesso()) {
                    return "redirect:/areasTrabalho/" + resultado.area().getId() + "/"
                            + UriUtils.encodePathSegment(resultado.area().getNome(), StandardCharsets.UTF_8);
                }
            }

            return "redirect:/menu";
        }

        model.addAttribute("erro", Mensagens.get("msg.login.incorreto"));
        model.addAttribute("credenciaisInvalidas", true);
        return "login";

    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        SessaoUtil.encerrar(request);
        return "redirect:/login";
    }
}


