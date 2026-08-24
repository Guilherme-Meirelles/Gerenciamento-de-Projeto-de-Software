package com.example.demo.Entidades;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Lista {

    /*
     * id : Long
     * nome : String
     * descricao : String
     * REL: areaMae : AreaTrabalho
     * REL: tarefas : Set<Tarefa>
     */

    // CAMPOS DE LISTA

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String descricao;

    // RELACIONAMENTOS DE LISTA

    // lista(n)-(1)areaTrabalho
    // area de trabalho é dona da relação
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private AreaTrabalho area;

    // lista(1)-(n)tarefa
    // lista é dona da relação
    @OneToMany(mappedBy="listaOrigem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Tarefa> tarefas = new HashSet<>();
    
    // GETTERS E SETTERS
    // id
    public Long getId() { return id; }

    // Nome
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    // descricao
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    // areaMae
    public AreaTrabalho getArea() { return area; }
    public void setArea(AreaTrabalho a) { area = a; }

    // tarefas
    public Set<Tarefa> getTarefas() { return tarefas; }
}
