package com.example.demo.Controles;

import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class edicaoUsuarioTest {

    private edicaoUsuario controller;
    private UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        controller = new edicaoUsuario();
        usuarioRepository = mock(UsuarioRepository.class);
        ReflectionTestUtils.setField(controller, "ur", usuarioRepository);
        ReflectionTestUtils.setField(controller, "passwordEncoder", passwordEncoder);
    }

    private Usuario usuarioExistente(long id) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setNome("Fulano");
        usuario.setEmail("fulano@teste.com");
        usuario.setSenha(passwordEncoder.encode("senhaAtual123"));
        usuario.setDataNascimento("2000-01-01");
        usuario.setEmailVerificado(true);
        return usuario;
    }

    @Test
    void get_semSessao_redirecionaParaLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ExtendedModelMap();

        String view = controller.carregarEdicaoUsuario(request, model);

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void get_comSessao_mostraFormularioComDadosAtuais() {
        Usuario usuario = usuarioExistente(1L);
        when(usuarioRepository.findUsuarioById(1L)).thenReturn(usuario);
        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        Model model = new ExtendedModelMap();

        String view = controller.carregarEdicaoUsuario(request, model);

        assertThat(view).isEqualTo("edicaoUsuario");
        assertThat(model.getAttribute("usuario")).isEqualTo(usuario);
    }

    @Test
    void post_tentandoTrocarEmail_ignoraONovoEmailEMantemOLogin() {
        Usuario existente = usuarioExistente(1L);
        when(usuarioRepository.findUsuarioById(1L)).thenReturn(existente);

        Usuario edicao = new Usuario();
        edicao.setNome("Fulano Editado");
        edicao.setEmail("novoemail@teste.com"); // o e-mail não é editável: deve ser ignorado
        edicao.setDataNascimento("2000-01-01");

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        BindingResult semErros = new BeanPropertyBindingResult(edicao, "usuario");
        Model model = new ExtendedModelMap();

        String view = controller.edicaoUsuario(edicao, request, semErros, model);

        assertThat(view).isEqualTo("login");
        assertThat(existente.getEmail()).isEqualTo("fulano@teste.com");
        assertThat(existente.isEmailVerificado()).isTrue();
        assertThat(existente.getNome()).isEqualTo("Fulano Editado");
        verify(usuarioRepository).save(existente);
        verify(usuarioRepository, never()).findByEmail(any());
    }

    @Test
    void post_semEmailNoFormulario_salvaMesmoAssim() {
        Usuario existente = usuarioExistente(1L);
        when(usuarioRepository.findUsuarioById(1L)).thenReturn(existente);

        Usuario edicao = new Usuario();
        edicao.setNome("Fulano Editado");
        edicao.setDataNascimento("2000-01-01"); // sem e-mail

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        BindingResult semErros = new BeanPropertyBindingResult(edicao, "usuario");
        Model model = new ExtendedModelMap();

        String view = controller.edicaoUsuario(edicao, request, semErros, model);

        assertThat(view).isEqualTo("login");
        assertThat(existente.getEmail()).isEqualTo("fulano@teste.com");
        verify(usuarioRepository).save(existente);
    }

    @Test
    void post_mantendoMesmoEmail_continuaVerificado() {
        Usuario existente = usuarioExistente(1L);
        when(usuarioRepository.findUsuarioById(1L)).thenReturn(existente);

        Usuario edicao = new Usuario();
        edicao.setNome("Fulano Editado");
        edicao.setEmail(existente.getEmail()); // mesmo e-mail
        edicao.setDataNascimento("2000-01-01");

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        BindingResult semErros = new BeanPropertyBindingResult(edicao, "usuario");
        Model model = new ExtendedModelMap();

        String view = controller.edicaoUsuario(edicao, request, semErros, model);

        assertThat(view).isEqualTo("login");
        assertThat(existente.isEmailVerificado()).isTrue();
        assertThat(existente.getNome()).isEqualTo("Fulano Editado");
        verify(usuarioRepository, never()).findByEmail(any());
    }

    @Test
    void post_semTrocarSenha_mantemHashAntigo() {
        Usuario existente = usuarioExistente(1L);
        String hashAntigo = existente.getSenha();
        when(usuarioRepository.findUsuarioById(1L)).thenReturn(existente);

        Usuario edicao = new Usuario();
        edicao.setNome("Fulano");
        edicao.setEmail(existente.getEmail());
        edicao.setDataNascimento("2000-01-01");
        edicao.setSenha(""); // campo em branco: não deve trocar a senha

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        BindingResult semErros = new BeanPropertyBindingResult(edicao, "usuario");
        Model model = new ExtendedModelMap();

        controller.edicaoUsuario(edicao, request, semErros, model);

        assertThat(existente.getSenha()).isEqualTo(hashAntigo);
    }

    @Test
    void post_trocandoSenhaCurta_bloqueiaEdicao() {
        Usuario existente = usuarioExistente(1L);
        when(usuarioRepository.findUsuarioById(1L)).thenReturn(existente);

        Usuario edicao = new Usuario();
        edicao.setNome("Fulano");
        edicao.setEmail(existente.getEmail());
        edicao.setDataNascimento("2000-01-01");
        edicao.setSenha("123");

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        BindingResult semErros = new BeanPropertyBindingResult(edicao, "usuario");
        Model model = new ExtendedModelMap();

        String view = controller.edicaoUsuario(edicao, request, semErros, model);

        assertThat(view).isEqualTo("edicaoUsuario");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void post_trocandoSenhaFraca_bloqueiaEdicao() {
        Usuario existente = usuarioExistente(1L);
        when(usuarioRepository.findUsuarioById(1L)).thenReturn(existente);

        Usuario edicao = new Usuario();
        edicao.setNome("Fulano");
        edicao.setEmail(existente.getEmail());
        edicao.setDataNascimento("2000-01-01");
        edicao.setSenha("senhafraca123"); // 8+ caracteres, mas sem maiúscula nem especial

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        BindingResult semErros = new BeanPropertyBindingResult(edicao, "usuario");
        Model model = new ExtendedModelMap();

        String view = controller.edicaoUsuario(edicao, request, semErros, model);

        assertThat(view).isEqualTo("edicaoUsuario");
        assertThat(model.getAttribute("mensagem")).isEqualTo(
                "A senha precisa ter letra maiúscula, letra minúscula, número e caractere especial.");
        verify(usuarioRepository, never()).save(any());
    }
}
