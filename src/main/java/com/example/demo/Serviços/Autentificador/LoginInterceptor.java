package com.example.demo.Serviços.Autentificador;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (SessaoUtil.getUsuarioId(request) != null) {
            return true; // ✅ Usuário autenticado, pode prosseguir
        }

        // ❌ Não autenticado, redireciona para login
        response.sendRedirect("/login");
        return false;
    }
}
