package com.example.demo.Controles;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FotoPerfilControllerTest {

    @Test
    void reconheceJpeg() {
        byte[] dados = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
        assertEquals("image/jpeg", FotoPerfilController.detectarTipoImagem(dados));
    }

    @Test
    void reconhecePng() {
        byte[] dados = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};
        assertEquals("image/png", FotoPerfilController.detectarTipoImagem(dados));
    }

    @Test
    void reconheceGif() {
        assertEquals("image/gif", FotoPerfilController.detectarTipoImagem("GIF89a....".getBytes()));
    }

    @Test
    void reconheceWebp() {
        assertEquals("image/webp", FotoPerfilController.detectarTipoImagem("RIFF....WEBPVP8 ".getBytes()));
    }

    @Test
    void rejeitaSvgTextoEArquivoVazio() {
        assertNull(FotoPerfilController.detectarTipoImagem("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes()));
        assertNull(FotoPerfilController.detectarTipoImagem("texto qualquer".getBytes()));
        assertNull(FotoPerfilController.detectarTipoImagem(new byte[0]));
    }
}
