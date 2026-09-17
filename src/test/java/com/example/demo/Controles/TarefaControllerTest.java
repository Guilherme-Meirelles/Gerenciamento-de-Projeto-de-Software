package com.example.demo.Controles;

import com.example.demo.ConsultasBD.ListaRepository;
import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.PermissaoAreaService;
import com.example.demo.Serviços.TarefaService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Testa o guard de IDOR (usuarioComAcessoALista/usuarioComAcessoATarefa) de
// ponta a ponta contra um banco real: participante Observador só lê, Editor
// lê/escreve, quem não participa da área toma 403 em tudo — mesmo padrão que
// já foi bug real (IDOR) em outros pontos do projeto (ver bugs-corrigidos.md).
@DataJpaTest
@Import({TarefaController.class, TarefaService.class, PermissaoAreaService.class})
class TarefaControllerTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TarefaController controller;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private ListaRepository listaRepository;

    @Autowired
    private ParticipacaoAreaRepository participacaoAreaRepository;

    private Usuario criarUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + email);
        usuario.setEmail(email);
        usuario.setSenha("hash");
        usuario.setDataNascimento("2000-01-01");
        return entityManager.persistAndFlush(usuario);
    }

    private MockHttpServletRequest requestAutenticado(Long usuarioId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (usuarioId != null) {
            SessaoUtil.autenticar(request, usuarioId);
        }
        return request;
    }

    private record Cenario(Lista lista, Usuario editor, Usuario observador, Usuario naoParticipante) {}

    private Cenario montarCenario() {
        Usuario dono = criarUsuario("dono@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);

        Usuario editor = criarUsuario("editor@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(editor, area, PermissaoArea.EDITOR));

        Usuario observador = criarUsuario("observador@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(observador, area, PermissaoArea.OBSERVADOR));

        Usuario naoParticipante = criarUsuario("estranho@teste.com");

        Lista lista = new Lista();
        lista.setNome("Lista");
        lista.setArea(area);
        entityManager.persistAndFlush(lista);

        entityManager.clear();
        return new Cenario(lista, editor, observador, naoParticipante);
    }

    private Map<String, Object> corpoTarefa(Long listaId, String titulo) {
        Map<String, Object> body = new HashMap<>();
        body.put("listaId", listaId);
        body.put("titulo", titulo);
        return body;
    }

    @Test
    void criarTarefa_naoAutenticado_retorna403() {
        Cenario cenario = montarCenario();
        ResponseEntity<?> resposta = controller.criarTarefa(
                corpoTarefa(cenario.lista().getId(), "Tarefa"), requestAutenticado(null));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void criarTarefa_naoParticipante_retorna403() {
        Cenario cenario = montarCenario();
        ResponseEntity<?> resposta = controller.criarTarefa(
                corpoTarefa(cenario.lista().getId(), "Tarefa"),
                requestAutenticado(cenario.naoParticipante().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void criarTarefa_observador_retorna403() {
        Cenario cenario = montarCenario();
        ResponseEntity<?> resposta = controller.criarTarefa(
                corpoTarefa(cenario.lista().getId(), "Tarefa"),
                requestAutenticado(cenario.observador().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void criarTarefa_editor_criaComSucesso() {
        Cenario cenario = montarCenario();
        ResponseEntity<?> resposta = controller.criarTarefa(
                corpoTarefa(cenario.lista().getId(), "Tarefa nova"),
                requestAutenticado(cenario.editor().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tarefaRepository.findByListaOrigemId(cenario.lista().getId())).hasSize(1);
    }

    @Test
    void listarPorLista_observador_consegueLer() {
        Cenario cenario = montarCenario();
        ResponseEntity<?> resposta = controller.listarPorLista(cenario.lista().getId(), requestAutenticado(cenario.observador().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listarPorLista_naoParticipante_retorna403() {
        Cenario cenario = montarCenario();
        ResponseEntity<?> resposta = controller.listarPorLista(cenario.lista().getId(), requestAutenticado(cenario.naoParticipante().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void editarTarefa_naoParticipante_retorna403ENaoAltera() {
        Cenario cenario = montarCenario();
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Original");
        tarefa.setListaOrigem(cenario.lista());
        entityManager.persistAndFlush(tarefa);
        entityManager.clear();

        Map<String, Object> corpo = corpoTarefa(cenario.lista().getId(), "Alterado por intruso");
        ResponseEntity<?> resposta = controller.editarTarefa(
                tarefa.getId(), corpo, requestAutenticado(cenario.naoParticipante().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(tarefaRepository.findById(tarefa.getId()).orElseThrow().getTitulo()).isEqualTo("Original");
    }

    @Test
    void editarTarefa_observador_retorna403() {
        Cenario cenario = montarCenario();
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Original");
        tarefa.setListaOrigem(cenario.lista());
        entityManager.persistAndFlush(tarefa);
        entityManager.clear();

        ResponseEntity<?> resposta = controller.editarTarefa(
                tarefa.getId(), corpoTarefa(cenario.lista().getId(), "x"),
                requestAutenticado(cenario.observador().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void editarTarefa_editor_alteraComSucesso() {
        Cenario cenario = montarCenario();
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Original");
        tarefa.setListaOrigem(cenario.lista());
        entityManager.persistAndFlush(tarefa);
        entityManager.clear();

        ResponseEntity<?> resposta = controller.editarTarefa(
                tarefa.getId(), corpoTarefa(cenario.lista().getId(), "Editado pelo editor"),
                requestAutenticado(cenario.editor().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tarefaRepository.findById(tarefa.getId()).orElseThrow().getTitulo()).isEqualTo("Editado pelo editor");
    }

    @Test
    void remover_naoParticipante_retorna403ENaoApaga() {
        Cenario cenario = montarCenario();
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Para não ser apagada");
        tarefa.setListaOrigem(cenario.lista());
        entityManager.persistAndFlush(tarefa);
        Long tarefaId = tarefa.getId();
        entityManager.clear();

        ResponseEntity<Void> resposta = controller.remover(tarefaId, requestAutenticado(cenario.naoParticipante().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(tarefaRepository.findById(tarefaId)).isPresent();
    }

    @Test
    void remover_editor_apagaComSucesso() {
        Cenario cenario = montarCenario();
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Para apagar");
        tarefa.setListaOrigem(cenario.lista());
        entityManager.persistAndFlush(tarefa);
        Long tarefaId = tarefa.getId();
        entityManager.clear();

        ResponseEntity<Void> resposta = controller.remover(tarefaId, requestAutenticado(cenario.editor().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tarefaRepository.findById(tarefaId)).isEmpty();
    }

    @Test
    void toggleTarefa_observador_retorna403() {
        Cenario cenario = montarCenario();
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Para concluir");
        tarefa.setListaOrigem(cenario.lista());
        tarefa.setStatus(false);
        entityManager.persistAndFlush(tarefa);
        entityManager.clear();

        Map<String, Object> body = new HashMap<>();
        body.put("concluida", true);
        ResponseEntity<?> resposta = controller.toggleTarefa(tarefa.getId(), body, requestAutenticado(cenario.observador().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(tarefaRepository.findById(tarefa.getId()).orElseThrow().getStatus()).isFalse();
    }

    @Test
    void toggleTarefa_editor_marcaConcluida() {
        Cenario cenario = montarCenario();
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Para concluir");
        tarefa.setListaOrigem(cenario.lista());
        tarefa.setStatus(false);
        entityManager.persistAndFlush(tarefa);
        entityManager.clear();

        Map<String, Object> body = new HashMap<>();
        body.put("concluida", true);
        ResponseEntity<?> resposta = controller.toggleTarefa(tarefa.getId(), body, requestAutenticado(cenario.editor().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tarefaRepository.findById(tarefa.getId()).orElseThrow().getStatus()).isTrue();
    }
}
