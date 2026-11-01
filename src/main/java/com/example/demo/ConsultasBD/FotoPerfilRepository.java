package com.example.demo.ConsultasBD;

import com.example.demo.Entidades.FotoPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FotoPerfilRepository extends JpaRepository<FotoPerfil, Long> {
    Optional<FotoPerfil> findByUsuarioId(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);
}
