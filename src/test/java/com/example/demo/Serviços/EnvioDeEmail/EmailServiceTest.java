package com.example.demo.Serviços.EnvioDeEmail;

import com.example.demo.Entidades.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceTest {

    private static final String[] TEMPLATES = {
            "templates/email_template.html",
            "templates/verificacaoEmail.html",
            "templates/lembreteTarefa.html",
            "templates/compartilhamentoAreaTrabalho.html"
    };

    private static String carregar(String arquivo) throws Exception {
        return new String(new ClassPathResource(arquivo).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void todosOsTemplatesTemTodosOsTextosNosTresIdiomas() throws Exception {
        for (String idioma : new String[]{"pt-BR", "en", "es"}) {
            for (String arquivo : TEMPLATES) {
                String html = EmailService.traduzirTemplate(carregar(arquivo), Locale.forLanguageTag(idioma));
                assertThat(html).as(arquivo + " em " + idioma).doesNotContain("{{").doesNotContain("{lang}").doesNotContainPattern("email\\.(recup|verif|lembrete|compart|ola|formatoData)");
                assertThat(html).contains("lang=\"" + Locale.forLanguageTag(idioma).toLanguageTag() + "\"");
            }
        }
    }

    @Test
    void templateDeVerificacaoSaiNoIdiomaPedido() throws Exception {
        String pt = EmailService.traduzirTemplate(carregar("templates/verificacaoEmail.html"), Locale.forLanguageTag("pt-BR"));
        String en = EmailService.traduzirTemplate(carregar("templates/verificacaoEmail.html"), Locale.ENGLISH);
        String es = EmailService.traduzirTemplate(carregar("templates/verificacaoEmail.html"), Locale.forLanguageTag("es"));

        assertThat(pt).contains("Olá, <strong>{nome}</strong>").contains("Confirmar E-mail");
        assertThat(en).contains("Hello, <strong>{nome}</strong>").contains("Confirm Email");
        assertThat(es).contains("Hola, <strong>{nome}</strong>").contains("Confirmar correo");
    }

    @Test
    void idiomaDoUsuarioUsaOPerfilEPortuguesQuandoVazio() {
        Usuario semIdioma = new Usuario();
        Usuario ingles = new Usuario();
        ingles.setIdioma("en");

        assertThat(EmailService.idiomaDoUsuario(semIdioma)).isEqualTo(Locale.forLanguageTag("pt-BR"));
        assertThat(EmailService.idiomaDoUsuario(ingles).getLanguage()).isEqualTo("en");
    }
}
