package com.example.demo.Controles;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.ConsultasBD.CategoriaRepository;
import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.Entidades.Categoria;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.CategoriaService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ParticipacaoAreaRepository participacaoAreaRepository;

    // Confere se o usuário autenticado participa da área informada.
    private Long usuarioComAcessoAArea(Long areaId, HttpServletRequest request) {
        String usuarioIdStr = SessaoUtil.getUsuarioId(request);
        if (usuarioIdStr == null) return null;
        Long usuarioId = Long.parseLong(usuarioIdStr);

        boolean temAcesso = participacaoAreaRepository.existsByUsuarioIdAndAreaId(usuarioId, areaId);
        return temAcesso ? usuarioId : null;
    }

    // Confere se o usuário autenticado participa da área dona da categoria informada.
    private Long usuarioComAcessoACategoria(Long categoriaId, HttpServletRequest request) {
        Categoria categoria = categoriaRepository.findById(categoriaId).orElse(null);
        if (categoria == null) return null;
        return usuarioComAcessoAArea(categoria.getArea().getId(), request);
    }

    @PostMapping
    public ResponseEntity<?> criarCategoria(
            @RequestParam Long areaId,
            @RequestParam String nome,
            @RequestParam String cor,
            HttpServletRequest request
    ) {
        if (usuarioComAcessoAArea(areaId, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Categoria categoria = categoriaService.criarCategoria(areaId, nome, cor);
        return ResponseEntity.ok(categoria);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarCategoria(
            @PathVariable Long id,
            @RequestParam String nome,
            @RequestParam String cor,
            HttpServletRequest request
    ) {
        if (usuarioComAcessoACategoria(id, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Categoria categoria = categoriaService.editarCategoria(id, nome, cor);
        return ResponseEntity.ok(categoria);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removerCategoria(@PathVariable Long id, HttpServletRequest request) {
        if (usuarioComAcessoACategoria(id, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        categoriaService.remover(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/area/{areaId}")
    public ResponseEntity<?> listarPorArea(@PathVariable Long areaId, HttpServletRequest request) {
        if (usuarioComAcessoAArea(areaId, request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Categoria> categorias = categoriaService.listarPorArea(areaId);
        return ResponseEntity.ok(categorias);
    }
}
