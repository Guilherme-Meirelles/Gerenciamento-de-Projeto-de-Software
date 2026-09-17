package com.example.demo.Serviços;

import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Anexo;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(AnexoService.class)
class AnexoServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AnexoService anexoService;

    private Tarefa tarefa;

    private void montarTarefa() {
        Usuario dono = new Usuario();
        dono.setNome("dono");
        dono.setEmail("dono-anexo@teste.com");
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
    void adicionar_salvaNomeTipoTamanhoEDados() throws Exception {
        montarTarefa();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "prova.pdf", "application/pdf", "conteúdo-fake".getBytes());

        Anexo anexo = anexoService.adicionar(tarefa.getId(), arquivo);

        assertThat(anexo.getId()).isNotNull();
        assertThat(anexo.getNomeArquivo()).isEqualTo("prova.pdf");
        assertThat(anexo.getTipoConteudo()).isEqualTo("application/pdf");
        assertThat(anexo.getTamanho()).isEqualTo(arquivo.getSize());
        assertThat(anexo.getDados()).isEqualTo("conteúdo-fake".getBytes());
    }

    @Test
    void listarPorTarefa_retornaSoOsAnexosDaquelaTarefa() throws Exception {
        montarTarefa();
        MockMultipartFile arquivo1 = new MockMultipartFile("arquivo", "a.txt", "text/plain", "1".getBytes());
        MockMultipartFile arquivo2 = new MockMultipartFile("arquivo", "b.txt", "text/plain", "2".getBytes());
        anexoService.adicionar(tarefa.getId(), arquivo1);
        anexoService.adicionar(tarefa.getId(), arquivo2);

        assertThat(anexoService.listarPorTarefa(tarefa.getId())).hasSize(2);
    }

    @Test
    void remover_apagaOAnexo() throws Exception {
        montarTarefa();
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "a.txt", "text/plain", "1".getBytes());
        Anexo anexo = anexoService.adicionar(tarefa.getId(), arquivo);

        anexoService.remover(anexo.getId());

        assertThat(anexoService.listarPorTarefa(tarefa.getId())).isEmpty();
    }

    @Test
    void buscarPorId_inexistente_lancaExcecao() {
        assertThatThrownBy(() -> anexoService.buscarPorId(999L)).isInstanceOf(RuntimeException.class);
    }
}
