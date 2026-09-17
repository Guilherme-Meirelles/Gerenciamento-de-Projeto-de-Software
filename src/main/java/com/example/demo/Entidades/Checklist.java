package com.example.demo.Entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Checklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tarefa é dona da relação (coluna checklist_id na tabela tarefa)
    @JsonIgnore
    @OneToOne(mappedBy = "checklist")
    private Tarefa tarefa;

    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemChecklist> itens = new ArrayList<>();

    public Long getId() { return id; }

    public Tarefa getTarefa() { return tarefa; }
    public void setTarefa(Tarefa tarefa) { this.tarefa = tarefa; }

    public List<ItemChecklist> getItens() { return itens; }
}
