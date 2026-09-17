package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.AreaTrabalhoRepository;
import com.example.demo.ConsultasBD.CategoriaRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Categoria;
import com.example.demo.Entidades.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AreaTrabalhoService {

    private final AreaTrabalhoRepository repo;
    private final CategoriaRepository categoriaRepository;
    private final CategoriaService categoriaService;

    public AreaTrabalhoService(AreaTrabalhoRepository repo, CategoriaRepository categoriaRepository, CategoriaService categoriaService) {
        this.repo = repo;
        this.categoriaRepository = categoriaRepository;
        this.categoriaService = categoriaService;
    }

    public AreaTrabalho criarArea(String nome, Usuario dono) {
        AreaTrabalho area = new AreaTrabalho();
        area.setNome(nome);
        area.setDono(dono);
        return repo.save(area);
    }

    // AreaTrabalho não tem cascade declarado para Categoria (Categoria.area_id
    // é FK obrigatória), então as categorias da área precisam ser removidas
    // antes do delete. CategoriaService.remover já cuida de tirar a categoria
    // de qualquer tarefa (tabela de junção categorias_tarefa) antes de
    // excluí-la. Listas/tarefas/participações já cascateiam via anotações em
    // AreaTrabalho.
    @Transactional
    public void excluirArea(AreaTrabalho area) {
        List<Categoria> categorias = categoriaRepository.findByAreaId(area.getId());
        for (Categoria categoria : categorias) {
            categoriaService.remover(categoria.getId());
        }
        repo.delete(area);
    }
}