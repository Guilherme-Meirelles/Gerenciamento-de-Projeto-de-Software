package com.example.demo.Entidades;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Tarefa {
    /*
     * id : Int
     * titulo : String
     * descricao : String
     * cor : Int
     * <Data e Hora> : String
     * <Anexos> : String
     * status : Boolean
     * REL: listaOrigem : Set<Lista>
     * REL: categorias : Set<Categoria>
     * REL: responsaveis : Set<Usuario>
     */

    // CAMPOS DE TAREFA

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String descricao;
    private Integer cor;
    private LocalDate dataFim;
    private Boolean status;
    private Boolean notificacoes;

    // RELACIONAMENTOS DE TAREFA

    // tarefa(n)-(1)lista
    // uma lista pode ter várias tarefas mas cada tarefa é de apenas 1 lista.
    @ManyToOne
    @JoinColumn(name = "lista_id")
    private Lista listaOrigem;

    // tarefa(n)-(n)categoria 
    // criar uma nova tabela
    @ManyToMany
    @JoinTable(
        name = "categorias_tarefa",
        joinColumns = @JoinColumn(name="tarefa_id"),
        inverseJoinColumns = @JoinColumn(name="categoria_id")
    )
    private Set<Categoria> categorias = new HashSet<>();

    // tarefa(n)-(n)usuario | RESPONSÁVEIS
    // criar uma nova tabela "responsaveis_tarefa"
    @ManyToMany
    @JoinTable(
        name = "responsaveis_tarefa",
        joinColumns = @JoinColumn(name = "tarefa_id"),
        inverseJoinColumns = @JoinColumn(name="usuario_id")
    )
    private Set<Usuario> responsaveis = new HashSet<>();

    // GETTERS E SETTERS
    public Long getId() { return id;}

    // titulo
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; } 

    // descricao
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    // cor
    public Integer getCor() { return cor; }
    public void setCor(Integer cor) { this.cor = cor; }

    // status
    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }

    // notificações
    public Boolean getNotificacoes() { return notificacoes; }
    public void setNotificacoes(Boolean not) { this.notificacoes = not; }

    // Date
    public LocalDate getDataFim() {return dataFim;}
    public void setDataFim(LocalDate dat) {this.dataFim = dat;}

    // listaOrigem
    public Lista getListaOrigem() { return listaOrigem; }
    public void setListaOrigem(Lista list) {this.listaOrigem = list;}

    // categorias
    public Set<Categoria> getCategorias() { return categorias; }

    // responsaveis
    public Set<Usuario> getResponsaveis() { return responsaveis; }
    public void setResponsaveis(Usuario u) { this.responsaveis = Set.of(u);}
}
