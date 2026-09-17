package com.example.demo.Controles;

import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuControllerTest {

    private MenuController controller;
    private UsuarioRepository usuarioRepository;
    private UsuarioService usuarioService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        controller = new MenuController();
        usuarioRepository = mock(UsuarioRepository.class);
        usuarioService = mock(UsuarioService.class);
        ReflectionTestUtils.setField(controller, "ur", usuarioRepository);
        ReflectionTestUtils.setField(controller, "usuarioService", usuarioService);
        ReflectionTestUtils.setField(controller, "passwordEncoder", passwordEncoder);
    }

    private Usuario usuarioExistente(long id) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setNome("Fulano");
        usuario.setEmail("fulano@teste.com");
        usuario.setSenha(passwordEncoder.encode("senhaCorreta123"));
        usuario.setDataNascimento("2000-01-01");
        return usuario;
    }

    @Test
    void menu_semSessao_redirecionaParaLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ExtendedModelMap();

        assertThat(controller.Menu(model, request)).isEqualTo("redirect:/login");
    }

    @Test
    void menu_comSessao_mostraDadosDoUsuario() {
        Usuario usuario = usuarioExistente(1L);
        when(usuarioRepository.findUsuarioById(1L)).thenReturn(usuario);
        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        Model model = new ExtendedModelMap();

        String view = controller.Menu(model, request);

        assertThat(view).isEqualTo("menu");
        assertThat(model.getAttribute("nome")).isEqualTo("Fulano");
    }

    @Test
    void removerConta_senhaErrada_naoExclui() {
        Usuario usuario = usuarioExistente(1L);
        when(usuarioRepository.findUsuarioById(1L)).thenReturn(usuario);

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        Model model = new ExtendedModelMap();

        Usuario senhaErrada = new Usuario();
        senhaErrada.setSenha("senhaErrada");

        String view = controller.exclusaoDeConta(senhaErrada, request, model);

        assertThat(view).isEqualTo("menu");
        assertThat(model.getAttribute("erro")).isEqualTo("Senha incorreta. Tente novamente.");
        verify(usuarioService, never()).excluirConta(any());
        assertThat(SessaoUtil.getUsuarioId(request)).isEqualTo("1"); // sessão não foi encerrada
    }

    @Test
    void removerConta_senhaCorreta_chamaServicoEEncerraSessao() {
        Usuario usuario = usuarioExistente(1L);
        when(usuarioRepository.findUsuarioById(1L)).thenReturn(usuario);

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        Model model = new ExtendedModelMap();

        Usuario senhaCorreta = new Usuario();
        senhaCorreta.setSenha("senhaCorreta123");

        String view = controller.exclusaoDeConta(senhaCorreta, request, model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("remocao")).isNotNull();
        verify(usuarioService).excluirConta(usuario);
        assertThat(SessaoUtil.getUsuarioId(request)).isNull();
    }

    @Test
    void editarConta_senhaErrada_naoRedirecionaParaEdicao() {
        Usuario usuario = usuarioExistente(1L);
        when(usuarioRepository.findUsuarioById(1L)).thenReturn(usuario);

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        Model model = new ExtendedModelMap();

        Usuario senhaErrada = new Usuario();
        senhaErrada.setSenha("errada");

        String view = controller.edicaodeConta(senhaErrada, request, model);

        assertThat(view).isEqualTo("menu");
    }

    @Test
    void editarConta_senhaCorreta_redirecionaParaEdicao() {
        Usuario usuario = usuarioExistente(1L);
        when(usuarioRepository.findUsuarioById(1L)).thenReturn(usuario);

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        Model model = new ExtendedModelMap();

        Usuario senhaCorreta = new Usuario();
        senhaCorreta.setSenha("senhaCorreta123");

        String view = controller.edicaodeConta(senhaCorreta, request, model);

        assertThat(view).isEqualTo("redirect:/edicaoUsuario");
    }
}
