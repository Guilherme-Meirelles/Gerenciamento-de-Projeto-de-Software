package com.example.demo.Entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class ItemChecklist {
    /*
     * id : Long
     * descricao : String
     * concluido : Boolean
     * REL: checklist : Checklist
     */

    // CAMPOS DE ITEM CHECKLIST

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao;
    private Boolean concluido = false;

    // RELACIONAMENTOS DE ITEM CHECKLIST

    // itemChecklist(n)-(1)checklist
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id", nullable = false)
    private Checklist checklist;

    // GETTERS E SETTERS

    public Long getId() { return id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Boolean getConcluido() { return concluido; }
    public void setConcluido(Boolean concluido) { this.concluido = concluido; }

    public Checklist getChecklist() { return checklist; }
    public void setChecklist(Checklist checklist) { this.checklist = checklist; }
}
