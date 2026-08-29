package com.example.demo.Entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private String email;
    private LocalDateTime expiraEm;
    private boolean usado;

    // getters e setters
    public Token() {}
    public Token(String token, String email, LocalDateTime expiraEm, boolean usado) {
        this.token = token;
        this.email = email;
        this.expiraEm = expiraEm;
        this.usado = usado;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public String getToken() {
        return token;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getEmail() {
        return email;
    }
    public void setExpiraEm(LocalDateTime expiraEm) {
        this.expiraEm = expiraEm;
    }
    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }
    public void setUsado(boolean usado) {
        this.usado = usado;
    }
    public boolean isUsado() {
        return usado;
    }
}
