package com.example.demo.ConsultasBD;

import com.example.demo.Entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Usuario findUsuarioById(Long id);
    Usuario findByEmail(String email);
    Usuario findByEmailAndSenha(String email, String senha);
}