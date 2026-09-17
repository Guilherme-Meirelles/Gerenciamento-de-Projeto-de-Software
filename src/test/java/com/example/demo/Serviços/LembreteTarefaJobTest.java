package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.EnvioDeEmail.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LembreteTarefaJobTest {

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private LembreteTarefaJob job;

    private Usuario criarResponsavel(long id, String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Resp " + id);
        usuario.setEmail(email);
        usuario.setSenha("hash");
        usuario.setDataNascimento("2000-01-01");
        // id é gerado pelo banco; aqui a tarefa só precisa de uma instância distinguível.
        return usuario;
    }

    private Tarefa criarTarefa(String titulo, Usuario... responsaveis) {
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(titulo);
        tarefa.setDataFim(LocalDate.now());
        tarefa.setLembreteEnviado(false);
        for (Usuario u : responsaveis) {
            tarefa.getResponsaveis().add(u);
        }
        return tarefa;
    }

    @Test
    void tarefaComResponsavel_envaEmailEMarcaLembreteEnviado() {
        Usuario responsavel = criarResponsavel(1, "resp1@teste.com");
        Tarefa tarefa = criarTarefa("Tarefa 1", responsavel);
        when(tarefaRepository.buscarTarefasParaLembrete(any(), any())).thenReturn(List.of(tarefa));

        job.enviarLembretes();

        verify(emailService, times(1)).enviarEmailLembreteTarefa(eq(responsavel), eq(tarefa));
        assertThat(tarefa.getLembreteEnviado()).isTrue();
        verify(tarefaRepository).save(tarefa);
    }

    @Test
    void tarefaSemResponsavel_naoEnviaEmailNemMarcaLembreteEnviado() {
        Tarefa tarefa = criarTarefa("Sem responsável");
        when(tarefaRepository.buscarTarefasParaLembrete(any(), any())).thenReturn(List.of(tarefa));

        job.enviarLembretes();

        verify(emailService, never()).enviarEmailLembreteTarefa(any(), any());
        assertThat(tarefa.getLembreteEnviado()).isFalse();
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void tarefaComVariosResponsaveis_envaUmEmailParaCadaUm() {
        Usuario r1 = criarResponsavel(1, "r1@teste.com");
        Usuario r2 = criarResponsavel(2, "r2@teste.com");
        Tarefa tarefa = criarTarefa("Tarefa em dupla", r1, r2);
        when(tarefaRepository.buscarTarefasParaLembrete(any(), any())).thenReturn(List.of(tarefa));

        job.enviarLembretes();

        verify(emailService).enviarEmailLembreteTarefa(eq(r1), eq(tarefa));
        verify(emailService).enviarEmailLembreteTarefa(eq(r2), eq(tarefa));
        assertThat(tarefa.getLembreteEnviado()).isTrue();
    }

    @Test
    void erroAoEnviarParaUmResponsavel_naoImpedeMarcarLembreteEnviado() {
        Usuario r1 = criarResponsavel(1, "falha@teste.com");
        Tarefa tarefa = criarTarefa("Com falha de envio", r1);
        when(tarefaRepository.buscarTarefasParaLembrete(any(), any())).thenReturn(List.of(tarefa));
        doThrow(new RuntimeException("SMTP fora do ar")).when(emailService)
                .enviarEmailLembreteTarefa(eq(r1), eq(tarefa));

        job.enviarLembretes();

        assertThat(tarefa.getLembreteEnviado()).isTrue();
        verify(tarefaRepository).save(tarefa);
    }
}
