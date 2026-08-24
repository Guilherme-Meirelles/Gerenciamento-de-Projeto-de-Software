package com.example.demo.Controles;

import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.CookieService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.Year;

@Controller
public class edicaoUsuario {

    @Autowired
    private UsuarioRepository ur;

    @GetMapping("/edicaoUsuario")
    public String carregarEdicaoUsuario(HttpServletRequest request, Model model) {

        String usuarioId = CookieService.getCookie(request, "usuarioId");

        if (usuarioId != null) {
            Usuario usuario = ur.findUsuarioById(Long.parseLong(usuarioId));

            if (usuario != null) {
                model.addAttribute("usuario", usuario);
                return "edicaoUsuario"; // nome do arquivo HTML (edicaoUsuario.html)
            }
        }

        // Caso não esteja logado, redireciona para login
        return "redirect:/login";
    }

    @PostMapping("/edicaoUsuario")
    public String edicaoUsuario(@ModelAttribute Usuario usuarioEditado, HttpServletRequest request, BindingResult result, Model model) {

        if (result.hasErrors()) {
            model.addAttribute("mensagem", "Erro na edição de usuário!");
            return "edicaoUsuario";

        } else if (Long.parseLong(usuarioEditado.getDataNascimento().substring(0, 4)) > Year.now().getValue()) {
            model.addAttribute("mensagem", "Ano inserido inválido");
            return "cadastro";
        }

        else if (usuarioEditado.getSenha().length() < 8) {
            model.addAttribute("mensagem", "A senha deve ter pelo menos 8 caracteres!");
            return "edicaoUsuario";
        } else {

            String usuarioId = CookieService.getCookie(request, "usuarioId");

            if (usuarioId != null) {
                Long id = Long.parseLong(usuarioId);

                Usuario usuarioExistente = ur.findUsuarioById(id);

                if (usuarioExistente != null) {
                    usuarioExistente.setNome(usuarioEditado.getNome());
                    usuarioExistente.setEmail(usuarioEditado.getEmail());
                    usuarioExistente.setDataNascimento(usuarioEditado.getDataNascimento());

                    if (usuarioEditado.getSenha() != null && !usuarioEditado.getSenha().isBlank()) {
                        usuarioExistente.setSenha(usuarioEditado.getSenha());
                    }

                    ur.save(usuarioExistente);

                    model.addAttribute("mensagem", "Dados atualizados com sucesso!");
                    model.addAttribute("usuario", usuarioExistente);
                    return "login";
                }
            }

            model.addAttribute("erro", "Usuário não encontrado ou não logado.");
            return "redirect:/login";
        }
    }

}