package com.example.demo.Entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Categoria {
    /*
     * id : Long
     * nome : String
     * cor : String
     * REL: area : AreaTrabalho
     * REL: tarefas : Set<Tarefa>
     */

    // CAMPOS DE CATEGORIA

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String cor;

    // RELACIONAMENTOS DE CATEGORIA

    // categoria(n)-(1)areaTrabalho
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private AreaTrabalho area;

    // categoria(n)-(n)tarefa
    @JsonIgnore
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
    // area
    public AreaTrabalho getArea() { return area; }
    public void setArea(AreaTrabalho area) { this.area = area; }
    // tarefas
    public Set<Tarefa> getTarefa() { return tarefas; }
}
