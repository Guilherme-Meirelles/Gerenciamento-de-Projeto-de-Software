package com.example.demo.Entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Checklist {
    /*
     * id : Long
     * REL: tarefa : Tarefa
     * REL: itens : List<ItemChecklist>
     */

    // CAMPOS DE CHECKLIST

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RELACIONAMENTOS DE CHECKLIST

    // checklist(1)-(1)tarefa
    // tarefa é dona da relação (coluna checklist_id na tabela tarefa)
    @JsonIgnore
    @OneToOne(mappedBy = "checklist")
    private Tarefa tarefa;

    // checklist(1)-(n)itemChecklist
    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemChecklist> itens = new ArrayList<>();

    // GETTERS E SETTERS

    public Long getId() { return id; }

    public Tarefa getTarefa() { return tarefa; }
    public void setTarefa(Tarefa tarefa) { this.tarefa = tarefa; }

    public List<ItemChecklist> getItens() { return itens; }
}
