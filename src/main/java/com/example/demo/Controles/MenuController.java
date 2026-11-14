package com.example.demo.Controles;

import com.example.demo.Serviços.Idioma.Mensagens;

import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.UsuarioService;
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
    private UsuarioService usuarioService;

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

        model.addAttribute("nome", usuario.getNome());
        model.addAttribute("email", usuario.getEmail());
        model.addAttribute("dataNascimento", dataParaExibicao(usuario.getDataNascimento()));
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
            model.addAttribute("erro", Mensagens.get("msg.menu.semUsuario"));
            return "redirect:/login";
        }

        Usuario usuario = this.ur.findUsuarioById(Long.parseLong(usuarioId));
        if (usuario == null) {
            return "redirect:/login";
        }

        if (passwordEncoder.matches(usuarioSenha.getSenha(), usuario.getSenha())) {
            this.usuarioService.excluirConta(usuario);
            SessaoUtil.encerrar(request);

            model.addAttribute("remocao", Mensagens.get("msg.menu.removido"));
            return "login";
        } else {
            model.addAttribute("erro", Mensagens.get("msg.menu.senhaIncorreta"));
            model.addAttribute("abrirModal", "verificacao");
            model.addAttribute("acaoConta", "remover");
            model.addAttribute("nome", usuario.getNome());
            model.addAttribute("email", usuario.getEmail());
            model.addAttribute("dataNascimento", dataParaExibicao(usuario.getDataNascimento()));
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
            model.addAttribute("erro", Mensagens.get("msg.menu.semUsuario"));
            return "redirect:/login";
        }

        Usuario usuario = this.ur.findUsuarioById(Long.parseLong(usuarioId));
        if (usuario == null) {
            return "redirect:/login";
        }

        if (passwordEncoder.matches(usuarioSenha.getSenha(), usuario.getSenha())) {
            return "redirect:/edicaoUsuario";
        } else {
            model.addAttribute("erro", Mensagens.get("msg.menu.senhaIncorreta"));
            model.addAttribute("abrirModal", "verificacao");
            model.addAttribute("acaoConta", "editar");

            model.addAttribute("nome", usuario.getNome());
            model.addAttribute("email", usuario.getEmail());
            model.addAttribute("dataNascimento", dataParaExibicao(usuario.getDataNascimento()));
            model.addAttribute("isMenu", true);

            model.addAttribute("areaId", "");
            model.addAttribute("areaName", "");
            model.addAttribute("area", "");
            model.addAttribute("listas", "");

            return "menu"; // sem redirect
        }
    }


    /** Converte a data guardada (aaaa-mm-dd) para dd/mm/aaaa, o formato mostrado no perfil em todas as telas. */
    static String dataParaExibicao(String dataIso) {
        if (dataIso != null && dataIso.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            return dataIso.substring(8, 10) + "/" + dataIso.substring(5, 7) + "/" + dataIso.substring(0, 4);
        }
        return dataIso;
    }
}
