package com.example.demo.Controles;

import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.CookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
public class MenuController {

    @Autowired
    private UsuarioRepository ur;

    @GetMapping("/menu")
    public String Menu(Model model, HttpServletRequest request) {
        String nome = CookieService.getCookie(request, "nomeUsuario");
        String email = CookieService.getCookie(request, "emailUsuario");
        String dataNascimento = CookieService.getCookie(request, "dataNascimento");

        // Segurança: se não houver cookies, redireciona
        if (nome == null || email == null) {
            return "redirect:/login";
        }
        String ano = dataNascimento.substring(0, 4);
        String mes = dataNascimento.substring(5, 7);
        String dia = dataNascimento.substring(8, 10);
        dataNascimento = dia + "/" + mes + "/" + ano;
        model.addAttribute("nome", nome);
        model.addAttribute("email", email);
        model.addAttribute("dataNascimento", dataNascimento);
        model.addAttribute("isMenu", true);

        model.addAttribute("areaId", "");
        model.addAttribute("areaName", "");
        model.addAttribute("area", "");
        model.addAttribute("listas", "");

        return "menu"; // Retorna a view, não redirect
    }

    @GetMapping("/sidebar")
    public String sidebar() {

        return "sidebar";
    }

    @PostMapping("/removerConta")
    public String exclusaoDeConta(
            @ModelAttribute Usuario usuarioSenha,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        String usuarioId = CookieService.getCookie(request, "usuarioId");

        if (usuarioId != null) {
            Usuario usuario = this.ur.findUsuarioById(Long.parseLong(usuarioId));

            if (usuario != null && usuarioSenha.getSenha().equals(usuario.getSenha())) {
                this.ur.delete(usuario);

                CookieService.deleteCookie(response, "usuarioId");
                CookieService.deleteCookie(response, "nomeUsuario");
                CookieService.deleteCookie(response, "emailUsuario");
                CookieService.deleteCookie(response, "dataNascimento");

                model.addAttribute("remocao", "Usuário removido com sucesso!");
                return "login";
            } else {
                model.addAttribute("erro", "Senha incorreta. Tente novamente.");
                model.addAttribute("abrirModal", "verificacao");
                model.addAttribute("nome", usuario.getNome());
                model.addAttribute("email", usuario.getEmail());
                model.addAttribute("dataNascimento", usuario.getDataNascimento());
                model.addAttribute("isMenu", true);

                model.addAttribute("areaId", "");
                model.addAttribute("areaName", "");
                model.addAttribute("area", "");
                model.addAttribute("listas", "");

                return "menu"; // sem redirect
            }
        } else {
            model.addAttribute("erro", "Nenhum usuário logado.");
            return "redirect:/login";
        }
    }

    @PostMapping("/editarConta")
    public String edicaodeConta(
            @ModelAttribute Usuario usuarioSenha,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        String usuarioId = CookieService.getCookie(request, "usuarioId");

        if (usuarioId != null) {
            Usuario usuario = this.ur.findUsuarioById(Long.parseLong(usuarioId));

            if (usuario != null && usuarioSenha.getSenha().equals(usuario.getSenha())) {

                return "redirect:/edicaoUsuario";
            } else {
                model.addAttribute("erro", "Senha incorreta. Tente novamente.");
                model.addAttribute("abrirModal", "verificacao");

                model.addAttribute("nome", usuario.getNome());
                model.addAttribute("email", usuario.getEmail());
                model.addAttribute("dataNascimento", usuario.getDataNascimento());
                model.addAttribute("isMenu", true);
                
                model.addAttribute("areaId", "");
                model.addAttribute("areaName", "");
                model.addAttribute("area", "");
                model.addAttribute("listas", "");
                
                return "menu"; // sem redirect
            }
        } else {
            model.addAttribute("erro", "Nenhum usuário logado.");
            return "redirect:/login";
        }
    }

}
