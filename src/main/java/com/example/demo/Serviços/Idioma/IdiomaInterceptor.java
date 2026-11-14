package com.example.demo.Serviços.Idioma;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Mantém o cookie de idioma igual ao da sessão. Assim, depois de sair da conta (a sessão acaba) a tela de login
 * continua no último idioma usado naquele navegador.
 */
@Component
public class IdiomaInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Idioma daSessao = IdiomaLocaleResolver.sessao(request);
        if (daSessao != null && daSessao != IdiomaLocaleResolver.cookie(request)) {
            gravarCookie(response, daSessao);
        }
        return true;
    }

    public static void gravarCookie(HttpServletResponse response, Idioma idioma) {
        ResponseCookie cookie = ResponseCookie.from(IdiomaLocaleResolver.COOKIE, idioma.codigo())
                .path("/")
                .maxAge(Duration.ofDays(365))
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
