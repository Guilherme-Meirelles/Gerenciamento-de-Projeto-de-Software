package com.example.demo.Controles;

import com.example.demo.ConsultasBD.ListaRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.ListaService;
import com.example.demo.Serviços.PermissaoAreaService;
import com.example.demo.Serviços.TarefaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

// Mesmo guard de IDOR do TarefaControllerTest, verificado aqui num controller
// irmão (ListaController usa exatamente o mesmo padrão de checagem).
@DataJpaTest
@Import({ListaController.class, ListaService.class, TarefaService.class, PermissaoAreaService.class})
class ListaControllerTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ListaController controller;

    @Autowired
    private ListaRepository listaRepository;

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

    @Test
    void criarLista_naoParticipante_retorna403() {
        Usuario dono = criarUsuario("dono@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        Usuario estranho = criarUsuario("estranho@teste.com");
        entityManager.clear();

        ResponseEntity<?> resposta = controller.criarLista(area.getId(), "Nova lista", null, requestAutenticado(estranho.getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(listaRepository.findByAreaId(area.getId())).isEmpty();
    }

    @Test
    void criarLista_observador_retorna403() {
        Usuario dono = criarUsuario("dono2@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        Usuario observador = criarUsuario("observador2@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(observador, area, PermissaoArea.OBSERVADOR));
        entityManager.clear();

        ResponseEntity<?> resposta = controller.criarLista(area.getId(), "Nova lista", null, requestAutenticado(observador.getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void criarLista_editor_criaComSucesso() {
        Usuario dono = criarUsuario("dono3@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        Usuario editor = criarUsuario("editor3@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(editor, area, PermissaoArea.EDITOR));
        entityManager.clear();

        ResponseEntity<?> resposta = controller.criarLista(area.getId(), "Nova lista", null, requestAutenticado(editor.getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listaRepository.findByAreaId(area.getId())).hasSize(1);
    }

    @Test
    void deletarLista_naoParticipante_retorna403ENaoApaga() {
        Usuario dono = criarUsuario("dono4@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        Lista lista = new Lista();
        lista.setNome("Lista");
        lista.setArea(area);
        entityManager.persistAndFlush(lista);
        Usuario estranho = criarUsuario("estranho4@teste.com");
        Long listaId = lista.getId();
        entityManager.clear();

        ResponseEntity<Void> resposta = controller.deletarLista(listaId, requestAutenticado(estranho.getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(listaRepository.findById(listaId)).isPresent();
    }
}
