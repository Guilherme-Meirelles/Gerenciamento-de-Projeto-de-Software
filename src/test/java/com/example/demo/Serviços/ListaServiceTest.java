package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.Entidades.AreaTrabalho;
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

// Regressão do mesmo padrão de bug já corrigido em Usuario/AreaTrabalho: excluir
// uma Lista cujas tarefas têm responsável atribuído (tabela de junção
// responsaveis_tarefa) precisa funcionar, já que Tarefa é o lado "dono" dessas
// coleções (ao contrário de Usuario, que é o lado inverso) e por isso já
// deveria cascatear certo — este teste prova isso.
@DataJpaTest
@Import(ListaService.class)
class ListaServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManager em;

    @Autowired
    private ListaService listaService;

    @Autowired
    private TarefaRepository tarefaRepository;

    private AreaTrabalho area;

    private Usuario criarUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + email);
        usuario.setEmail(email);
        usuario.setSenha("hash");
        usuario.setDataNascimento("2000-01-01");
        return entityManager.persistAndFlush(usuario);
    }

    private void montarArea() {
        Usuario dono = criarUsuario("dono-lista@teste.com");
        area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
    }

    @Test
    void criarEditarListarLista_fluxoBasico() {
        montarArea();

        Lista criada = listaService.criarLista(area.getId(), "Lista 1", "desc");
        assertThat(criada.getId()).isNotNull();

        Lista editada = listaService.editarLista(criada.getId(), "Lista 1 renomeada", "desc nova");
        assertThat(editada.getNome()).isEqualTo("Lista 1 renomeada");

        assertThat(listaService.listarPorArea(area.getId())).extracting(Lista::getId).containsExactly(criada.getId());
    }

    @Test
    void getLista_inexistente_lancaExcecao() {
        assertThatThrownBy(() -> listaService.getLista(999L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void deletarLista_comTarefaAtribuidaAUmResponsavel_removeSemErro() {
        montarArea();
        Usuario responsavel = criarUsuario("resp-lista@teste.com");

        Lista lista = listaService.criarLista(area.getId(), "Lista com tarefas", null);

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Tarefa com responsável");
        tarefa.setListaOrigem(lista);
        tarefa.getResponsaveis().add(responsavel);
        entityManager.persistAndFlush(tarefa);

        Long listaId = lista.getId();
        Long tarefaId = tarefa.getId();
        entityManager.clear();

        assertThatCode(() -> {
            listaService.deletarLista(listaId);
            em.flush();
        }).doesNotThrowAnyException();

        entityManager.clear();
        assertThat(tarefaRepository.findById(tarefaId)).isEmpty();
    }
}
