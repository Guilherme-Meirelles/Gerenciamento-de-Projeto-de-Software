package com.example.demo.Entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String descricao;
    private Integer cor;
    private LocalDate dataFim;
    private Boolean status;
    private Boolean notificacoes;
    private Boolean lembreteEnviado;

    // Repetição automática (dia/mês/ano): ao concluir uma tarefa repetitiva, o
    // TarefaService avança a dataFim em vez de deixar a tarefa marcada como concluída.
    @Enumerated(EnumType.STRING)
    private Repeticao repeticao;

    @ManyToOne
    @JoinColumn(name = "lista_id")
    private Lista listaOrigem;

    // Criado sob demanda quando o primeiro item é adicionado.
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "checklist_id")
    private Checklist checklist;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "categorias_tarefa",
        joinColumns = @JoinColumn(name="tarefa_id"),
        inverseJoinColumns = @JoinColumn(name="categoria_id")
    )
    private Set<Categoria> categorias = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "responsaveis_tarefa",
        joinColumns = @JoinColumn(name = "tarefa_id"),
        inverseJoinColumns = @JoinColumn(name="usuario_id")
    )
    private Set<Usuario> responsaveis = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "tarefa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Anexo> anexos = new ArrayList<>();

    public Long getId() { return id;}

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Integer getCor() { return cor; }
    public void setCor(Integer cor) { this.cor = cor; }

    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }

    public Boolean getNotificacoes() { return notificacoes; }
    public void setNotificacoes(Boolean not) { this.notificacoes = not; }

    // Evita mandar o e-mail de lembrete mais de uma vez pra mesma tarefa.
    public Boolean getLembreteEnviado() { return lembreteEnviado; }
    public void setLembreteEnviado(Boolean lembreteEnviado) { this.lembreteEnviado = lembreteEnviado; }

    public Repeticao getRepeticao() { return repeticao; }
    public void setRepeticao(Repeticao repeticao) { this.repeticao = repeticao; }

    public LocalDate getDataFim() {return dataFim;}
    public void setDataFim(LocalDate dat) {this.dataFim = dat;}

    public Lista getListaOrigem() { return listaOrigem; }
    public void setListaOrigem(Lista list) {this.listaOrigem = list;}

    public Checklist getChecklist() { return checklist; }
    public void setChecklist(Checklist checklist) { this.checklist = checklist; }

    public Set<Categoria> getCategorias() { return categorias; }
    public void setCategorias(Set<Categoria> categorias) { this.categorias = categorias; }

    public Set<Usuario> getResponsaveis() { return responsaveis; }
    // Set.of(u) geraria uma coleção imutável neste campo, que o resto do
    // código (ex: TarefaService.editarTarefa) trata como mutável (.clear()/.add()).
    public void setResponsaveis(Usuario u) {
        Set<Usuario> novo = new HashSet<>();
        if (u != null) {
            novo.add(u);
        }
        this.responsaveis = novo;
    }

    public List<Anexo> getAnexos() { return anexos; }
}
