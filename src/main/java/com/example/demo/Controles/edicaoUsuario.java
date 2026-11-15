package com.example.demo.Controles;

import com.example.demo.Serviços.Idioma.Mensagens;

import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
public class edicaoUsuario {

    @Autowired
    private UsuarioRepository ur;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/edicaoUsuario")
    public String carregarEdicaoUsuario(HttpServletRequest request, Model model) {

        String usuarioId = SessaoUtil.getUsuarioId(request);

        if (usuarioId != null) {
            Usuario usuario = ur.findUsuarioById(Long.parseLong(usuarioId));

            if (usuario != null) {
                model.addAttribute("usuario", usuario);
                // A data já vem escrita no formato do idioma, para aparecer junto com os outros campos
                model.addAttribute("dataNascimentoExibicao", MenuController.dataParaExibicao(usuario.getDataNascimento()));
                return "edicaoUsuario";
            }
        }

        return "redirect:/login";
    }

    @PostMapping("/edicaoUsuario")
    public String edicaoUsuario(@ModelAttribute Usuario usuarioEditado, HttpServletRequest request, BindingResult result, Model model) {

        boolean vaiTrocarSenha = usuarioEditado.getSenha() != null && !usuarioEditado.getSenha().isBlank();
        model.addAttribute("dataNascimentoExibicao", MenuController.dataParaExibicao(usuarioEditado.getDataNascimento()));

        if (result.hasErrors()) {
            model.addAttribute("mensagem", Mensagens.get("msg.edicao.erro"));
            return "edicaoUsuario";

        } else if (LoginCadastroControle.dataNascimentoInvalida(usuarioEditado.getDataNascimento())) {
            model.addAttribute("mensagem", Mensagens.get("msg.ano.invalido"));
            return "edicaoUsuario";
        }

        else if (usuarioEditado.getNome() != null && usuarioEditado.getNome().length() > Usuario.NOME_MAX) {
            model.addAttribute("mensagem", Mensagens.get("msg.nome.longo", Usuario.NOME_MAX));
            return "edicaoUsuario";
        }

        else if (vaiTrocarSenha && LoginCadastroControle.senhaLonga(usuarioEditado.getSenha())) {
            model.addAttribute("mensagem", Mensagens.get("msg.senha.longa", Usuario.SENHA_MAX));
            return "edicaoUsuario";
        }

        else if (vaiTrocarSenha && usuarioEditado.getSenha().length() < 8) {
            model.addAttribute("mensagem", Mensagens.get("msg.senha.curta"));
            return "edicaoUsuario";
        }

        else if (vaiTrocarSenha && !LoginCadastroControle.senhaForte(usuarioEditado.getSenha())) {
            model.addAttribute("mensagem", Mensagens.get("msg.senha.fraca"));
            return "edicaoUsuario";
        } else {

            String usuarioId = SessaoUtil.getUsuarioId(request);

            if (usuarioId != null) {
                Long id = Long.parseLong(usuarioId);

                Usuario usuarioExistente = ur.findUsuarioById(id);

                if (usuarioExistente != null) {
                    // O e-mail é o login e a identidade da conta: não pode ser alterado aqui (o campo da tela
                    // é somente leitura e qualquer valor enviado é ignorado). Assim um erro de digitação não
                    // tranca a conta.
                    usuarioExistente.setNome(usuarioEditado.getNome());
                    usuarioExistente.setDataNascimento(usuarioEditado.getDataNascimento());

                    if (vaiTrocarSenha) {
                        usuarioExistente.setSenha(passwordEncoder.encode(usuarioEditado.getSenha()));
                    }

                    ur.save(usuarioExistente);

                    model.addAttribute("mensagem", Mensagens.get("msg.edicao.sucesso"));
                    model.addAttribute("usuario", usuarioExistente);
                    return "login";
                }
            }

            model.addAttribute("erro", Mensagens.get("msg.edicao.naoLogado"));
            return "redirect:/login";
        }
    }

}