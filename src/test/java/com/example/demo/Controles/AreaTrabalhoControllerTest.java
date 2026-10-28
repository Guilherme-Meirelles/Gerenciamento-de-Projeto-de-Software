package com.example.demo.Controles;

import com.example.demo.ConsultasBD.AreaTrabalhoRepository;
import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.ConsultasBD.TokenRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
import com.example.demo.Entidades.Token;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.AreaTrabalhoService;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.example.demo.Serviços.CategoriaService;
import com.example.demo.Serviços.ConviteAreaService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "app.base-url=http://localhost:8080")
@Import({AreaTrabalhoController.class, AreaTrabalhoService.class, ListaService.class, TarefaService.class,
        CategoriaService.class, ConviteAreaService.class, PermissaoAreaService.class})
class AreaTrabalhoControllerTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AreaTrabalhoController controller;

    @Autowired
    private AreaTrabalhoRepository areaTrabalhoRepository;

    @Autowired
    private ParticipacaoAreaRepository participacaoAreaRepository;

    @Autowired
    private TokenRepository tokenRepository;

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

    // ---------- criarArea ----------

    @Test
    void criarArea_naoLogado_retornaNotLogged() {
        String resultado = controller.criarArea("Nova área", requestAutenticado(null));
        assertThat(resultado).isEqualTo("NOT_LOGGED");
    }

    @Test
    void criarArea_valido_criaAreaEParticipacaoAdminParaOCriador() {
        Usuario usuario = criarUsuario("criador@teste.com");
        entityManager.clear();

        String idCriado = controller.criarArea("Minha área", requestAutenticado(usuario.getId()));

        Long areaId = Long.valueOf(idCriado);
        AreaTrabalho area = areaTrabalhoRepository.findById(areaId).orElseThrow();
        assertThat(area.getDono().getId()).isEqualTo(usuario.getId());

        ParticipacaoArea participacao = participacaoAreaRepository.findByUsuarioIdAndAreaId(usuario.getId(), areaId);
        assertThat(participacao).isNotNull();
        assertThat(participacao.getPermissao()).isEqualTo(PermissaoArea.ADMIN);
    }

    // ---------- renomearArea (só o dono) ----------

    @Test
    void renomearArea_naoEhDono_retorna403() {
        Usuario dono = criarUsuario("dono-renomear@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Original");
        area.setDono(dono);
        entityManager.persistAndFlush(area);

        // Mesmo sendo ADMIN participante, só quem é o "dono" (criador) pode renomear.
        Usuario adminNaoDono = criarUsuario("admin-nao-dono@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(adminNaoDono, area, PermissaoArea.ADMIN));
        Long areaId = area.getId();
        entityManager.clear();

        MockHttpServletRequest request = requestAutenticado(adminNaoDono.getId());
        request.setParameter("nome", "Nome hostil");
        ResponseEntity<String> resposta = controller.renomearArea(areaId, null, request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(403);
        assertThat(areaTrabalhoRepository.findById(areaId).orElseThrow().getNome()).isEqualTo("Original");
    }

    @Test
    void renomearArea_dono_renomeiaComSucesso() {
        Usuario dono = criarUsuario("dono-renomear2@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Original");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        Long areaId = area.getId();
        entityManager.clear();

        MockHttpServletRequest request = requestAutenticado(dono.getId());
        request.setParameter("nome", "Nome novo");
        ResponseEntity<String> resposta = controller.renomearArea(areaId, null, request);

        assertThat(resposta.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(areaTrabalhoRepository.findById(areaId).orElseThrow().getNome()).isEqualTo("Nome novo");
    }

    // ---------- gerarLink ----------

    @Test
    void gerarLink_observador_retorna403() {
        Usuario dono = criarUsuario("dono-link@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        Usuario observador = criarUsuario("observador-link@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(observador, area, PermissaoArea.OBSERVADOR));
        Long areaId = area.getId();
        entityManager.clear();

        ResponseEntity<?> resposta = controller.gerarLink(areaId, "EDITOR", requestAutenticado(observador.getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void gerarLink_editorPedindoNivelAdmin_ehRebaixadoParaEditor() {
        Usuario dono = criarUsuario("dono-link2@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        Usuario editor = criarUsuario("editor-link2@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(editor, area, PermissaoArea.EDITOR));
        Long areaId = area.getId();
        entityManager.clear();

        controller.gerarLink(areaId, "ADMIN", requestAutenticado(editor.getId()));

        List<Token> tokens = tokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getPermissao()).isEqualTo(PermissaoArea.EDITOR); // rebaixado
    }

    @Test
    void gerarLink_adminPedindoNivelAdmin_mantemAdmin() {
        Usuario dono = criarUsuario("dono-link3@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        Usuario admin = criarUsuario("admin-link3@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(admin, area, PermissaoArea.ADMIN));
        Long areaId = area.getId();
        entityManager.clear();

        controller.gerarLink(areaId, "ADMIN", requestAutenticado(admin.getId()));

        List<Token> tokens = tokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getPermissao()).isEqualTo(PermissaoArea.ADMIN);
    }

    // ---------- removerMembro (só admin) ----------

    @Test
    void removerMembro_solicitanteNaoAdmin_retorna403ENaoRemove() {
        Usuario dono = criarUsuario("dono-remover@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        Usuario editor = criarUsuario("editor-remover@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(editor, area, PermissaoArea.EDITOR));
        Usuario alvo = criarUsuario("alvo-remover@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(alvo, area, PermissaoArea.OBSERVADOR));
        Long areaId = area.getId();
        Long alvoId = alvo.getId();
        entityManager.clear();

        Map<String, Object> body = Map.of("usuarioId", alvoId, "areaId", areaId);
        ResponseEntity<?> resposta = controller.removerMembro(body, requestAutenticado(editor.getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(participacaoAreaRepository.findByUsuarioIdAndAreaId(alvoId, areaId)).isNotNull();
    }

    @Test
    void removerMembro_solicitanteAdmin_removeComSucesso() {
        Usuario dono = criarUsuario("dono-remover2@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        Usuario admin = criarUsuario("admin-remover2@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(admin, area, PermissaoArea.ADMIN));
        Usuario alvo = criarUsuario("alvo-remover2@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(alvo, area, PermissaoArea.OBSERVADOR));
        Long areaId = area.getId();
        Long alvoId = alvo.getId();
        entityManager.clear();

        Map<String, Object> body = Map.of("usuarioId", alvoId, "areaId", areaId);
        ResponseEntity<?> resposta = controller.removerMembro(body, requestAutenticado(admin.getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(participacaoAreaRepository.findByUsuarioIdAndAreaId(alvoId, areaId)).isNull();
    }

    // ---------- areasTrabalhoId (acesso à página da área) ----------

    @Test
    void areasTrabalhoId_naoParticipante_redirecionaParaListaDeAreas() {
        Usuario dono = criarUsuario("dono-pagina@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        Usuario estranho = criarUsuario("estranho-pagina@teste.com");
        Long areaId = area.getId();
        entityManager.clear();

        Model model = new ExtendedModelMap();
        String view = controller.areasTrabalhoId(areaId, "Área", model, requestAutenticado(estranho.getId()));

        assertThat(view).isEqualTo("redirect:/areasTrabalho");
    }

    @Test
    void areasTrabalhoId_participante_mostraAPagina() {
        Usuario dono = criarUsuario("dono-pagina2@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        entityManager.persistAndFlush(new ParticipacaoArea(dono, area, PermissaoArea.ADMIN));
        Long areaId = area.getId();
        entityManager.clear();

        Model model = new ExtendedModelMap();
        String view = controller.areasTrabalhoId(areaId, "Área", model, requestAutenticado(dono.getId()));

        assertThat(view).isEqualTo("menu");
    }

    // ---------- alterarPermissaoMembro (só admin) ----------

    private Long[] prepararAreaComAdminEMembro(String sufixo, PermissaoArea permissaoDoMembro, Usuario[] saida) {
        Usuario dono = criarUsuario("dono-perm-" + sufixo + "@teste.com");
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        entityManager.persistAndFlush(area);
        Usuario admin = criarUsuario("admin-perm-" + sufixo + "@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(admin, area, PermissaoArea.ADMIN));
        Usuario membro = criarUsuario("membro-perm-" + sufixo + "@teste.com");
        entityManager.persistAndFlush(new ParticipacaoArea(membro, area, permissaoDoMembro));
        saida[0] = admin;
        saida[1] = membro;
        Long[] ids = {area.getId(), membro.getId()};
        entityManager.clear();
        return ids;
    }

    @Test
    void alterarPermissaoMembro_admin_gravaNovaPermissao() {
        Usuario[] u = new Usuario[2];
        Long[] ids = prepararAreaComAdminEMembro("ok", PermissaoArea.OBSERVADOR, u);

        ResponseEntity<?> resposta = controller.alterarPermissaoMembro(ids[0], ids[1],
                Map.of("permissao", "editor"), requestAutenticado(u[0].getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        entityManager.flush(); // grava a alteração antes de limpar o cache, para reler do banco
        entityManager.clear();
        assertThat(participacaoAreaRepository.findByUsuarioIdAndAreaId(ids[1], ids[0]).getPermissao())
                .isEqualTo(PermissaoArea.EDITOR);
    }

    @Test
    void alterarPermissaoMembro_naoLogado_retorna401() {
        Usuario[] u = new Usuario[2];
        Long[] ids = prepararAreaComAdminEMembro("401", PermissaoArea.OBSERVADOR, u);

        ResponseEntity<?> resposta = controller.alterarPermissaoMembro(ids[0], ids[1],
                Map.of("permissao", "editor"), requestAutenticado(null));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void alterarPermissaoMembro_naoAdmin_retorna403ENaoAltera() {
        Usuario[] u = new Usuario[2];
        Long[] ids = prepararAreaComAdminEMembro("403", PermissaoArea.OBSERVADOR, u);

        // o próprio membro (observador) tenta se promover
        ResponseEntity<?> resposta = controller.alterarPermissaoMembro(ids[0], ids[1],
                Map.of("permissao", "editor"), requestAutenticado(u[1].getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        entityManager.clear();
        assertThat(participacaoAreaRepository.findByUsuarioIdAndAreaId(ids[1], ids[0]).getPermissao())
                .isEqualTo(PermissaoArea.OBSERVADOR);
    }

    @Test
    void alterarPermissaoMembro_permissaoInvalidaOuAdmin_retorna400() {
        Usuario[] u = new Usuario[2];
        Long[] ids = prepararAreaComAdminEMembro("400", PermissaoArea.OBSERVADOR, u);

        ResponseEntity<?> invalida = controller.alterarPermissaoMembro(ids[0], ids[1],
                Map.of("permissao", "qualquer"), requestAutenticado(u[0].getId()));
        ResponseEntity<?> promoverAdmin = controller.alterarPermissaoMembro(ids[0], ids[1],
                Map.of("permissao", "admin"), requestAutenticado(u[0].getId()));

        assertThat(invalida.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(promoverAdmin.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void alterarPermissaoMembro_alvoEhAdmin_retorna400ENaoRebaixa() {
        Usuario[] u = new Usuario[2];
        Long[] ids = prepararAreaComAdminEMembro("adm", PermissaoArea.ADMIN, u);

        ResponseEntity<?> resposta = controller.alterarPermissaoMembro(ids[0], ids[1],
                Map.of("permissao", "observador"), requestAutenticado(u[0].getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        entityManager.clear();
        assertThat(participacaoAreaRepository.findByUsuarioIdAndAreaId(ids[1], ids[0]).getPermissao())
                .isEqualTo(PermissaoArea.ADMIN);
    }

    @Test
    void alterarPermissaoMembro_usuarioForaDaArea_retorna404() {
        Usuario[] u = new Usuario[2];
        Long[] ids = prepararAreaComAdminEMembro("404", PermissaoArea.OBSERVADOR, u);

        ResponseEntity<?> resposta = controller.alterarPermissaoMembro(ids[0], 999999L,
                Map.of("permissao", "editor"), requestAutenticado(u[0].getId()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}