package com.example.demo.Serviços.Idioma;

import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Idiomas: escolha do idioma, textos nos três idiomas e coerência dos arquivos de tradução. */
class IdiomaTest {

    @AfterEach
    void limpaIdioma() {
        LocaleContextHolder.resetLocaleContext();
    }

    private static Properties carregar(String arquivo) throws Exception {
        Properties p = new Properties();
        try (InputStream in = IdiomaTest.class.getResourceAsStream("/" + arquivo)) {
            assertThat(in).as(arquivo).isNotNull();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    // ---------- Idioma ----------

    @Test
    void reconheceOsCodigosDeIdioma() {
        assertThat(Idioma.de("pt")).isEqualTo(Idioma.PT_BR);
        assertThat(Idioma.de("pt_BR")).isEqualTo(Idioma.PT_BR);
        assertThat(Idioma.de("EN")).isEqualTo(Idioma.EN);
        assertThat(Idioma.de("en-US")).isEqualTo(Idioma.EN);
        assertThat(Idioma.de("es-MX")).isEqualTo(Idioma.ES);
        assertThat(Idioma.de("fr")).isNull();
        assertThat(Idioma.de(null)).isNull();
        assertThat(Idioma.deOuPadrao("fr")).isEqualTo(Idioma.PT_BR);
    }

    // ---------- textos ----------

    @Test
    void semRequisicaoOsTextosSaemEmPortugues() {
        assertThat(Mensagens.get("msg.usuario.naoEncontrado")).isEqualTo("Usuário não encontrado");
    }

    @Test
    void textosSaemNoIdiomaDaRequisicao() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(Mensagens.get("msg.usuario.naoEncontrado")).isEqualTo("User not found");
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
        assertThat(Mensagens.get("msg.usuario.naoEncontrado")).isEqualTo("Usuario no encontrado");
    }

    @Test
    void textosComParametrosSubstituemOValor() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(Mensagens.get("msg.email.erroEnvio", "falhou")).isEqualTo("Error sending email! falhou");
    }

    @Test
    void chaveInexistenteDevolveAPropriaChave() {
        assertThat(Mensagens.get("chave.que.nao.existe")).isEqualTo("chave.que.nao.existe");
    }

    @Test
    void textosDoJavaScriptVemSoComPrefixoJs() {
        Map<String, String> en = Mensagens.comPrefixo(Locale.ENGLISH, "js.");
        assertThat(en).isNotEmpty().allSatisfy((chave, texto) -> assertThat(chave).startsWith("js."));
        assertThat(en.get("js.aviso.cancelar")).isEqualTo("Cancel");
        assertThat(Mensagens.comPrefixo(Locale.forLanguageTag("pt-BR"), "js.").get("js.aviso.cancelar")).isEqualTo("Cancelar");
    }

    @Test
    void ingleseEEspanholTemTodasAsChavesDoPortuguesESemApostrofoComum() throws Exception {
        Properties pt = carregar("mensagens.properties");
        for (String arquivo : new String[]{"mensagens.properties", "mensagens_en.properties", "mensagens_es.properties"}) {
            Properties outro = carregar(arquivo);
            assertThat(outro.stringPropertyNames()).as(arquivo).isEqualTo(pt.stringPropertyNames());
            outro.forEach((chave, valor) -> {
                assertThat(valor.toString()).as(arquivo + " " + chave).isNotBlank().doesNotContain("'");
            });
        }
    }

    // ---------- escolha do idioma ----------

    @Test
    void idiomaDaRequisicao_sessaoVenceCookieQueVenceOPadrao() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(IdiomaLocaleResolver.idiomaDa(request)).isEqualTo(Idioma.PT_BR);

        request.setCookies(new Cookie(IdiomaLocaleResolver.COOKIE, "en"));
        assertThat(IdiomaLocaleResolver.idiomaDa(request)).isEqualTo(Idioma.EN);

        request.getSession(true).setAttribute(IdiomaLocaleResolver.ATRIBUTO_SESSAO, "es");
        assertThat(IdiomaLocaleResolver.idiomaDa(request)).isEqualTo(Idioma.ES);
        assertThat(new IdiomaLocaleResolver().resolveLocale(request).getLanguage()).isEqualTo("es");
    }

    @Test
    void cookieComValorInvalidoCaiNoPadrao() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(IdiomaLocaleResolver.COOKIE, "klingon"));
        assertThat(IdiomaLocaleResolver.idiomaDa(request)).isEqualTo(Idioma.PT_BR);
    }

    @Test
    void interceptadorAtualizaOCookieQuandoASessaoMudou() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(IdiomaLocaleResolver.ATRIBUTO_SESSAO, "en");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new IdiomaInterceptor().preHandle(request, response, new Object());

        assertThat(response.getHeader("Set-Cookie")).contains("todaily_idioma=en").contains("Path=/").contains("SameSite=Lax");
    }

    @Test
    void interceptadorNaoMexeNoCookieQuandoJaEstaIgual() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(IdiomaLocaleResolver.ATRIBUTO_SESSAO, "en");
        request.setCookies(new Cookie(IdiomaLocaleResolver.COOKIE, "en"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new IdiomaInterceptor().preHandle(request, response, new Object());

        assertThat(response.getHeader("Set-Cookie")).isNull();
    }

    // ---------- IdiomaController ----------

    private IdiomaController controller(UsuarioRepository repo) {
        IdiomaController c = new IdiomaController();
        ReflectionTestUtils.setField(c, "usuarioRepository", repo);
        ReflectionTestUtils.setField(c, "objectMapper", new ObjectMapper());
        return c;
    }

    @Test
    void trocarIdioma_semLogin_guardaNaSessaoENoCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String destino = controller(mock(UsuarioRepository.class)).trocar("en", "/cadastro", request, response);

        assertThat(destino).isEqualTo("redirect:/cadastro");
        assertThat(request.getSession().getAttribute(IdiomaLocaleResolver.ATRIBUTO_SESSAO)).isEqualTo("en");
        assertThat(response.getHeader("Set-Cookie")).contains("todaily_idioma=en");
    }

    @Test
    void trocarIdioma_comLogin_guardaNoPerfil() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        when(repo.findById(7L)).thenReturn(Optional.of(usuario));
        MockHttpServletRequest request = new MockHttpServletRequest();
        com.example.demo.Serviços.Autentificador.SessaoUtil.autenticar(request, 7L);

        controller(repo).trocar("es", "/menu?view=todas", request, new MockHttpServletResponse());

        assertThat(usuario.getIdioma()).isEqualTo("es");
        verify(repo).save(usuario);
    }

    @Test
    void trocarIdioma_comCodigoInvalidoUsaPortugues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        controller(mock(UsuarioRepository.class)).trocar("xx", null, request, new MockHttpServletResponse());
        assertThat(request.getSession().getAttribute(IdiomaLocaleResolver.ATRIBUTO_SESSAO)).isEqualTo("pt-BR");
    }

    @Test
    void destinoDeVoltaSoAceitaCaminhosDoProprioSite() {
        assertThat(IdiomaController.destinoSeguro("/menu?view=todas")).isEqualTo("/menu?view=todas");
        assertThat(IdiomaController.destinoSeguro("//site-malicioso.com")).isEqualTo("/");
        assertThat(IdiomaController.destinoSeguro("https://site-malicioso.com")).isEqualTo("/");
        assertThat(IdiomaController.destinoSeguro("/\\site-malicioso.com")).isEqualTo("/");
        assertThat(IdiomaController.destinoSeguro(null)).isEqualTo("/");
    }

    @Test
    void javascriptDeTextosTemOIdiomaDaRequisicaoENaoFicaEmCache() throws Exception {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));

        ResponseEntity<String> resposta = controller(mock(UsuarioRepository.class)).mensagensJs();

        assertThat(resposta.getBody()).startsWith("window.I18N=").contains("\"js.aviso.cancelar\":\"Cancelar\"")
                .contains("window.t=function").contains("window.trocarIdioma=function").contains("todaily.idioma.campos");
        assertThat(resposta.getBody()).contains("\"js.acess.tema\":\"Tema\"").doesNotContain("msg.");
        assertThat(resposta.getHeaders().getCacheControl()).contains("no-store");
    }
}
