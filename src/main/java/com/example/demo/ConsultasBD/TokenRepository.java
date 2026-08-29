package com.example.demo.ConsultasBD;

import com.example.demo.Entidades.Token;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Token findByToken(String token);
}