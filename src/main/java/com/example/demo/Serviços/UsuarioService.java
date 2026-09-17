package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public void excluirConta(Usuario usuario) {
        List<Tarefa> tarefasAtribuidas = tarefaRepository.findByResponsaveisId(usuario.getId());
        for (Tarefa tarefa : tarefasAtribuidas) {
            tarefa.getResponsaveis().remove(usuario);
        }

        usuarioRepository.delete(usuario);
    }
}
