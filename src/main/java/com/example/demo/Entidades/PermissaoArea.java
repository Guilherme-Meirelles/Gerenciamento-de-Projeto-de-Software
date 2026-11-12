package com.example.demo.Entidades;

public enum PermissaoArea {
    ADMIN,
    EDITOR,
    OBSERVADOR;

    // Níveis que um convite (por e-mail ou por link) pode conceder. ADMIN fica de fora
    // de propósito: o administrador é quem criou a área, e só ele gerencia membros e a exclui.
    // Retorna null para valor ausente, inválido ou ADMIN.
    public static PermissaoArea doConvite(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            PermissaoArea nivel = valueOf(valor.trim().toUpperCase());
            return nivel == ADMIN ? null : nivel;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}