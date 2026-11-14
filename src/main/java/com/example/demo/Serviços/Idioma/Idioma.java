package com.example.demo.Serviços.Idioma;

import java.util.Locale;

/** Idiomas em que o sistema pode ser exibido. O padrão (e o que os testes esperam) é o português do Brasil. */
public enum Idioma {
    PT_BR("pt-BR", Locale.forLanguageTag("pt-BR")),
    EN("en", Locale.ENGLISH),
    ES("es", Locale.forLanguageTag("es"));

    public static final Idioma PADRAO = PT_BR;

    private final String codigo;
    private final Locale locale;

    Idioma(String codigo, Locale locale) {
        this.codigo = codigo;
        this.locale = locale;
    }

    /** Código guardado no perfil e no cookie ("pt-BR", "en" ou "es"). */
    public String codigo() { return codigo; }

    public Locale locale() { return locale; }

    /** Aceita "pt", "pt-BR", "pt_br", "EN", "es-MX"...; devolve null se não for um idioma suportado. */
    public static Idioma de(String texto) {
        if (texto == null) return null;
        String t = texto.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (t.equals("pt") || t.startsWith("pt-")) return PT_BR;
        if (t.equals("en") || t.startsWith("en-")) return EN;
        if (t.equals("es") || t.startsWith("es-")) return ES;
        return null;
    }

    /** Como {@link #de(String)}, mas cai no padrão quando o texto é inválido. */
    public static Idioma deOuPadrao(String texto) {
        Idioma i = de(texto);
        return i != null ? i : PADRAO;
    }
}
