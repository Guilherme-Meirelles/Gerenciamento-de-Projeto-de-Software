package com.example.demo.Controles;

import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/menu")
    public String Menu(Model model, HttpServletRequest request) {
        String usuarioId = SessaoUtil.getUsuarioId(request);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        Usuario usuario = ur.findUsuarioById(Long.parseLong(usuarioId));
        if (usuario == null) {
            return "redirect:/login";
        }

        String dataNascimento = usuario.getDataNascimento();
        String ano = dataNascimento.substring(0, 4);
        String mes = dataNascimento.substring(5, 7);
        String dia = dataNascimento.substring(8, 10);
        dataNascimento = dia + "/" + mes + "/" + ano;
        model.addAttribute("nome", usuario.getNome());
        model.addAttribute("email", usuario.getEmail());
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
            Model model) {

        String usuarioId = SessaoUtil.getUsuarioId(request);

        if (usuarioId == null) {
            model.addAttribute("erro", "Nenhum usuário logado.");
            return "redirect:/login";
        }

        Usuario usuario = this.ur.findUsuarioById(Long.parseLong(usuarioId));
        if (usuario == null) {
            return "redirect:/login";
        }

        if (passwordEncoder.matches(usuarioSenha.getSenha(), usuario.getSenha())) {
            this.ur.delete(usuario);
            SessaoUtil.encerrar(request);

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
    }

    @PostMapping("/editarConta")
    public String edicaodeConta(
            @ModelAttribute Usuario usuarioSenha,
            HttpServletRequest request,
            Model model) {

        String usuarioId = SessaoUtil.getUsuarioId(request);

        if (usuarioId == null) {
            model.addAttribute("erro", "Nenhum usuário logado.");
            return "redirect:/login";
        }

        Usuario usuario = this.ur.findUsuarioById(Long.parseLong(usuarioId));
        if (usuario == null) {
            return "redirect:/login";
        }

        if (passwordEncoder.matches(usuarioSenha.getSenha(), usuario.getSenha())) {
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
    }

}
