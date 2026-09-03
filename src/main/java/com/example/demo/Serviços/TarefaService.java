package com.example.demo.Serviços;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.ConsultasBD.CategoriaRepository;
import com.example.demo.ConsultasBD.ListaRepository;
import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.Categoria;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;

@Service
public class TarefaService {

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private ListaRepository listaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    // ------------------------
    // CRIAR
    // ------------------------
    public Tarefa criarTarefa(Long listaId, String titulo, String descricao, Integer cor,
                              String dataFim, Long responsavelId, Boolean notificacoes,
                              List<Long> categoriaIds) {

        Lista lista = listaRepository.findById(listaId)
                .orElseThrow(() -> new RuntimeException("Lista não encontrada"));

        Tarefa tarefa = new Tarefa();
        tarefa.setListaOrigem(lista);
        tarefa.setTitulo(titulo);
        tarefa.setDescricao(descricao);
        tarefa.setCor(cor);
        tarefa.setNotificacoes(notificacoes != null ? notificacoes : false);
        tarefa.setStatus(false);

        if (dataFim != null && !dataFim.isBlank()) {
            tarefa.setDataFim(LocalDate.parse(dataFim)); // LocalDate <---------
        }

        if (responsavelId != null) {
            Usuario u = usuarioRepository.findById(responsavelId)
                    .orElseThrow(() -> new RuntimeException("Responsável não encontrado"));
            tarefa.setResponsaveis(u);
        }

        aplicarCategorias(tarefa, categoriaIds);

        return tarefaRepository.save(tarefa);
    }

    // ------------------------
    // EDITAR
    // ------------------------
    public Tarefa editarTarefa(Long id, Long listaId, String titulo, String descricao, Integer cor,
                               String dataFim, Long responsavelId, Boolean notificacoes,
                               List<Long> categoriaIds) {

        Tarefa tarefa = tarefaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarefa não encontrada"));

        Lista lista = listaRepository.findById(listaId)
                .orElseThrow(() -> new RuntimeException("Lista não encontrada"));

        tarefa.setListaOrigem(lista);
        tarefa.setTitulo(titulo);
        tarefa.setDescricao(descricao);
        tarefa.setCor(cor);
        tarefa.setNotificacoes(notificacoes != null ? notificacoes : false);

        if (dataFim != null && !dataFim.isBlank()) {
            tarefa.setDataFim(LocalDate.parse(dataFim));
        } else {
            tarefa.setDataFim(null);
        }

        // Atualizar responsáveis
        tarefa.getResponsaveis().clear();
        if (responsavelId != null) {
            Usuario u = usuarioRepository.findById(responsavelId)
                    .orElseThrow(() -> new RuntimeException("Responsável não encontrado"));
            tarefa.getResponsaveis().add(u);
        }

        aplicarCategorias(tarefa, categoriaIds);

        return tarefaRepository.save(tarefa);
    }

    private void aplicarCategorias(Tarefa tarefa, List<Long> categoriaIds) {
        if (categoriaIds == null) {
            return; // nenhuma alteração solicitada nas categorias
        }
        Set<Categoria> categorias = new HashSet<>();
        for (Long categoriaId : categoriaIds) {
            Categoria categoria = categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
            categorias.add(categoria);
        }
        tarefa.setCategorias(categorias);
    }

    public Tarefa toggleTarefa(Long id, Boolean concluida) {

        Tarefa tarefa = tarefaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarefa não encontrada"));

        tarefa.setStatus(concluida);

        return tarefaRepository.save(tarefa);
    }

    // ------------------------
    // LISTAR POR LISTA
    // ------------------------
    public List<Tarefa> listarTarefasPorLista(Long listaId) {
        return tarefaRepository.findByListaOrigemId(listaId);
    }

    // ------------------------
    // DELETAR
    // ------------------------
    public void remover(Long id) {
        tarefaRepository.deleteById(id);
    }
}