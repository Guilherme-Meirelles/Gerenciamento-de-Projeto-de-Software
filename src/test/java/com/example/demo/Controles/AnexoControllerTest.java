package com.example.demo.Controles;

import com.example.demo.ConsultasBD.AnexoRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.AnexoService;
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
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({AnexoController.class, AnexoService.class, PermissaoAreaService.class})
class AnexoControllerTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AnexoController controller;

    @Autowired
    private AnexoRepository anexoRepository;

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
        Usuario dono = criarUsuario("dono-anexo-ctrl@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);

        Usuario editor = criarUsuario("editor-anexo-ctrl@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(editor, area, PermissaoArea.EDITOR));

        Usuario naoParticipante = criarUsuario("estranho-anexo-ctrl@teste.com");

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
    void adicionar_naoParticipante_retorna403() {
        Cenario cenario = montarCenario();
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "a.txt", "text/plain", "conteudo".getBytes());

        ResponseEntity<?> resposta = controller.adicionar(
                cenario.tarefa().getId(), arquivo, requestAutenticado(cenario.naoParticipante().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(anexoRepository.findAll()).isEmpty();
    }

    @Test
    void adicionar_editor_salvaComSucesso() {
        Cenario cenario = montarCenario();
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "a.txt", "text/plain", "conteudo".getBytes());

        ResponseEntity<?> resposta = controller.adicionar(
                cenario.tarefa().getId(), arquivo, requestAutenticado(cenario.editor().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(anexoRepository.findAll()).hasSize(1);
    }

    @Test
    void download_naoParticipante_retorna403() {
        Cenario cenario = montarCenario();
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "a.txt", "text/plain", "conteudo".getBytes());
        controller.adicionar(cenario.tarefa().getId(), arquivo, requestAutenticado(cenario.editor().getId()));
        Long anexoId = anexoRepository.findAll().get(0).getId();

        ResponseEntity<byte[]> resposta = controller.download(anexoId, requestAutenticado(cenario.naoParticipante().getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
