package com.example.demo.Controles;

import com.example.demo.ConsultasBD.AreaTrabalhoRepository;
import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.ConsultasBD.TokenRepository;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
import com.example.demo.Entidades.Token;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.EnvioDeEmail.EmailService;
import com.example.demo.Serviços.PermissaoAreaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompartilharAreadeTrabalhoTest {

    private CompartilharAreadeTrabalho controller;
    private UsuarioRepository usuarioRepository;
    private AreaTrabalhoRepository areaTrabalhoRepository;
    private TokenRepository tokenRepository;
    private ParticipacaoAreaRepository participacaoAreaRepository;
    private EmailService emailService;
    private PermissaoAreaService permissaoAreaService;

    @BeforeEach
    void setUp() {
        controller = new CompartilharAreadeTrabalho();
        usuarioRepository = mock(UsuarioRepository.class);
        areaTrabalhoRepository = mock(AreaTrabalhoRepository.class);
        tokenRepository = mock(TokenRepository.class);
        participacaoAreaRepository = mock(ParticipacaoAreaRepository.class);
        emailService = mock(EmailService.class);
        permissaoAreaService = mock(PermissaoAreaService.class);

        ReflectionTestUtils.setField(controller, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(controller, "areaTrabalhoRepository", areaTrabalhoRepository);
        ReflectionTestUtils.setField(controller, "tokenRepository", tokenRepository);
        ReflectionTestUtils.setField(controller, "participacaoAreaRepository", participacaoAreaRepository);
        ReflectionTestUtils.setField(controller, "emailService", emailService);
        ReflectionTestUtils.setField(controller, "permissaoAreaService", permissaoAreaService);
    }

    private Usuario usuario(long id, String email) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setNome("Usuário " + id);
        usuario.setEmail(email);
        usuario.setDataNascimento("2000-01-01");
        return usuario;
    }

    private AreaTrabalho area(long id) {
        AreaTrabalho area = new AreaTrabalho();
        ReflectionTestUtils.setField(area, "id", id);
        area.setNome("Área " + id);
        return area;
    }

    // ---------- enviarNotificacao (convite por e-mail) ----------

    @Test
    void enviarNotificacao_naoAutenticado_retorna401() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Map<String, Object> dados = Map.of("email", "convidado@teste.com", "id_area", 1);

        ResponseEntity<?> resposta = controller.enviarNotificacao(dados, request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void enviarNotificacao_observadorSemPermissao_retorna403() {
        Usuario remetente = usuario(1L, "dono@teste.com");
        Usuario destinatario = usuario(2L, "convidado@teste.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(remetente));
        when(areaTrabalhoRepository.findById(10L)).thenReturn(Optional.of(area(10L)));
        when(usuarioRepository.findByEmail("convidado@teste.com")).thenReturn(destinatario);
        when(permissaoAreaService.podeEditar(1L, 10L)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        Map<String, Object> dados = Map.of("email", "convidado@teste.com", "id_area", 10);

        ResponseEntity<?> resposta = controller.enviarNotificacao(dados, request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(403);
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviarEmailCompartilharAreaTrabalho(any(), anyString(), any(), anyString());
    }

    @Test
    void enviarNotificacao_editorComPermissao_geraTokenEEnviaConvite() {
        Usuario remetente = usuario(1L, "dono@teste.com");
        Usuario destinatario = usuario(2L, "convidado@teste.com");
        AreaTrabalho area = area(10L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(remetente));
        when(areaTrabalhoRepository.findById(10L)).thenReturn(Optional.of(area));
        when(usuarioRepository.findByEmail("convidado@teste.com")).thenReturn(destinatario);
        when(permissaoAreaService.podeEditar(1L, 10L)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        Map<String, Object> dados = Map.of("email", "convidado@teste.com", "id_area", 10);

        ResponseEntity<?> resposta = controller.enviarNotificacao(dados, request);

        assertThat(resposta.getStatusCode().is2xxSuccessful()).isTrue();
        verify(tokenRepository).save(any(Token.class));
        verify(emailService).enviarEmailCompartilharAreaTrabalho(eq(destinatario), eq(remetente.getNome()), eq(area), anyString());
    }

    // ---------- compartilhamentoArea (aceitar convite por e-mail) ----------

    @Test
    void aceitarConvite_tokenExpirado_mostraErro() {
        Token token = new Token("tok", "convidado@teste.com", LocalDateTime.now().minusMinutes(1), false);
        token.setAreaId(10L);
        when(tokenRepository.findByToken("tok")).thenReturn(token);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "convidado@teste.com")));
        when(areaTrabalhoRepository.findById(10L)).thenReturn(Optional.of(area(10L)));
        Model model = new ExtendedModelMap();

        String view = controller.compartilhamentoArea("tok", "2", "10", model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("erro")).isNotNull();
        verify(participacaoAreaRepository, never()).save(any());
    }

    @Test
    void aceitarConvite_areaDoTokenDiferenteDaUrl_bloqueiaIDOR() {
        // Token válido, mas emitido para OUTRA área — a URL tenta trocar o areaID
        // pra entrar numa área diferente reaproveitando um convite legítimo.
        Token token = new Token("tok", "convidado@teste.com", LocalDateTime.now().plusMinutes(10), false);
        token.setAreaId(10L); // área original do convite
        when(tokenRepository.findByToken("tok")).thenReturn(token);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "convidado@teste.com")));
        when(areaTrabalhoRepository.findById(99L)).thenReturn(Optional.of(area(99L))); // área diferente na URL
        Model model = new ExtendedModelMap();

        String view = controller.compartilhamentoArea("tok", "2", "99", model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("erro")).isNotNull();
        verify(participacaoAreaRepository, never()).save(any());
    }

    @Test
    void aceitarConvite_destinatarioDoTokenDiferenteDaUrl_bloqueiaIDOR() {
        // Token emitido pro e-mail de outra pessoa — a URL tenta trocar o destID
        // pra outro usuário usar o convite de alguém que não é ele.
        Token token = new Token("tok", "vitima@teste.com", LocalDateTime.now().plusMinutes(10), false);
        token.setAreaId(10L);
        when(tokenRepository.findByToken("tok")).thenReturn(token);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "intruso@teste.com")));
        when(areaTrabalhoRepository.findById(10L)).thenReturn(Optional.of(area(10L)));
        Model model = new ExtendedModelMap();

        String view = controller.compartilhamentoArea("tok", "2", "10", model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("erro")).isNotNull();
        verify(participacaoAreaRepository, never()).save(any());
    }

    @Test
    void aceitarConvite_valido_criaParticipacaoEMarcaTokenUsado() {
        Token token = new Token("tok", "convidado@teste.com", LocalDateTime.now().plusMinutes(10), false);
        token.setAreaId(10L);
        Usuario destinatario = usuario(2L, "convidado@teste.com");
        AreaTrabalho area = area(10L);
        when(tokenRepository.findByToken("tok")).thenReturn(token);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(destinatario));
        when(areaTrabalhoRepository.findById(10L)).thenReturn(Optional.of(area));
        Model model = new ExtendedModelMap();

        String view = controller.compartilhamentoArea("tok", "2", "10", model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("mensagem")).isNotNull();
        assertThat(token.isUsado()).isTrue();
        verify(participacaoAreaRepository).save(any(ParticipacaoArea.class));
        verify(tokenRepository).save(token);
    }
}
