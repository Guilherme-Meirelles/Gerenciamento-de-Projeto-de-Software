package com.example.demo.Serviços;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Registra no console as requisições que demoram mais de {@value #LIMITE_MS} ms, para achar o que está lento.
 * Exemplo de linha: "Requisição lenta: POST /tarefas respondeu 200 em 2910 ms".
 */
@Component
public class RequisicoesLentasFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequisicoesLentasFilter.class);
    static final long LIMITE_MS = 700;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            if (ms >= LIMITE_MS) {
                LOG.warn("Requisição lenta: {} {} respondeu {} em {} ms",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), ms);
            }
        }
    }
}
