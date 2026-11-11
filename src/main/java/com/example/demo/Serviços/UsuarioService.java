package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.ParticipacaoArea;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;


@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TarefaRepository tarefaRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, TarefaRepository tarefaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tarefaRepository = tarefaRepository;
    }

    // Antes de excluir o usuário, precisa desfazer os vínculos que não são
    // apagados em cascata pelo JPA: tarefas em áreas de terceiros das quais
    // ele é só responsável (tabela de junção responsaveis_tarefa). Áreas das
    // quais ele é dono, e suas participações, já são cascateadas pelas
    // anotações em Usuario (areasCriadas/participacoes).
    @Transactional
    public void excluirConta(Usuario usuarioParam) {
        // 1. Busca a entidade gerenciada no contexto transacional atual
        Usuario usuario = usuarioRepository.findById(usuarioParam.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        // 2. Remove o usuário de todas as tarefas nas quais ele é responsável
        // (Remove registros da tabela intermediária responsaveis_tarefa)
        for (Tarefa tarefa : new HashSet<>(usuario.getTarefasAtribuidas())) {
            tarefa.getResponsaveis().remove(usuario);
        }
        usuario.getTarefasAtribuidas().clear();

        // 3. Desvincula o usuário das participações em áreas de trabalho
        for (ParticipacaoArea participacao : new HashSet<>(usuario.getParticipacoes())) {
            AreaTrabalho area = participacao.getArea();
            if (area != null) {
                area.getParticipacoes().remove(participacao);
            }
        }
        usuario.getParticipacoes().clear();

        // 4. Remove o usuário.
        // O cascade.ALL de 'areasCriadas' removerá automaticamente as áreas das quais
        // ele é dono, bem como as listas, checklists, anexos e participações dessas áreas.
        usuarioRepository.delete(usuario);
    }
}