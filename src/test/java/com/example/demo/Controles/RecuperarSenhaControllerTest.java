package com.example.demo.Controles;

import com.example.demo.ConsultasBD.TokenRepository;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Token;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.EnvioDeEmail.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecuperarSenhaControllerTest {

    private RecuperarSenhaController controller;
    private UsuarioRepository usuarioRepository;
    private TokenRepository tokenRepository;
    private EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        controller = new RecuperarSenhaController();
        usuarioRepository = mock(UsuarioRepository.class);
        tokenRepository = mock(TokenRepository.class);
        emailService = mock(EmailService.class);

        ReflectionTestUtils.setField(controller, "ur", usuarioRepository);
        ReflectionTestUtils.setField(controller, "tokenRepository", tokenRepository);
        ReflectionTestUtils.setField(controller, "emailService2", emailService);
        ReflectionTestUtils.setField(controller, "passwordEncoder", passwordEncoder);
    }

    private Usuario usuarioExistente() {
        Usuario usuario = new Usuario();
        usuario.setNome("Fulano");
        usuario.setEmail("fulano@teste.com");
        usuario.setSenha(passwordEncoder.encode("senhaAntiga123"));
        usuario.setDataNascimento("2000-01-01");
        return usuario;
    }

    @Test
    void recuperarSenha_emailNaoCadastrado_naoEnviaEmail() {
        when(usuarioRepository.findByEmail("naoexiste@teste.com")).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.recuperarSenha("naoexiste@teste.com", model, new MockHttpServletResponse());

        assertThat(view).isEqualTo("recuperarSenhaEmail");
        assertThat(model.getAttribute("erro")).isNotNull();
        verify(emailService, never()).enviarEmailRecuperacaoSenha(anyString(), anyString(), anyString());
    }

    @Test
    void recuperarSenha_emailCadastrado_geraTokenEEnviaEmail() {
        Usuario usuario = usuarioExistente();
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(usuario);
        Model model = new ExtendedModelMap();

        String view = controller.recuperarSenha(usuario.getEmail(), model, new MockHttpServletResponse());

        assertThat(view).isEqualTo("recuperarSenhaEmail");
        verify(tokenRepository).save(org.mockito.ArgumentMatchers.any(Token.class));
        verify(emailService).enviarEmailRecuperacaoSenha(eq(usuario.getEmail()), eq(usuario.getNome()), anyString());
    }

    @Test
    void redefinicaoSenha_tokenInvalido_redirecionaParaLoginComErro() {
        when(tokenRepository.findByToken("xyz")).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.redefinicaoSenha("xyz", model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("erro")).isNotNull();
    }

    @Test
    void redefinicaoSenha_tokenExpirado_redirecionaParaLoginComErro() {
        Token token = new Token("xyz", "fulano@teste.com", LocalDateTime.now().minusMinutes(1), false);
        when(tokenRepository.findByToken("xyz")).thenReturn(token);
        Model model = new ExtendedModelMap();

        String view = controller.redefinicaoSenha("xyz", model);

        assertThat(view).isEqualTo("login");
    }

    @Test
    void redefinicaoSenha_tokenValido_mostraFormulario() {
        Token token = new Token("xyz", "fulano@teste.com", LocalDateTime.now().plusMinutes(5), false);
        when(tokenRepository.findByToken("xyz")).thenReturn(token);
        Model model = new ExtendedModelMap();

        String view = controller.redefinicaoSenha("xyz", model);

        assertThat(view).isEqualTo("redefinicaoSenha");
        assertThat(model.getAttribute("token")).isEqualTo("xyz");
    }

    @Test
    void redefinirSenha_tokenInvalido_naoAlteraSenha() {
        when(tokenRepository.findByToken("xyz")).thenReturn(null);
        Model model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest();

        String view = controller.redefinirSenha("novaSenha123", "xyz", model, request);

        assertThat(view).isEqualTo("redefinicaoSenha");
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void redefinirSenha_tokenValido_alteraSenhaMarcaTokenUsadoEEncerraSessao() {
        Usuario usuario = usuarioExistente();
        String senhaAntigaHash = usuario.getSenha();
        Token token = new Token("xyz", usuario.getEmail(), LocalDateTime.now().plusMinutes(5), false);
        when(tokenRepository.findByToken("xyz")).thenReturn(token);
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(usuario);

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L); // simula sessão ativa que deve ser encerrada
        Model model = new ExtendedModelMap();

        String view = controller.redefinirSenha("novaSenhaForte123", "xyz", model, request);

        assertThat(view).isEqualTo("login");
        assertThat(usuario.getSenha()).isNotEqualTo(senhaAntigaHash);
        assertThat(passwordEncoder.matches("novaSenhaForte123", usuario.getSenha())).isTrue();
        assertThat(token.isUsado()).isTrue();
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).save(token);
        assertThat(SessaoUtil.getUsuarioId(request)).isNull();
    }
}
