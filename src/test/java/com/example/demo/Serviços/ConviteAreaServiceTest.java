package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
import com.example.demo.Entidades.Token;
import com.example.demo.Entidades.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Fluxo de "Copiar Link" de convite de área de trabalho. Regressão relevante:
// o bug de IDOR já corrigido no projeto era um convite (por e-mail) não amarrado
// a destinatário/área; aqui a proteção equivalente é aceitar só token que tenha
// areaId e NÃO tenha email (o que marca um token como "link genérico", diferente
// de um convite por e-mail).
@DataJpaTest
@Import(ConviteAreaService.class)
class ConviteAreaServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ConviteAreaService conviteAreaService;

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

    private AreaTrabalho criarArea(Usuario dono) {
        AreaTrabalho area = new AreaTrabalho();
        area.setNome("Área");
        area.setDono(dono);
        return entityManager.persistAndFlush(area);
    }

    private Token criarTokenLink(Long areaId, PermissaoArea permissao, LocalDateTime expiraEm) {
        Token token = new Token();
        token.setToken(UUID.randomUUID().toString());
        token.setEmail(null);
        token.setAreaId(areaId);
        token.setPermissao(permissao);
        token.setExpiraEm(expiraEm);
        token.setUsado(false);
        return entityManager.persistAndFlush(token);
    }

    @Test
    void tokenInexistente_falha() {
        Usuario usuario = criarUsuario("a@teste.com");

        var resultado = conviteAreaService.aceitarLinkGenerico("token-que-nao-existe", usuario);

        assertThat(resultado.sucesso()).isFalse();
    }

    @Test
    void tokenDeConviteAmail_naoEhAceitoComoLinkGenerico() {
        Usuario dono = criarUsuario("dono-token@teste.com");
        AreaTrabalho area = criarArea(dono);
        Usuario usuario = criarUsuario("convidado@teste.com");

        Token tokenDeEmail = new Token();
        tokenDeEmail.setToken(UUID.randomUUID().toString());
        tokenDeEmail.setEmail(usuario.getEmail()); // marca como convite por e-mail, não link genérico
        tokenDeEmail.setAreaId(area.getId());
        tokenDeEmail.setExpiraEm(LocalDateTime.now().plusDays(1));
        tokenDeEmail.setUsado(false);
        entityManager.persistAndFlush(tokenDeEmail);

        var resultado = conviteAreaService.aceitarLinkGenerico(tokenDeEmail.getToken(), usuario);

        assertThat(resultado.sucesso()).isFalse();
    }

    @Test
    void tokenExpirado_falha() {
        Usuario dono = criarUsuario("dono-exp@teste.com");
        AreaTrabalho area = criarArea(dono);
        Usuario usuario = criarUsuario("convidado-exp@teste.com");
        Token token = criarTokenLink(area.getId(), PermissaoArea.EDITOR, LocalDateTime.now().minusMinutes(1));

        var resultado = conviteAreaService.aceitarLinkGenerico(token.getToken(), usuario);

        assertThat(resultado.sucesso()).isFalse();
    }

    @Test
    void areaDoTokenNaoExisteMais_falha() {
        Usuario usuario = criarUsuario("convidado-semarea@teste.com");
        Token token = criarTokenLink(999999L, PermissaoArea.EDITOR, LocalDateTime.now().plusDays(1));

        var resultado = conviteAreaService.aceitarLinkGenerico(token.getToken(), usuario);

        assertThat(resultado.sucesso()).isFalse();
    }

    @Test
    void tokenValido_usuarioNovo_criaParticipacaoComPermissaoDoToken() {
        Usuario dono = criarUsuario("dono-ok@teste.com");
        AreaTrabalho area = criarArea(dono);
        Usuario usuario = criarUsuario("convidado-ok@teste.com");
        Token token = criarTokenLink(area.getId(), PermissaoArea.ADMIN, LocalDateTime.now().plusDays(1));

        var resultado = conviteAreaService.aceitarLinkGenerico(token.getToken(), usuario);

        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.area().getId()).isEqualTo(area.getId());

        List<ParticipacaoArea> participacoes = participacaoAreaRepository.findByUsuario(usuario);
        assertThat(participacoes).extracting(ParticipacaoArea::getPermissao).containsExactly(PermissaoArea.ADMIN);
    }

    @Test
    void tokenSemPermissaoDefinida_usaEditorComoPadrao() {
        Usuario dono = criarUsuario("dono-padrao@teste.com");
        AreaTrabalho area = criarArea(dono);
        Usuario usuario = criarUsuario("convidado-padrao@teste.com");
        Token token = criarTokenLink(area.getId(), null, LocalDateTime.now().plusDays(1));

        var resultado = conviteAreaService.aceitarLinkGenerico(token.getToken(), usuario);

        assertThat(resultado.sucesso()).isTrue();
        List<ParticipacaoArea> participacoes = participacaoAreaRepository.findByUsuario(usuario);
        assertThat(participacoes).extracting(ParticipacaoArea::getPermissao).containsExactly(PermissaoArea.EDITOR);
    }

    @Test
    void usuarioJaParticipante_naoDuplicaParticipacao() {
        Usuario dono = criarUsuario("dono-dup@teste.com");
        AreaTrabalho area = criarArea(dono);
        Usuario usuario = criarUsuario("convidado-dup@teste.com");

        entityManager.persistAndFlush(new ParticipacaoArea(usuario, area, PermissaoArea.OBSERVADOR));
        Token token = criarTokenLink(area.getId(), PermissaoArea.ADMIN, LocalDateTime.now().plusDays(1));

        // Simula uma requisição HTTP nova: força o serviço a carregar a área do
        // zero, em vez de reaproveitar a coleção `participacoes` já em memória
        // (que, como objeto Java, não se atualiza sozinha quando a participação
        // é persistida por fora, direto pelo repositório).
        entityManager.clear();

        var resultado = conviteAreaService.aceitarLinkGenerico(token.getToken(), usuario);

        assertThat(resultado.sucesso()).isTrue();
        List<ParticipacaoArea> participacoes = participacaoAreaRepository.findByUsuario(usuario);
        // continua com uma única participação, na permissão original (OBSERVADOR) —
        // não foi duplicada nem promovida pro nível do token (ADMIN).
        assertThat(participacoes).extracting(ParticipacaoArea::getPermissao).containsExactly(PermissaoArea.OBSERVADOR);
    }
}
