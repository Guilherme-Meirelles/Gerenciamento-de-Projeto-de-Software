package com.example.demo.Serviços;

/**
 * Texto maior que o limite permitido (título, nome, descrição...). Os controladores respondem 400 com a
 * mensagem, que já vem no idioma do usuário.
 */
public class TextoMuitoLongoException extends IllegalArgumentException {
    public TextoMuitoLongoException(String mensagem) {
        super(mensagem);
    }
}
