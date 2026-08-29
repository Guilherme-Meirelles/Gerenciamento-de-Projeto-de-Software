package com.example.demo.Serviços.Autentificador;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

// Guarda o usuário autenticado na sessão do servlet (opaca e assinada pelo
// próprio container via JSESSIONID) em vez de um cookie de texto puro que
// qualquer um podia forjar (ex: usuarioId=1) para virar outro usuário.
public class SessaoUtil {

    private static final String ATRIBUTO_USUARIO_ID = "usuarioId";

    public static void autenticar(HttpServletRequest request, Long usuarioId) {
        HttpSession session = request.getSession(true);
        session.setAttribute(ATRIBUTO_USUARIO_ID, usuarioId);
    }

    public static String getUsuarioId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object valor = session.getAttribute(ATRIBUTO_USUARIO_ID);
        return valor != null ? valor.toString() : null;
    }

    public static void encerrar(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
