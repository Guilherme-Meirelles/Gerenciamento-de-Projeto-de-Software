package com.example.demo.Serviços.Idioma;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Textos do sistema em vários idiomas (arquivos mensagens.properties, mensagens_en.properties e
 * mensagens_es.properties). Dentro de uma requisição usa o idioma escolhido pelo usuário; fora dela (testes,
 * tarefas agendadas) usa o português.
 *
 * Os textos que levam parâmetros ({0}, {1}) passam por MessageFormat: por isso nos arquivos não se usa o
 * apóstrofo comum (') e sim o tipográfico (’).
 */
public final class Mensagens {

    private static final String ARQUIVOS = "mensagens";
    private static final ResourceBundleMessageSource FONTE = new ResourceBundleMessageSource();

    static {
        FONTE.setBasename(ARQUIVOS);
        FONTE.setDefaultEncoding("UTF-8");
        FONTE.setFallbackToSystemLocale(false);
        FONTE.setUseCodeAsDefaultMessage(true);
    }

    private Mensagens() {}

    /** Idioma da requisição em andamento; sem requisição (testes, jobs) é o português. */
    public static Locale localeAtual() {
        LocaleContext contexto = LocaleContextHolder.getLocaleContext();
        Locale locale = contexto == null ? null : contexto.getLocale();
        return locale != null ? locale : Idioma.PADRAO.locale();
    }

    public static String get(String chave, Object... argumentos) {
        return get(localeAtual(), chave, argumentos);
    }

    public static String get(Locale locale, String chave, Object... argumentos) {
        return FONTE.getMessage(chave, argumentos, locale);
    }

    /** Todos os textos cujas chaves começam com o prefixo, já no idioma pedido (usado pelo JavaScript). */
    public static Map<String, String> comPrefixo(Locale locale, String prefixo) {
        ResourceBundle pacote = ResourceBundle.getBundle(ARQUIVOS, locale,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        Map<String, String> mapa = new LinkedHashMap<>();
        pacote.keySet().stream().filter(c -> c.startsWith(prefixo)).sorted()
                .forEach(c -> mapa.put(c, pacote.getString(c)));
        return mapa;
    }
}
