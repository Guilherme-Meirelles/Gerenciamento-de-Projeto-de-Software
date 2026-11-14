package com.example.demo.Controles;

import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.PermissaoAreaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Notificações do sino: só tarefas do próprio usuário, com notificação ligada, não concluídas e
// vencendo até amanhã (atrasadas incluídas), de áreas em que ele ainda participa.
@DataJpaTest
@Import({NotificacaoController.class, PermissaoAreaService.class})
class NotificacaoControllerTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificacaoController controller;

    @Autowired
    private TarefaRepository tarefaRepository;

    private Usuario criarUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + email);
        usuario.setEmail(email);
        usuario.setSenha("hash");
        usuario.setDataNascimento("2000-01-01");
        return entityManager.persistAndFlush(usuario);
    }

    private MockHttpServletRequest requestDe(Usuario usuario) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (usuario != null) {
            SessaoUtil.autenticar(request, usuario.getId());
        }
        return request;
    }

    private Lista criarLista(Usuario dono, String nomeArea, Usuario... participantes) {
        AreaTrabalho area = new AreaTrabalho();
        area.setNome(nomeArea);
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        for (Usuario p : participantes) {
            entityManager.persistAndFlush(new ParticipacaoArea(p, area, PermissaoArea.EDITOR));
        }
        Lista lista = new Lista();
        lista.setNome("Lista de " + nomeArea);
        lista.setArea(area);
        return entityManager.persistAndFlush(lista);
    }

    private Tarefa criarTarefa(Lista lista, Usuario responsavel, String titulo, LocalDate dataFim,
                               boolean notificacoes, boolean concluida) {
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(titulo);
        tarefa.setDataFim(dataFim);
        tarefa.setNotificacoes(notificacoes);
        tarefa.setStatus(concluida);
        tarefa.setLembreteEnviado(false);
        tarefa.setListaOrigem(lista);
        if (responsavel != null) {
            tarefa.getResponsaveis().add(responsavel);
        }
        return entityManager.persistAndFlush(tarefa);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> corpo(ResponseEntity<?> resposta) {
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (Map<String, Object>) resposta.getBody();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> itens(ResponseEntity<?> resposta) {
        return (List<Map<String, Object>>) corpo(resposta).get("itens");
    }

    @Test
    void semSessao_retorna401() {
        ResponseEntity<?> resposta = controller.listar(requestDe(null));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listaAtrasadaHojeEAmanha_naOrdemDaData_comASituacaoCerta() {
        Usuario ana = criarUsuario("ana@teste.com");
        Lista lista = criarLista(ana, "Casa", ana);
        LocalDate hoje = LocalDate.now();
        criarTarefa(lista, ana, "Amanhã", hoje.plusDays(1), true, false);
        criarTarefa(lista, ana, "Hoje", hoje, true, false);
        criarTarefa(lista, ana, "Atrasada", hoje.minusDays(3), true, false);

        ResponseEntity<?> resposta = controller.listar(requestDe(ana));

        assertThat(corpo(resposta).get("total")).isEqualTo(3);
        List<Map<String, Object>> itens = itens(resposta);
        assertThat(itens).extracting(i -> i.get("titulo")).containsExactly("Atrasada", "Hoje", "Amanhã");
        assertThat(itens).extracting(i -> i.get("situacao")).containsExactly("ATRASADA", "HOJE", "AMANHA");
        assertThat(itens.get(0).get("diasAtraso")).isEqualTo(3L);
        assertThat(itens.get(0).get("areaNome")).isEqualTo("Casa");
    }

    @Test
    void ignoraNotificacaoDesligadaConcluidaDistanteSemDataEDeOutroResponsavel() {
        Usuario ana = criarUsuario("ana@teste.com");
        Usuario beto = criarUsuario("beto@teste.com");
        Lista lista = criarLista(ana, "Casa", ana, beto);
        LocalDate hoje = LocalDate.now();
        criarTarefa(lista, ana, "Valida", hoje, true, false);
        criarTarefa(lista, ana, "Sem aviso", hoje, false, false);
        criarTarefa(lista, ana, "Concluída", hoje, true, true);
        criarTarefa(lista, ana, "Daqui a dois dias", hoje.plusDays(2), true, false);
        criarTarefa(lista, ana, "Sem data", null, true, false);
        criarTarefa(lista, beto, "Do Beto", hoje, true, false);
        criarTarefa(lista, null, "Sem responsável", hoje, true, false);

        List<Map<String, Object>> itens = itens(controller.listar(requestDe(ana)));

        assertThat(itens).extracting(i -> i.get("titulo")).containsExactly("Valida");
    }

    @Test
    void quemSaiuDaArea_naoRecebeMaisAvisosDeLa() {
        Usuario ana = criarUsuario("ana@teste.com");
        Usuario beto = criarUsuario("beto@teste.com");
        // Beto é responsável da tarefa, mas já não participa da área
        Lista lista = criarLista(ana, "Casa", ana);
        criarTarefa(lista, beto, "Tarefa do Beto", LocalDate.now(), true, false);

        ResponseEntity<?> resposta = controller.listar(requestDe(beto));

        assertThat(corpo(resposta).get("total")).isEqualTo(0);
        assertThat(itens(resposta)).isEmpty();
    }

    @Test
    void preferenciasPadrao_estaoLigadas() {
        Usuario ana = criarUsuario("ana@teste.com");

        Map<String, Object> corpo = corpo(controller.listar(requestDe(ana)));

        assertThat(corpo.get("preferencias")).isEqualTo(Map.of("email", true, "sistema", true));
    }

    @Test
    void sistemaDesligadoNasPreferencias_sinoVem_vazio_masAsPreferenciasVoltam() {
        Usuario ana = criarUsuario("ana@teste.com");
        Lista lista = criarLista(ana, "Casa", ana);
        criarTarefa(lista, ana, "Hoje", LocalDate.now(), true, false);

        ResponseEntity<?> salvou = controller.salvarPreferencias(Map.of("sistema", false), requestDe(ana));
        assertThat(salvou.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> corpo = corpo(controller.listar(requestDe(ana)));
        assertThat(corpo.get("total")).isEqualTo(0);
        assertThat(itens(controller.listar(requestDe(ana)))).isEmpty();
        assertThat(corpo.get("preferencias")).isEqualTo(Map.of("email", true, "sistema", false));
    }

    @Test
    void salvarPreferencias_soMudaOQueFoiEnviado_eGravaNoUsuario() {
        Usuario ana = criarUsuario("ana@teste.com");

        controller.salvarPreferencias(Map.of("email", false), requestDe(ana));
        entityManager.flush(); // grava de verdade antes de esvaziar o contexto e reler do banco
        entityManager.clear();

        Usuario recarregada = entityManager.find(Usuario.class, ana.getId());
        assertThat(recarregada.isNotificarPorEmail()).isFalse();
        assertThat(recarregada.isNotificarNoSistema()).isTrue();
    }

    @Test
    void salvarPreferencias_semSessao_retorna401() {
        ResponseEntity<?> resposta = controller.salvarPreferencias(Map.of("email", false), requestDe(null));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
