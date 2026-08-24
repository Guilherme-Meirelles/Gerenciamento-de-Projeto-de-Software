package com.example.demo.Controles;

import com.example.demo.Entidades.Usuario;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Serviços.CookieService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Year;

@Controller
public class LoginCadastroControle {

    @Autowired
    private UsuarioRepository ur;

    @GetMapping("/login")
    public String login(HttpServletResponse response) {
        /*
        CookieService.deleteCookie(response, "usuarioId");
        CookieService.deleteCookie(response, "nomeUsuario");
        CookieService.deleteCookie(response, "emailUsuario");
        CookieService.deleteCookie(response, "dataNascimento");
        */
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
            return "redirect:/cadastro";
        }

        if (usuario.getSenha().length() < 8) {
            model.addAttribute("mensagem", "A senha deve ter pelo menos 8 caracteres!");
            return "cadastro";
        }

        if (Long.parseLong(usuario.getDataNascimento().substring(0, 4)) > Year.now().getValue()) {
            model.addAttribute("mensagem", "Ano inserido inválido");
            return "cadastro";

        }


        ur.save(usuario); // ✅ salva no banco
        model.addAttribute("mensagem", "Usuário cadastrado com sucesso!");
        return "login"; // redireciona para tela de login
    }

    @PostMapping("/login")
    public String loginUsuario(Usuario usuario, Model model, HttpServletResponse response) {
        Usuario usuarioLogado = this.ur.findByEmailAndSenha(usuario.getEmail(), usuario.getSenha());

        if (usuarioLogado != null) {
            CookieService.setCookie(response, "usuarioId", String.valueOf(usuarioLogado.getId()), 10000);
            CookieService.setCookie(response, "nomeUsuario", usuarioLogado.getNome(), 10000);
            CookieService.setCookie(response, "emailUsuario", usuarioLogado.getEmail(), 10000);
            CookieService.setCookie(response, "dataNascimento", usuarioLogado.getDataNascimento(), 10000);
            return "redirect:/menu"; // ✅ Agora redireciona corretamente
        }

        model.addAttribute("erro", "Usuário Inválido");
        return "login";

    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {

        // Lista de todos os cookies que você usa
        String[] cookies = {
            "usuarioId",
            "nomeUsuario",
            "emailUsuario",
            "dataNascimento"
        };

        // Apaga cada cookie
        for (String nome : cookies) {
            Cookie cookie = new Cookie(nome, "");
            cookie.setMaxAge(0); // apaga
            cookie.setPath("/"); // importante!!
            response.addCookie(cookie);
        }

        return "redirect:/login"; // volta para a página de login
    }
}


