package com.example.demo.Entidades;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Categoria {
    /*
     * id : Long
     * nome : String
     * cor : String
     * REL: tarefas : Set<Tarefa>
     */

    // CAMPOS DE CATEGORIA

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String cor;

    // RELACIONAMENTOS DE CATEGORIA
    // categoria(n)-(n)tarefa
    @ManyToMany(mappedBy = "categorias")
    private Set<Tarefa> tarefas = new HashSet<>();

    //GETTERS E SETTERS
    // id
    public Long getId() { return id; }
    // nome
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    // cor
    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }
    // tarefas
    public Set<Tarefa> getTarefa() { return tarefas; }
}
