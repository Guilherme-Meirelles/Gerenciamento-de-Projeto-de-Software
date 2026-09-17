package com.example.demo.Controles;

import com.example.demo.ConsultasBD.ItemChecklistRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.ChecklistService;
import com.example.demo.Serviços.PermissaoAreaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ChecklistController.class, ChecklistService.class, PermissaoAreaService.class})
class ChecklistControllerTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChecklistController controller;

    @Autowired
    private ItemChecklistRepository itemChecklistRepository;

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
        SessaoUtil.autenticar(request, usuarioId);
        return request;
    }

    private record Cenario(Tarefa tarefa, Usuario editor, Usuario naoParticipante) {}

    private Cenario montarCenario() {
        Usuario dono = criarUsuario("dono-checklist@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);

        Usuario editor = criarUsuario("editor-checklist@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(editor, area, PermissaoArea.EDITOR));

        Usuario naoParticipante = criarUsuario("estranho-checklist@teste.com");

        Lista lista = new Lista();
        lista.setNome("Lista");
        lista.setArea(area);
        entityManager.persistAndFlush(lista);

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Tarefa");
        tarefa.setListaOrigem(lista);
        entityManager.persistAndFlush(tarefa);

        entityManager.clear();
        return new Cenario(tarefa, editor, naoParticipante);
    }

    @Test
    void adicionarItem_naoParticipante_retorna403() {
        Cenario cenario = montarCenario();

        ResponseEntity<?> resposta = controller.adicionarItem(
                cenario.tarefa().getId(), Map.of("descricao", "Item"), requestAutenticado(cenario.naoParticipante().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(itemChecklistRepository.findAll()).isEmpty();
    }

    @Test
    void adicionarItem_editor_criaItemComSucesso() {
        Cenario cenario = montarCenario();

        ResponseEntity<?> resposta = controller.adicionarItem(
                cenario.tarefa().getId(), Map.of("descricao", "Item"), requestAutenticado(cenario.editor().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(itemChecklistRepository.findAll()).hasSize(1);
    }

    @Test
    void getChecklist_naoParticipante_retorna403() {
        Cenario cenario = montarCenario();

        ResponseEntity<?> resposta = controller.getChecklist(cenario.tarefa().getId(), requestAutenticado(cenario.naoParticipante().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
