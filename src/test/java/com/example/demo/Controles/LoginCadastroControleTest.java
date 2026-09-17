package com.example.demo.Controles;

import com.example.demo.ConsultasBD.TokenRepository;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Token;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.ConviteAreaService;
import com.example.demo.Serviços.EnvioDeEmail.EmailService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Testes unitários "de controller como POJO": instancia LoginCadastroControle
// diretamente (sem subir contexto Spring), injeta mocks/stubs via reflection
// nos campos @Autowired, e chama os métodos como o MVC chamaria. EmailService
// é sempre mockado aqui — nunca deixamos um teste realmente enviar e-mail.
class LoginCadastroControleTest {

    private LoginCadastroControle controller;
    private UsuarioRepository usuarioRepository;
    private TokenRepository tokenRepository;
    private EmailService emailService;
    private ConviteAreaService conviteAreaService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        controller = new LoginCadastroControle();
        usuarioRepository = mock(UsuarioRepository.class);
        tokenRepository = mock(TokenRepository.class);
        emailService = mock(EmailService.class);
        conviteAreaService = mock(ConviteAreaService.class);

        ReflectionTestUtils.setField(controller, "ur", usuarioRepository);
        ReflectionTestUtils.setField(controller, "tokenRepository", tokenRepository);
        ReflectionTestUtils.setField(controller, "emailService", emailService);
        ReflectionTestUtils.setField(controller, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(controller, "conviteAreaService", conviteAreaService);
    }

    private Usuario usuarioValido() {
        Usuario usuario = new Usuario();
        usuario.setNome("Fulano");
        usuario.setEmail("fulano@teste.com");
        usuario.setSenha("senhaSegura123");
        usuario.setDataNascimento("2000-01-01");
        return usuario;
    }

    // ---------- cadastroUsuario ----------

    @Test
    void cadastro_comErrosDeBinding_naoSalvaERetornaFormulario() {
        Usuario usuario = usuarioValido();
        BindingResult errosDeBinding = new BeanPropertyBindingResult(usuario, "usuario");
        errosDeBinding.reject("erro.generico");
        Model model = new ExtendedModelMap();

        String view = controller.cadastroUsuario(usuario, errosDeBinding, model);

        assertThat(view).isEqualTo("cadastro");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cadastro_senhaCurta_naoSalva() {
        Usuario usuario = usuarioValido();
        usuario.setSenha("1234567"); // 7 caracteres
        BindingResult semErros = new BeanPropertyBindingResult(usuario, "usuario");
        Model model = new ExtendedModelMap();

        String view = controller.cadastroUsuario(usuario, semErros, model);

        assertThat(view).isEqualTo("cadastro");
        assertThat(model.getAttribute("mensagem")).isEqualTo("A senha deve ter pelo menos 8 caracteres!");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cadastro_anoDeNascimentoNoFuturo_naoSalva() {
        Usuario usuario = usuarioValido();
        usuario.setDataNascimento((Year.now().getValue() + 1) + "-01-01");
        BindingResult semErros = new BeanPropertyBindingResult(usuario, "usuario");
        Model model = new ExtendedModelMap();

        String view = controller.cadastroUsuario(usuario, semErros, model);

        assertThat(view).isEqualTo("cadastro");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cadastro_emailJaCadastrado_naoSalva() {
        Usuario usuario = usuarioValido();
        BindingResult semErros = new BeanPropertyBindingResult(usuario, "usuario");
        Model model = new ExtendedModelMap();
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(usuarioValido());

        String view = controller.cadastroUsuario(usuario, semErros, model);

        assertThat(view).isEqualTo("cadastro");
        assertThat(model.getAttribute("mensagem")).isEqualTo("Este e-mail já está cadastrado!");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cadastro_valido_salvaComSenhaHasheadaENaoVerificadoEEnviaEmail() {
        Usuario usuario = usuarioValido();
        BindingResult semErros = new BeanPropertyBindingResult(usuario, "usuario");
        Model model = new ExtendedModelMap();
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(null);

        String view = controller.cadastroUsuario(usuario, semErros, model);

        assertThat(view).isEqualTo("login");
        assertThat(usuario.isEmailVerificado()).isFalse();
        assertThat(usuario.getSenha()).startsWith("$2"); // hash BCrypt, não texto puro
        assertThat(passwordEncoder.matches("senhaSegura123", usuario.getSenha())).isTrue();
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).save(any(Token.class));
        verify(emailService).enviarEmailVerificacao(eq(usuario.getEmail()), eq(usuario.getNome()), anyString());
    }

    @Test
    void cadastro_falhaAoEnviarEmail_aindaAssimSalvaUsuario() {
        Usuario usuario = usuarioValido();
        BindingResult semErros = new BeanPropertyBindingResult(usuario, "usuario");
        Model model = new ExtendedModelMap();
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(null);
        doThrow(new RuntimeException("SMTP indisponível")).when(emailService)
                .enviarEmailVerificacao(anyString(), anyString(), anyString());

        String view = controller.cadastroUsuario(usuario, semErros, model);

        assertThat(view).isEqualTo("login");
        verify(usuarioRepository).save(usuario);
        assertThat(model.getAttribute("mensagem").toString()).contains("erro ao enviar");
    }

    // ---------- verificarEmail ----------

    @Test
    void verificarEmail_tokenInexistente_mostraErro() {
        when(tokenRepository.findByToken("abc")).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.verificarEmail("abc", model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("erro")).isNotNull();
    }

    @Test
    void verificarEmail_tokenExpirado_mostraErro() {
        Token token = new Token("abc", "fulano@teste.com", LocalDateTime.now().minusMinutes(1), false);
        when(tokenRepository.findByToken("abc")).thenReturn(token);
        Model model = new ExtendedModelMap();

        String view = controller.verificarEmail("abc", model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("erro")).isNotNull();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void verificarEmail_tokenJaUsado_mostraErro() {
        Token token = new Token("abc", "fulano@teste.com", LocalDateTime.now().plusMinutes(10), true);
        when(tokenRepository.findByToken("abc")).thenReturn(token);
        Model model = new ExtendedModelMap();

        String view = controller.verificarEmail("abc", model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("erro")).isNotNull();
    }

    @Test
    void verificarEmail_valido_marcaUsuarioVerificadoETokenUsado() {
        Token token = new Token("abc", "fulano@teste.com", LocalDateTime.now().plusMinutes(10), false);
        Usuario usuario = usuarioValido();
        usuario.setEmailVerificado(false);
        when(tokenRepository.findByToken("abc")).thenReturn(token);
        when(usuarioRepository.findByEmail("fulano@teste.com")).thenReturn(usuario);
        Model model = new ExtendedModelMap();

        String view = controller.verificarEmail("abc", model);

        assertThat(view).isEqualTo("login");
        assertThat(usuario.isEmailVerificado()).isTrue();
        assertThat(token.isUsado()).isTrue();
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).save(token);
    }

    // ---------- reenviarVerificacao ----------

    @Test
    void reenviarVerificacao_emailNaoCadastrado_mostraErro() {
        when(usuarioRepository.findByEmail("naoexiste@teste.com")).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.reenviarVerificacao("naoexiste@teste.com", model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("erro")).isNotNull();
        verify(emailService, never()).enviarEmailVerificacao(anyString(), anyString(), anyString());
    }

    @Test
    void reenviarVerificacao_emailJaVerificado_naoReenvia() {
        Usuario usuario = usuarioValido();
        usuario.setEmailVerificado(true);
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(usuario);
        Model model = new ExtendedModelMap();

        String view = controller.reenviarVerificacao(usuario.getEmail(), model);

        assertThat(view).isEqualTo("login");
        verify(emailService, never()).enviarEmailVerificacao(anyString(), anyString(), anyString());
    }

    @Test
    void reenviarVerificacao_naoVerificado_geraNovoTokenEReenvia() {
        Usuario usuario = usuarioValido();
        usuario.setEmailVerificado(false);
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(usuario);
        Model model = new ExtendedModelMap();

        String view = controller.reenviarVerificacao(usuario.getEmail(), model);

        assertThat(view).isEqualTo("login");
        verify(tokenRepository).save(any(Token.class));
        verify(emailService).enviarEmailVerificacao(eq(usuario.getEmail()), eq(usuario.getNome()), anyString());
    }

    // ---------- loginUsuario ----------

    @Test
    void login_senhaErrada_naoAutentica() {
        Usuario cadastrado = usuarioValido();
        cadastrado.setSenha(passwordEncoder.encode("senhaCorreta123"));
        cadastrado.setEmailVerificado(true);
        when(usuarioRepository.findByEmail(cadastrado.getEmail())).thenReturn(cadastrado);

        Usuario tentativa = new Usuario();
        tentativa.setEmail(cadastrado.getEmail());
        tentativa.setSenha("senhaErrada");

        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ExtendedModelMap();

        String view = controller.loginUsuario(tentativa, model, request);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("erro")).isEqualTo("Usuário Inválido");
        assertThat(SessaoUtil.getUsuarioId(request)).isNull();
    }

    @Test
    void login_senhaEmTextoPuroLegado_naoAutentica() {
        // Conta antiga (era assim antes do BCrypt ser adotado): senha salva sem hash.
        Usuario cadastrado = usuarioValido();
        cadastrado.setSenha("senhaSegura123"); // não começa com "$2"
        cadastrado.setEmailVerificado(true);
        when(usuarioRepository.findByEmail(cadastrado.getEmail())).thenReturn(cadastrado);

        Usuario tentativa = new Usuario();
        tentativa.setEmail(cadastrado.getEmail());
        tentativa.setSenha("senhaSegura123");

        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ExtendedModelMap();

        String view = controller.loginUsuario(tentativa, model, request);

        assertThat(view).isEqualTo("login");
        assertThat(SessaoUtil.getUsuarioId(request)).isNull();
    }

    @Test
    void login_emailNaoVerificado_bloqueiaEIndicaReenvio() {
        Usuario cadastrado = usuarioValido();
        cadastrado.setSenha(passwordEncoder.encode("senhaSegura123"));
        cadastrado.setEmailVerificado(false);
        when(usuarioRepository.findByEmail(cadastrado.getEmail())).thenReturn(cadastrado);

        Usuario tentativa = new Usuario();
        tentativa.setEmail(cadastrado.getEmail());
        tentativa.setSenha("senhaSegura123");

        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ExtendedModelMap();

        String view = controller.loginUsuario(tentativa, model, request);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("emailNaoVerificado")).isEqualTo(cadastrado.getEmail());
        assertThat(SessaoUtil.getUsuarioId(request)).isNull();
    }

    @Test
    void login_valido_autenticaERedirecionaParaMenu() {
        Usuario cadastrado = usuarioValido();
        cadastrado.setSenha(passwordEncoder.encode("senhaSegura123"));
        cadastrado.setEmailVerificado(true);
        ReflectionTestUtils.setField(cadastrado, "id", 42L);
        when(usuarioRepository.findByEmail(cadastrado.getEmail())).thenReturn(cadastrado);

        Usuario tentativa = new Usuario();
        tentativa.setEmail(cadastrado.getEmail());
        tentativa.setSenha("senhaSegura123");

        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ExtendedModelMap();

        String view = controller.loginUsuario(tentativa, model, request);

        assertThat(view).isEqualTo("redirect:/menu");
        assertThat(SessaoUtil.getUsuarioId(request)).isEqualTo("42");
    }

    @Test
    void login_comConvitePendenteNaSessao_aceitaConviteERedirecionaParaArea() {
        Usuario cadastrado = usuarioValido();
        cadastrado.setSenha(passwordEncoder.encode("senhaSegura123"));
        cadastrado.setEmailVerificado(true);
        ReflectionTestUtils.setField(cadastrado, "id", 42L);
        when(usuarioRepository.findByEmail(cadastrado.getEmail())).thenReturn(cadastrado);

        var areaFake = new com.example.demo.Entidades.AreaTrabalho();
        ReflectionTestUtils.setField(areaFake, "id", 7L);
        areaFake.setNome("Área Convidada");
        when(conviteAreaService.aceitarLinkGenerico(eq("token-convite"), any()))
                .thenReturn(new ConviteAreaService.Resultado(true, "ok", areaFake));

        Usuario tentativa = new Usuario();
        tentativa.setEmail(cadastrado.getEmail());
        tentativa.setSenha("senhaSegura123");

        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession(true);
        session.setAttribute("conviteAreaPendente", "token-convite");
        Model model = new ExtendedModelMap();

        String view = controller.loginUsuario(tentativa, model, request);

        assertThat(view).isEqualTo("redirect:/areasTrabalho/7/Área Convidada");
        assertThat(session.getAttribute("conviteAreaPendente")).isNull(); // consumido
    }

    // ---------- logout ----------

    @Test
    void logout_encerraSessaoERedirecionaParaLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);

        String view = controller.logout(request);

        assertThat(view).isEqualTo("redirect:/login");
    }
}
