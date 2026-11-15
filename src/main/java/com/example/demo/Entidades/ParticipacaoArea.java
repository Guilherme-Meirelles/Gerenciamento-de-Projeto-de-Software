package com.example.demo.Entidades;

import jakarta.persistence.*;

@Entity
public class ParticipacaoArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "area_id")
    private AreaTrabalho area;

    @Enumerated(EnumType.STRING)
    private PermissaoArea permissao;

    // Posição da área na tela "Áreas de Trabalho" deste usuário (ordem manual, por arrastar). Nula = ainda não ordenada.
    private Integer posicao;

    public ParticipacaoArea() {}

    public ParticipacaoArea(Usuario usuario, AreaTrabalho area, PermissaoArea permissao) {
        this.usuario = usuario;
        this.area = area;
        this.permissao = permissao;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public AreaTrabalho getArea() { return area; }
    public PermissaoArea getPermissao() { return permissao; }
    public void setPermissao(PermissaoArea permissao) { this.permissao = permissao; }
    public Integer getPosicao() { return posicao; }
    public void setPosicao(Integer posicao) { this.posicao = posicao; }
    public void setUsuario(Usuario usuario) {this.usuario = usuario;}
    public void setArea(AreaTrabalho area) {this.area = area;}

}
