package com.example.demo.Entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// Foto de perfil do usuário. Fica numa tabela própria, e não em Usuario, para que
// os bytes da imagem não sejam carregados toda vez que um Usuario é lido (participantes
// de áreas, responsáveis de tarefas etc.). O ON DELETE CASCADE apaga a foto junto com
// a conta, sem exigir mudança no fluxo de exclusão de usuário.
@Entity
public class FotoPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    private String tipoConteudo;

    // Momento da última alteração (epoch em ms); serve de versão para o cache do navegador.
    private long atualizadaEm;

    @Lob
    @JsonIgnore
    @Column(length = 16777215) // MEDIUMBLOB
    private byte[] dados;

    public Long getId() { return id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getTipoConteudo() { return tipoConteudo; }
    public void setTipoConteudo(String tipoConteudo) { this.tipoConteudo = tipoConteudo; }

    public long getAtualizadaEm() { return atualizadaEm; }
    public void setAtualizadaEm(long atualizadaEm) { this.atualizadaEm = atualizadaEm; }

    public byte[] getDados() { return dados; }
    public void setDados(byte[] dados) { this.dados = dados; }
}
