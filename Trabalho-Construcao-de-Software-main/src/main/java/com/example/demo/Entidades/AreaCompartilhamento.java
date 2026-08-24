package com.example.demo.Entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AreaCompartilhamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long idArea;

    private String nomeArea;

    private String emailUsuario;

    private String nomeUsuario;

    public AreaCompartilhamento() {}

    public AreaCompartilhamento(Long idArea, String nomeArea, String emailUsuario, String nomeUsuario) {
        this.idArea = idArea;
        this.nomeArea = nomeArea;
        this.emailUsuario = emailUsuario;
        this.nomeUsuario = nomeUsuario;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getIdArea() {
        return idArea;
    }
    public void setIdArea(Long idArea) {
        this.idArea = idArea;
    }
    public String getNomeArea() {
        return nomeArea;
    }
    public void setNomeArea(String nomeArea) {
        this.nomeArea = nomeArea;
    }
    public String getEmailUsuario() {
        return emailUsuario;
    }
    public void setEmailUsuario(String emailUsuario) {
        this.emailUsuario = emailUsuario;
    }
    public String getNomeUsuario() {
        return nomeUsuario;
    }
    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }
}
