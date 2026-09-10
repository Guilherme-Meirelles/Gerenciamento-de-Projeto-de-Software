package com.example.demo.Entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Anexo {
    /*
     * id : Long
     * nomeArquivo : String
     * tipoConteudo : String
     * tamanho : Long
     * dados : byte[]
     * REL: tarefa : Tarefa
     */

    // CAMPOS DE ANEXO

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeArquivo;
    private String tipoConteudo;
    private Long tamanho;

    @Lob
    @JsonIgnore
    @Column(length = 16777215) // MEDIUMBLOB
    private byte[] dados;

    // RELACIONAMENTOS DE ANEXO

    // anexo(n)-(1)tarefa
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarefa_id", nullable = false)
    private Tarefa tarefa;

    // GETTERS E SETTERS

    public Long getId() { return id; }

    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }

    public String getTipoConteudo() { return tipoConteudo; }
    public void setTipoConteudo(String tipoConteudo) { this.tipoConteudo = tipoConteudo; }

    public Long getTamanho() { return tamanho; }
    public void setTamanho(Long tamanho) { this.tamanho = tamanho; }

    public byte[] getDados() { return dados; }
    public void setDados(byte[] dados) { this.dados = dados; }

    public Tarefa getTarefa() { return tarefa; }
    public void setTarefa(Tarefa tarefa) { this.tarefa = tarefa; }
}
