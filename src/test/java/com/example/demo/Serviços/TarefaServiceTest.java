package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.AreaTrabalhoRepository;
import com.example.demo.ConsultasBD.CategoriaRepository;
import com.example.demo.ConsultasBD.ListaRepository;
import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Categoria;
import com.example.demo.Entidades.Checklist;
import com.example.demo.Entidades.ItemChecklist;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.Repeticao;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@Import({TarefaService.class, ChecklistService.class, AnexoService.class})
class TarefaServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManager em;

    @Autowired
    private TarefaService tarefaService;

    @Autowired
    private ChecklistService checklistService;

    @Autowired
    private AnexoService anexoService;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private ListaRepository listaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private Usuario usuario;
    private Lista lista;

    private void montarCenarioBasico() {
        usuario = new Usuario();
        usuario.setNome("Responsável");
        usuario.setEmail("responsavel@teste.com");
        usuario.setSenha("hash");
        usuario.setDataNascimento("2000-01-01");
        entityManager.persistAndFlush(usuario);

        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(usuario);
        entityManager.persistAndFlush(area);

        lista = new Lista();
        lista.setNome("Lista");
        lista.setArea(area);
        entityManager.persistAndFlush(lista);
    }

    @Test
    void criarTarefa_comResponsavelECategoria_persisteTudo() {
        montarCenarioBasico();

        Categoria categoria = new Categoria();
        categoria.setNome("Urgente");
        categoria.setCor("#f00");
        categoria.setArea(lista.getArea());
        entityManager.persistAndFlush(categoria);

        Tarefa tarefa = tarefaService.criarTarefa(
                lista.getId(), "Título", "Descrição", 1, "2026-12-31",
                usuario.getId(), true, List.of(categoria.getId()), "diaria");

        assertThat(tarefa.getId()).isNotNull();
        assertThat(tarefa.getResponsaveis()).extracting(Usuario::getId).containsExactly(usuario.getId());
        assertThat(tarefa.getCategorias()).extracting(Categoria::getId).containsExactly(categoria.getId());
        assertThat(tarefa.getRepeticao()).isEqualTo(Repeticao.DIARIA);
        assertThat(tarefa.getDataFim()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void criarTarefa_repeticaoInvalida_caiParaNenhuma() {
        montarCenarioBasico();

        Tarefa tarefa = tarefaService.criarTarefa(
                lista.getId(), "Título", null, null, null, null, null, null, "chuva-de-flor");

        assertThat(tarefa.getRepeticao()).isEqualTo(Repeticao.NENHUMA);
    }

    @Test
    void editarTarefa_mudandoDataFim_resetaLembreteEnviado() {
        montarCenarioBasico();

        Tarefa tarefa = tarefaService.criarTarefa(
                lista.getId(), "Título", null, null, "2026-01-10", null, null, null, null);
        tarefa.setLembreteEnviado(true);
        entityManager.persistAndFlush(tarefa);

        Tarefa editada = tarefaService.editarTarefa(
                tarefa.getId(), lista.getId(), "Título", null, null, "2026-01-20", null, null, null, null);

        assertThat(editada.getLembreteEnviado()).isFalse();
    }

    @Test
    void editarTarefa_mantendoMesmaDataFim_naoResetaLembreteEnviado() {
        montarCenarioBasico();

        Tarefa tarefa = tarefaService.criarTarefa(
                lista.getId(), "Título", null, null, "2026-01-10", null, null, null, null);
        tarefa.setLembreteEnviado(true);
        entityManager.persistAndFlush(tarefa);

        Tarefa editada = tarefaService.editarTarefa(
                tarefa.getId(), lista.getId(), "Título novo", null, null, "2026-01-10", null, null, null, null);

        assertThat(editada.getLembreteEnviado()).isTrue();
    }

    @Test
    void editarTarefa_trocandoResponsavel_substituiOAnterior() {
        montarCenarioBasico();

        Usuario outroResponsavel = new Usuario();
        outroResponsavel.setNome("Outro");
        outroResponsavel.setEmail("outro@teste.com");
        outroResponsavel.setSenha("hash");
        outroResponsavel.setDataNascimento("2000-01-01");
        entityManager.persistAndFlush(outroResponsavel);

        Tarefa tarefa = tarefaService.criarTarefa(
                lista.getId(), "Título", null, null, null, usuario.getId(), null, null, null);

        Tarefa editada = tarefaService.editarTarefa(
                tarefa.getId(), lista.getId(), "Título", null, null, null,
                outroResponsavel.getId(), null, null, null);

        assertThat(editada.getResponsaveis()).extracting(Usuario::getId).containsExactly(outroResponsavel.getId());
    }

    @Test
    void toggleTarefa_semRepeticao_apenasConclui() {
        montarCenarioBasico();
        Tarefa tarefa = tarefaService.criarTarefa(
                lista.getId(), "Título", null, null, "2026-01-10", null, null, null, null);

        Tarefa concluida = tarefaService.toggleTarefa(tarefa.getId(), true);

        assertThat(concluida.getStatus()).isTrue();
        assertThat(concluida.getDataFim()).isEqualTo(LocalDate.of(2026, 1, 10));
    }

    @Test
    void toggleTarefa_repetitivaSemDataFim_conclineNormalmente() {
        montarCenarioBasico();
        Tarefa tarefa = tarefaService.criarTarefa(
                lista.getId(), "Título", null, null, null, null, null, null, "diaria");

        Tarefa concluida = tarefaService.toggleTarefa(tarefa.getId(), true);

        assertThat(concluida.getStatus()).isTrue();
        assertThat(concluida.getDataFim()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "diaria,   2026-01-31, 2026-02-01",
            "semanal,  2026-01-31, 2026-02-07",
            "mensal,   2026-01-31, 2026-02-28", // normalização de fim de mês
            "anual,    2024-02-29, 2025-02-28"  // ano bissexto -> não bissexto
    })
    void toggleTarefa_repetitiva_avancaDataFimEMantemAberta(String repeticao, String dataInicial, String dataEsperada) {
        montarCenarioBasico();
        Tarefa tarefa = tarefaService.criarTarefa(
                lista.getId(), "Título", null, null, dataInicial, null, null, null, repeticao);
        tarefa.setLembreteEnviado(true);
        entityManager.persistAndFlush(tarefa);

        Tarefa resultado = tarefaService.toggleTarefa(tarefa.getId(), true);

        assertThat(resultado.getStatus()).isFalse(); // não marca concluída, reinicia o ciclo
        assertThat(resultado.getDataFim()).isEqualTo(LocalDate.parse(dataEsperada));
        assertThat(resultado.getLembreteEnviado()).isFalse();
    }

    @Test
    void remover_tarefaComChecklistItensECategoria_removeTudoSemErro() {
        montarCenarioBasico();

        Categoria categoria = new Categoria();
        categoria.setNome("Cat");
        categoria.setCor("#000");
        categoria.setArea(lista.getArea());
        entityManager.persistAndFlush(categoria);

        Tarefa tarefa = tarefaService.criarTarefa(
                lista.getId(), "Título", null, null, null, usuario.getId(), null,
                List.of(categoria.getId()), null);

        checklistService.adicionarItem(tarefa.getId(), "Item 1");
        checklistService.adicionarItem(tarefa.getId(), "Item 2");

        Long tarefaId = tarefa.getId();
        entityManager.flush();
        entityManager.clear();

        assertThatCode(() -> {
            tarefaService.remover(tarefaId);
            em.flush();
        }).doesNotThrowAnyException();

        entityManager.clear();
        assertThat(tarefaRepository.findById(tarefaId)).isEmpty();
        // categoria e usuário (dono/responsável) não pertencem à tarefa: continuam existindo.
        assertThat(categoriaRepository.findById(categoria.getId())).isPresent();
        assertThat(usuarioRepository.findById(usuario.getId())).isPresent();
    }
}
