package com.example.demo.Controles;

import com.example.demo.ConsultasBD.FotoPerfilRepository;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.FotoPerfil;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

// Foto de perfil do usuário autenticado. Cada pessoa só enxerga e altera a própria foto:
// o id vem sempre da sessão, nunca da requisição.
@RestController
public class FotoPerfilController {

    private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024; // 5 MB

    @Autowired
    private FotoPerfilRepository fotoPerfilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/perfil/foto")
    public ResponseEntity<byte[]> obter(HttpServletRequest request) {
        Long usuarioId = usuarioLogado(request);
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return fotoPerfilRepository.findByUsuarioId(usuarioId)
                .map(foto -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(foto.getTipoConteudo()))
                        .header("X-Content-Type-Options", "nosniff")
                        // O navegador guarda a imagem, mas confere a versão a cada uso.
                        .cacheControl(CacheControl.noCache().cachePrivate())
                        .eTag("\"" + foto.getAtualizadaEm() + "\"")
                        .body(foto.getDados()))
                .orElseGet(() -> ResponseEntity.noContent().build()); // sem foto: 204, sem erro no console
    }

    @PostMapping("/perfil/foto")
    @Transactional
    public ResponseEntity<?> salvar(@RequestParam("foto") MultipartFile arquivo, HttpServletRequest request) {
        Long usuarioId = usuarioLogado(request);
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (arquivo == null || arquivo.isEmpty()) {
            return ResponseEntity.badRequest().body("Nenhuma imagem enviada.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            return ResponseEntity.badRequest().body("A imagem deve ter no máximo 5 MB.");
        }

        byte[] dados;
        try {
            dados = arquivo.getBytes();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao ler a imagem.");
        }

        // O tipo é deduzido do conteúdo do arquivo, não do que o cliente declara,
        // e só aceita formatos de imagem comuns (SVG fica de fora de propósito).
        String tipo = detectarTipoImagem(dados);
        if (tipo == null) {
            return ResponseEntity.badRequest().body("Formato inválido. Use uma imagem JPG, PNG, GIF ou WebP.");
        }

        Usuario usuario = usuarioRepository.findUsuarioById(usuarioId);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        FotoPerfil foto = fotoPerfilRepository.findByUsuarioId(usuarioId).orElseGet(FotoPerfil::new);
        foto.setUsuario(usuario);
        foto.setTipoConteudo(tipo);
        foto.setDados(dados);
        foto.setAtualizadaEm(System.currentTimeMillis());
        fotoPerfilRepository.save(foto);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/perfil/foto")
    @Transactional
    public ResponseEntity<?> remover(HttpServletRequest request) {
        Long usuarioId = usuarioLogado(request);
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        fotoPerfilRepository.deleteByUsuarioId(usuarioId);
        return ResponseEntity.noContent().build();
    }

    private Long usuarioLogado(HttpServletRequest request) {
        String id = SessaoUtil.getUsuarioId(request);
        return id != null ? Long.parseLong(id) : null;
    }

    // Reconhece JPEG, PNG, GIF e WebP pelos primeiros bytes (assinatura) do arquivo.
    static String detectarTipoImagem(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return "image/png";
        }
        if (d.length >= 6 && d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8'
                && (d[4] == '7' || d[4] == '9') && d[5] == 'a') {
            return "image/gif";
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
