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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
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

    // ---------- coerência entre o código e os arquivos de tradução ----------

    private static void chavesUsadas(Path pasta, String extensao, Pattern padrao, Set<String> destino) throws Exception {
        try (Stream<Path> arquivos = Files.walk(pasta)) {
            for (Path arquivo : (Iterable<Path>) arquivos.filter(a -> a.toString().endsWith(extensao))::iterator) {
                Matcher m = padrao.matcher(Files.readString(arquivo, StandardCharsets.UTF_8));
                while (m.find()) destino.add(m.group(1));
            }
        }
    }

    @Test
    void todaChaveUsadaNosTemplatesNoJavaScriptENoJavaExisteNosArquivosDeTraducao() throws Exception {
        Set<String> usadas = new HashSet<>();
        Path recursos = Path.of("src/main/resources");
        Pattern chaveJs = Pattern.compile("\\bt\\(\\s*['\"]([\\w.]+)['\"]");
        chavesUsadas(recursos.resolve("templates"), ".html", Pattern.compile("#\\{([\\w.]+)}"), usadas);
        chavesUsadas(recursos.resolve("templates"), ".html", chaveJs, usadas);
        chavesUsadas(recursos.resolve("static/js"), ".js", chaveJs, usadas);
        chavesUsadas(Path.of("src/main/java"), ".java", Pattern.compile("Mensagens\\.get\\(\\s*\"([\\w.]+)\""), usadas);

        Set<String> existentes = carregar("mensagens.properties").stringPropertyNames();
        usadas.removeIf(k -> !k.contains("."));
        assertThat(usadas).isNotEmpty();
        assertThat(usadas).as("chaves usadas mas ausentes em mensagens.properties").isSubsetOf(existentes);
    }

    @Test
    void textosDeTarefasEmIngles() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(Mensagens.get("msg.tarefa.naoEncontrada")).isEqualTo("Task not found");
        assertThat(Mensagens.get("msg.lista.naoEncontrada")).isEqualTo("List not found");
        assertThat(Mensagens.get("msg.anexo.nenhumArquivo")).isEqualTo("No file uploaded.");
    }

    @Test
    void textosComParametrosDeTarefasNoJavaScriptTemOsMarcadores() {
        for (String idioma : new String[]{"pt-BR", "en", "es"}) {
            Map<String, String> js = Mensagens.comPrefixo(Locale.forLanguageTag(idioma), "js.");
            assertThat(js.get("js.checklist.concluidos")).as(idioma).contains("{0}").contains("{1}");
            assertThat(js.get("js.tarefa.pessoas")).as(idioma).contains("{0}");
            assertThat(js.get("js.cal.verMesDoAno")).as(idioma).contains("{0}").contains("{1}");
        }
    }

    @Test
    void textosDeAreasEConvitesNosTresIdiomas() {
        assertThat(Mensagens.get(Locale.forLanguageTag("pt-BR"), "msg.convite.participa", "Casa"))
                .isEqualTo("Você agora participa da área \"Casa\".");
        assertThat(Mensagens.get(Locale.ENGLISH, "msg.convite.participa", "Casa"))
                .isEqualTo("You are now a member of the workspace \"Casa\".");
        assertThat(Mensagens.get(Locale.forLanguageTag("es"), "msg.convite.linkExpirado"))
                .isEqualTo("Enlace de invitación caducado.");
        Map<String, String> js = Mensagens.comPrefixo(Locale.ENGLISH, "js.");
        assertThat(js.get("js.areas.permissaoAlterada")).contains("{0}").contains("{1}");
        assertThat(js.get("js.arquivo.nenhum")).isEqualTo("No file chosen");
    }
}
