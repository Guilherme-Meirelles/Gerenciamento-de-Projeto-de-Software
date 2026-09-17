package com.example.demo.Serviços;

import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Checklist;
import com.example.demo.Entidades.ItemChecklist;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(ChecklistService.class)
class ChecklistServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChecklistService checklistService;

    private Tarefa tarefa;

    private void montarTarefa() {
        Usuario dono = new Usuario();
        dono.setNome("dono");
        dono.setEmail("dono-checklist@teste.com");
        dono.setSenha("hash");
        dono.setDataNascimento("2000-01-01");
        entityManager.persistAndFlush(dono);

        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);

        Lista lista = new Lista();
        lista.setNome("Lista");
        lista.setArea(area);
        entityManager.persistAndFlush(lista);

        tarefa = new Tarefa();
        tarefa.setTitulo("Tarefa");
        tarefa.setListaOrigem(lista);
        entityManager.persistAndFlush(tarefa);
    }

    @Test
    void getChecklistDaTarefa_semChecklistAinda_retornaNull() {
        montarTarefa();
        assertThat(checklistService.getChecklistDaTarefa(tarefa.getId())).isNull();
    }

    @Test
    void obterOuCriarChecklist_chamadoDuasVezes_naoCriaChecklistDuplicado() {
        montarTarefa();

        Checklist primeira = checklistService.obterOuCriarChecklist(tarefa.getId());
        Checklist segunda = checklistService.obterOuCriarChecklist(tarefa.getId());

        assertThat(primeira.getId()).isEqualTo(segunda.getId());
    }

    @Test
    void adicionarItem_criaChecklistSobDemandaEAcumulaItens() {
        montarTarefa();

        ItemChecklist item1 = checklistService.adicionarItem(tarefa.getId(), "Ler slides");
        entityManager.flush();
        entityManager.clear(); // simula outra requisição HTTP lendo o estado já salvo
        ItemChecklist item2 = checklistService.adicionarItem(tarefa.getId(), "Fazer resumo");

        assertThat(item1.getConcluido()).isFalse();
        entityManager.flush();
        entityManager.clear();
        Checklist checklist = checklistService.getChecklistDaTarefa(tarefa.getId());
        assertThat(checklist.getItens()).extracting(ItemChecklist::getId)
                .containsExactlyInAnyOrder(item1.getId(), item2.getId());
    }

    @Test
    void editarItem_atualizaSoOsCamposInformados() {
        montarTarefa();
        ItemChecklist item = checklistService.adicionarItem(tarefa.getId(), "Original");

        ItemChecklist apenasConcluido = checklistService.editarItem(item.getId(), null, true);
        assertThat(apenasConcluido.getDescricao()).isEqualTo("Original");
        assertThat(apenasConcluido.getConcluido()).isTrue();

        ItemChecklist apenasDescricao = checklistService.editarItem(item.getId(), "Novo texto", null);
        assertThat(apenasDescricao.getDescricao()).isEqualTo("Novo texto");
        assertThat(apenasDescricao.getConcluido()).isTrue(); // não foi mexido, continua true
    }

    @Test
    void removerItem_remove() {
        montarTarefa();
        ItemChecklist item = checklistService.adicionarItem(tarefa.getId(), "Item");

        checklistService.removerItem(item.getId());

        Checklist checklist = checklistService.getChecklistDaTarefa(tarefa.getId());
        assertThat(checklist.getItens()).isEmpty();
    }

    @Test
    void adicionarItem_tarefaInexistente_lancaExcecao() {
        assertThatThrownBy(() -> checklistService.adicionarItem(999L, "x"))
                .isInstanceOf(RuntimeException.class);
    }
}
