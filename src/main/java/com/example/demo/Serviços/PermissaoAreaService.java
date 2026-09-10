package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Centraliza a checagem do nível de PermissaoArea (ADMIN/EDITOR/OBSERVADOR)
// de um usuário numa área, para os controllers não repetirem a mesma lógica
// e sempre exigirem o nível certo antes de liberar leitura vs. escrita.
@Service
public class PermissaoAreaService {

    @Autowired
    private ParticipacaoAreaRepository participacaoAreaRepository;

    // Observador, Editor ou Administrador: basta participar da área pra ler.
    public boolean podeVisualizar(Long usuarioId, Long areaId) {
        return participacaoAreaRepository.existsByUsuarioIdAndAreaId(usuarioId, areaId);
    }

    // Editor ou Administrador: qualquer ação que crie/edite/apague dados da área
    // (listas, tarefas, categorias, checklist, anexos etc.). Observador é read-only.
    public boolean podeEditar(Long usuarioId, Long areaId) {
        PermissaoArea permissao = getPermissao(usuarioId, areaId);
        return permissao != null && permissao != PermissaoArea.OBSERVADOR;
    }

    // Só Administrador: ações de gerenciamento da própria área (convidar/remover
    // membro, gerar link de convite, excluir a área etc.).
    public boolean podeAdministrar(Long usuarioId, Long areaId) {
        return getPermissao(usuarioId, areaId) == PermissaoArea.ADMIN;
    }

    public PermissaoArea getPermissao(Long usuarioId, Long areaId) {
        ParticipacaoArea participacao = participacaoAreaRepository.findByUsuarioIdAndAreaId(usuarioId, areaId);
        return participacao != null ? participacao.getPermissao() : null;
    }
}
