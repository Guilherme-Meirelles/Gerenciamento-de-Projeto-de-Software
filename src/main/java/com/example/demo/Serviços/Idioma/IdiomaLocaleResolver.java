package com.example.demo.Serviços.Idioma;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/**
 * Decide o idioma de cada requisição: primeiro o da sessão (definido no login a partir do perfil, ou ao trocar o
 * idioma), depois o do cookie (a escolha feita antes do login) e, por fim, o português.
 * O nome do bean precisa ser "localeResolver" para o Spring MVC usá-lo.
 */
@Component("localeResolver")
public class IdiomaLocaleResolver implements LocaleResolver {

    public static final String ATRIBUTO_SESSAO = "idioma";
    public static final String COOKIE = "todaily_idioma";

    public static Idioma sessao(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        Object valor = sessao == null ? null : sessao.getAttribute(ATRIBUTO_SESSAO);
        return valor == null ? null : Idioma.de(valor.toString());
    }

    public static Idioma cookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (COOKIE.equals(c.getName())) return Idioma.de(c.getValue());
        }
        return null;
    }

    /** Idioma em vigor para a requisição. */
    public static Idioma idiomaDa(HttpServletRequest request) {
        Idioma daSessao = sessao(request);
        if (daSessao != null) return daSessao;
        Idioma doCookie = cookie(request);
        return doCookie != null ? doCookie : Idioma.PADRAO;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return idiomaDa(request).locale();
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        Idioma idioma = Idioma.deOuPadrao(locale == null ? null : locale.toLanguageTag());
        request.getSession(true).setAttribute(ATRIBUTO_SESSAO, idioma.codigo());
    }
}
