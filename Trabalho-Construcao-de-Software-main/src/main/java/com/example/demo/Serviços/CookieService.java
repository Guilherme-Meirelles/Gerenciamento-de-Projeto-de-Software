package com.example.demo.Serviços;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

public class CookieService {

    public static void setCookie(HttpServletResponse response, String key, String valor, int segundos) {
        Cookie cookie = new Cookie(key, URLEncoder.encode(valor, StandardCharsets.UTF_8));
        cookie.setMaxAge(segundos);
        cookie.setPath("/"); // 🔥 torna o cookie visível para toda a aplicação
        response.addCookie(cookie);
    }

    public static String getCookie(HttpServletRequest request, String key) {
        String valor = Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays.stream(cookies)
                        .filter(cookie -> key.equals(cookie.getName()))
                        .findAny())
                .map(Cookie::getValue)
                .orElse(null);

        if (valor != null) {
            return URLDecoder.decode(valor, StandardCharsets.UTF_8);
        }
        return null;
    }

    public static void deleteCookie(HttpServletResponse response, String key) {
        Cookie cookie = new Cookie(key, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}

