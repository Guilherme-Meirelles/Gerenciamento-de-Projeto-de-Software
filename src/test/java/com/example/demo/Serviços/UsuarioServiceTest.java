package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.AreaTrabalhoRepository;
import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

// Regressão do bug relatado: excluir um usuário que possui área de trabalho
// própria (e outras coisas cadastradas nela) derrubava a aplicação, porque o
// delete violava a foreign key de AreaTrabalho.dono_id (sem cascade) e da
// tabela de junção responsaveis_tarefa. @DataJpaTest roda contra um banco H2
// real em memória para exercitar de fato o flush/DELETE no banco, não só o
// grafo de objetos em memória.
@DataJpaTest
@Import(UsuarioService.class)
class UsuarioServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManager em;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AreaTrabalhoRepository areaTrabalhoRepository;

    @Autowired
    private ParticipacaoAreaRepository participacaoAreaRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    private Usuario criarUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + email);
        usuario.setEmail(email);
        usuario.setSenha("hash-fake");
        usuario.setDataNascimento("2000-01-01");
        return entityManager.persistAndFlush(usuario);
    }

    @Test
    void excluirConta_usuarioComAreaPropriaEComoResponsavelEmAreaAlheia_removeTudoSemErro() {
        Usuario dono = criarUsuario("dono@teste.com");
        Usuario outroDono = criarUsuario("outroDono@teste.com");

        // Área própria do usuário que será excluído, com uma lista e uma
        // tarefa dentro dela ("outras coisas cadastradas" mencionadas no bug).
        AreaTrabalho areaPropria = new AreaTrabalho();
        areaPropria.setNome("Área do dono");
        areaPropria.setDono(dono);
        entityManager.persistAndFlush(areaPropria);

        ParticipacaoArea participacaoNaPropria = new ParticipacaoArea(dono, areaPropria, PermissaoArea.ADMIN);
        entityManager.persistAndFlush(participacaoNaPropria);

        Lista listaPropria = new Lista();
        listaPropria.setNome("Lista do dono");
        listaPropria.setArea(areaPropria);
        entityManager.persistAndFlush(listaPropria);

        Tarefa tarefaPropria = new Tarefa();
        tarefaPropria.setTitulo("Tarefa na área do dono");
        tarefaPropria.setListaOrigem(listaPropria);
        entityManager.persistAndFlush(tarefaPropria);

        // Área de um terceiro, na qual o usuário excluído é só convidado
        // (participação) e responsável por uma tarefa (tabela de junção
        // responsaveis_tarefa) — não deve ser afetada pela exclusão.
        AreaTrabalho areaAlheia = new AreaTrabalho();
        areaAlheia.setNome("Área de outro dono");
        areaAlheia.setDono(outroDono);
        entityManager.persistAndFlush(areaAlheia);

        ParticipacaoArea participacaoNaAlheia = new ParticipacaoArea(dono, areaAlheia, PermissaoArea.EDITOR);
        entityManager.persistAndFlush(participacaoNaAlheia);

        Lista listaAlheia = new Lista();
        listaAlheia.setNome("Lista de outro dono");
        listaAlheia.setArea(areaAlheia);
        entityManager.persistAndFlush(listaAlheia);

        Tarefa tarefaAlheiaComResponsavel = new Tarefa();
        tarefaAlheiaComResponsavel.setTitulo("Tarefa de outro dono, atribuída ao usuário excluído");
        tarefaAlheiaComResponsavel.setListaOrigem(listaAlheia);
        tarefaAlheiaComResponsavel.getResponsaveis().add(dono);
        entityManager.persistAndFlush(tarefaAlheiaComResponsavel);

        Long donoId = dono.getId();
        Long areaPropriaId = areaPropria.getId();
        Long listaPropriaId = listaPropria.getId();
        Long tarefaPropriaId = tarefaPropria.getId();
        Long areaAlheiaId = areaAlheia.getId();
        Long tarefaAlheiaId = tarefaAlheiaComResponsavel.getId();

        entityManager.clear();
        Usuario donoGerenciado = usuarioRepository.findById(donoId).orElseThrow();

        // Antes da correção, isso lançava DataIntegrityViolationException
        // (violação de FK) e não excluía nada.
        assertThatCode(() -> {
            usuarioService.excluirConta(donoGerenciado);
            em.flush();
        }).doesNotThrowAnyException();

        entityManager.clear();

        assertThat(usuarioRepository.findById(donoId)).isEmpty();

        // Área própria e tudo dentro dela some em cascata.
        assertThat(areaTrabalhoRepository.findById(areaPropriaId)).isEmpty();
        assertThat(entityManager.find(Lista.class, listaPropriaId)).isNull();
        assertThat(entityManager.find(Tarefa.class, tarefaPropriaId)).isNull();

        // Nenhuma ParticipacaoArea do usuário excluído sobra em lugar nenhum.
        boolean sobrouParticipacao = participacaoAreaRepository.findAll().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(donoId));
        assertThat(sobrouParticipacao).isFalse();

        // Área de terceiro continua intacta: só perde o vínculo do usuário excluído.
        assertThat(areaTrabalhoRepository.findById(areaAlheiaId)).isPresent();
        Tarefa tarefaAlheiaRecarregada = tarefaRepository.findById(tarefaAlheiaId).orElseThrow();
        assertThat(tarefaAlheiaRecarregada.getResponsaveis()).isEmpty();

        assertThat(usuarioRepository.findById(outroDono.getId())).isPresent();
    }

    @Test
    void excluirConta_usuarioSemVinculos_removeApenasOUsuario() {
        Usuario usuario = criarUsuario("semvinculos@teste.com");
        Long usuarioId = usuario.getId();

        assertThatCode(() -> {
            usuarioService.excluirConta(usuario);
            em.flush();
        }).doesNotThrowAnyException();

        assertThat(usuarioRepository.findById(usuarioId)).isEmpty();
    }
}
