package com.example.demo.Serviços;

import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Categoria;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(CategoriaService.class)
class CategoriaServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManager em;

    @Autowired
    private CategoriaService categoriaService;

    private AreaTrabalho area;

    private void montarArea() {
        Usuario dono = new Usuario();
        dono.setNome("dono");
        dono.setEmail("dono-categoria@teste.com");
        dono.setSenha("hash");
        dono.setDataNascimento("2000-01-01");
        entityManager.persistAndFlush(dono);

        area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
    }

    @Test
    void criarEditarListarCategoria_fluxoBasico() {
        montarArea();

        Categoria criada = categoriaService.criarCategoria(area.getId(), "Urgente", "#ff0000");
        assertThat(criada.getId()).isNotNull();

        Categoria editada = categoriaService.editarCategoria(criada.getId(), "Muito urgente", "#aa0000");
        assertThat(editada.getNome()).isEqualTo("Muito urgente");
        assertThat(editada.getCor()).isEqualTo("#aa0000");

        assertThat(categoriaService.listarPorArea(area.getId())).extracting(Categoria::getId)
                .containsExactly(criada.getId());
    }

    @Test
    void criarCategoria_areaInexistente_lancaExcecao() {
        assertThatThrownBy(() -> categoriaService.criarCategoria(999L, "x", "#000"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void remover_categoriaSemUso_removeDireto() {
        montarArea();
        Categoria categoria = categoriaService.criarCategoria(area.getId(), "Sem uso", "#000");

        categoriaService.remover(categoria.getId());

        assertThat(categoriaService.listarPorArea(area.getId())).isEmpty();
    }

    @Test
    void remover_categoriaAtribuidaAUmaTarefa_desvinculaAntesDeApagar() {
        montarArea();
        Categoria categoria = categoriaService.criarCategoria(area.getId(), "Em uso", "#000");

        Lista lista = new Lista();
        lista.setNome("Lista");
        lista.setArea(area);
        entityManager.persistAndFlush(lista);

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Tarefa");
        tarefa.setListaOrigem(lista);
        tarefa.getCategorias().add(categoria);
        entityManager.persistAndFlush(tarefa);

        Long tarefaId = tarefa.getId();
        Long categoriaId = categoria.getId();
        entityManager.clear();

        assertThatCode(() -> {
            categoriaService.remover(categoriaId);
            em.flush();
        }).doesNotThrowAnyException();

        entityManager.clear();
        Tarefa tarefaRecarregada = em.find(Tarefa.class, tarefaId);
        assertThat(tarefaRecarregada).isNotNull(); // a tarefa continua existindo
        assertThat(tarefaRecarregada.getCategorias()).isEmpty(); // só perdeu a categoria
    }
}
