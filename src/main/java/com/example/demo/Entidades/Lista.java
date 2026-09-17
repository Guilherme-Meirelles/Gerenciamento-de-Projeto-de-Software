package com.example.demo.Entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Lista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private AreaTrabalho area;

    // @JsonIgnore evita o ciclo Lista -> tarefas -> Tarefa -> listaOrigem -> ...
    @JsonIgnore
    @OneToMany(mappedBy="listaOrigem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Tarefa> tarefas = new HashSet<>();

    public Long getId() { return id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public AreaTrabalho getArea() { return area; }
    public void setArea(AreaTrabalho a) { area = a; }

    public Set<Tarefa> getTarefas() { return tarefas; }
}
