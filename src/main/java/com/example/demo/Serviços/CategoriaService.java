package com.example.demo.Serviços;

import com.example.demo.Serviços.Idioma.Mensagens;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ConsultasBD.AreaTrabalhoRepository;
import com.example.demo.ConsultasBD.CategoriaRepository;
import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Categoria;
import com.example.demo.Entidades.Tarefa;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private AreaTrabalhoRepository areaTrabalhoRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    public Categoria criarCategoria(Long areaId, String nome, String cor) {
        AreaTrabalho area = areaTrabalhoRepository.findById(areaId)
                .orElseThrow(() -> new RuntimeException(Mensagens.get("msg.area.naoEncontrada")));

        Categoria categoria = new Categoria();
        categoria.setArea(area);
        categoria.setNome(nome);
        categoria.setCor(cor);

        return categoriaRepository.save(categoria);
    }

    public Categoria editarCategoria(Long id, String nome, String cor) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(Mensagens.get("msg.categoria.naoEncontrada")));

        categoria.setNome(nome);
        categoria.setCor(cor);

        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void remover(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(Mensagens.get("msg.categoria.naoEncontrada")));

        List<Tarefa> tarefasVinculadas = tarefaRepository.findByCategoriasId(id);
        for (Tarefa tarefa : tarefasVinculadas) {
            tarefa.getCategorias().remove(categoria);
        }
        tarefaRepository.saveAll(tarefasVinculadas);

        categoriaRepository.delete(categoria);
    }

    public List<Categoria> listarPorArea(Long areaId) {
        return categoriaRepository.findByAreaId(areaId);
    }

    public Categoria getCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(Mensagens.get("msg.categoria.naoEncontrada")));
    }
}
