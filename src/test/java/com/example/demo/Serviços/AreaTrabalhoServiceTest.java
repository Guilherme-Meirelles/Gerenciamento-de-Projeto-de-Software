package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.AreaTrabalhoRepository;
import com.example.demo.ConsultasBD.CategoriaRepository;
import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Categoria;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
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

// Regressão do mesmo padrão de bug já corrigido em UsuarioService: excluir uma
// AreaTrabalho que tem Categoria(s) cadastradas violava a FK de
// Categoria.area_id (AreaTrabalho não cascateava para Categoria), e se a
// categoria estivesse atribuída a alguma tarefa, a limpeza da tabela de
// junção categorias_tarefa também precisa acontecer antes do delete.
@DataJpaTest
@Import({AreaTrabalhoService.class, CategoriaService.class})
class AreaTrabalhoServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManager em;

    @Autowired
    private AreaTrabalhoService areaTrabalhoService;

    @Autowired
    private AreaTrabalhoRepository areaTrabalhoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    private Usuario criarUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + email);
        usuario.setEmail(email);
        usuario.setSenha("hash-fake");
        usuario.setDataNascimento("2000-01-01");
        return entityManager.persistAndFlush(usuario);
    }

    @Test
    void excluirArea_comCategoriaSemUsoEmTarefa_removeAreaECategoriaSemErro() {
        Usuario dono = criarUsuario("dono-cat@teste.com");

        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área com categoria");
        area.setDono(dono);
        entityManager.persistAndFlush(area);

        Categoria categoria = new Categoria();
        categoria.setNome("Urgente");
        categoria.setCor("#ff0000");
        categoria.setArea(area);
        entityManager.persistAndFlush(categoria);

        Long areaId = area.getId();
        Long categoriaId = categoria.getId();

        entityManager.clear();
        AreaTrabalho areaGerenciada = areaTrabalhoRepository.findById(areaId).orElseThrow();

        // Antes da correção, isso lançava DataIntegrityViolationException
        // (FK de Categoria.area_id) e não excluía nada.
        assertThatCode(() -> {
            areaTrabalhoService.excluirArea(areaGerenciada);
            em.flush();
        }).doesNotThrowAnyException();

        entityManager.clear();
        assertThat(areaTrabalhoRepository.findById(areaId)).isEmpty();
        assertThat(categoriaRepository.findById(categoriaId)).isEmpty();
    }

    @Test
    void excluirArea_comCategoriaAtribuidaAUmaTarefa_removeTudoSemErro() {
        Usuario dono = criarUsuario("dono-cat-tarefa@teste.com");

        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área com categoria em uso");
        area.setDono(dono);
        entityManager.persistAndFlush(area);

        ParticipacaoArea participacao = new ParticipacaoArea(dono, area, PermissaoArea.ADMIN);
        entityManager.persistAndFlush(participacao);

        Categoria categoria = new Categoria();
        categoria.setNome("Bloqueado");
        categoria.setCor("#000000");
        categoria.setArea(area);
        entityManager.persistAndFlush(categoria);

        Lista lista = new Lista();
        lista.setNome("Lista");
        lista.setArea(area);
        entityManager.persistAndFlush(lista);

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Tarefa com categoria");
        tarefa.setListaOrigem(lista);
        tarefa.getCategorias().add(categoria);
        entityManager.persistAndFlush(tarefa);

        Long areaId = area.getId();

        entityManager.clear();
        AreaTrabalho areaGerenciada = areaTrabalhoRepository.findById(areaId).orElseThrow();

        assertThatCode(() -> {
            areaTrabalhoService.excluirArea(areaGerenciada);
            em.flush();
        }).doesNotThrowAnyException();

        entityManager.clear();
        assertThat(areaTrabalhoRepository.findById(areaId)).isEmpty();
        assertThat(categoriaRepository.findAll()).isEmpty();
        assertThat(tarefaRepository.findAll()).isEmpty();
    }
}
