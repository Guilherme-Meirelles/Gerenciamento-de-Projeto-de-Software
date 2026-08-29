package com.example.demo.Entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Usuario {

    // CAMPOS DE USUÁRIO
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;

    // Nunca deve ir para o JSON de resposta: mesmo o hash não deveria trafegar até o cliente.
    @JsonIgnore
    private String senha;
    private String dataNascimento;
    private boolean emailVerificado = false;

    // RELACIONAMENTOS DE USUÁRIO

    // Áreas que o usuário criou
    // @JsonIgnore evita o ciclo Usuario -> areasCriadas -> AreaTrabalho -> dono -> Usuario -> ...
    @JsonIgnore
    @OneToMany(mappedBy = "dono")
    private Set<AreaTrabalho> areasCriadas = new HashSet<>();

    // Participações do usuário em áreas
    @JsonIgnore
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private Set<ParticipacaoArea> participacoes = new HashSet<>();

    // usuario(n)-(n)tarefa
    @JsonIgnore
    @ManyToMany(mappedBy = "responsaveis")
    private Set<Tarefa> tarefasAtribuidas = new HashSet<>();

    // getters e setters básicos
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(String dataNascimento) { this.dataNascimento = dataNascimento; }
    public boolean isEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(boolean emailVerificado) { this.emailVerificado = emailVerificado; }

    public Set<AreaTrabalho> getAreasCriadas() { return areasCriadas; }
    public Set<ParticipacaoArea> getParticipacoes() { return participacoes; }

    // tarefasAtribuidas
    public Set<Tarefa> getTarefasAtribuidas() { return tarefasAtribuidas; }
}