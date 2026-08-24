package com.example.demo.Serviços.Autentificador;

import com.example.demo.Serviços.CookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // Verifica se há cookie de usuário
        String usuarioId = CookieService.getCookie(request, "usuarioId");
        String usuarioEmail = CookieService.getCookie(request, "emailUsuario");

        String url = request.getRequestURI();

        if (usuarioId != null) {
            return true; // ✅ Usuário autenticado, pode prosseguir
        }

        if ((url.equals("/redefinicaoSenha") || url.equals("/redefinirSenha")) && usuarioEmail != null) {
            return true;
        }
        // ❌ Não autenticado, redireciona para login
        response.sendRedirect("/login");
        return false;
    }
}
