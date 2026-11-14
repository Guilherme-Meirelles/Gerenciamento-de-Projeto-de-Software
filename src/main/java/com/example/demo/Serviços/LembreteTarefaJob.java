package com.example.demo.Serviços;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.Entidades.Tarefa;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Serviços.EnvioDeEmail.EmailService;

// Job diário que avisa por e-mail os responsáveis de tarefas com notificações
// ligadas (Tarefa.notificacoes) que estão vencendo (hoje ou amanhã) e ainda
// não foram concluídas. Cada tarefa só dispara um lembrete (Tarefa.lembreteEnviado),
// que é resetado em TarefaService.editarTarefa quando a dataFim muda.
@Component
public class LembreteTarefaJob {

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private EmailService emailService;

    // Quando o servidor sobe: recupera lembretes que ficariam sem enviar se o programa estivesse
    // desligado às 08:00 (comum em máquina de desenvolvimento). Cada tarefa só recebe um lembrete
    // (lembreteEnviado), então rodar de novo aqui não gera e-mail repetido.
    // @Transactional aqui também: a chamada interna a enviarLembretes() não passa pelo proxy.
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void enviarLembretesAoIniciar() {
        try {
            enviarLembretes();
        } catch (Exception e) {
            // falha aqui (banco, e-mail) não pode impedir o sistema de subir
            e.printStackTrace();
        }
    }

    // Todo dia às 08:00, no fuso do servidor. Para testar sem esperar, dá pra trocar o horário
    // pela variável de ambiente TODAILY_LEMBRETES_CRON (ex.: "0 * * * * *" roda todo minuto).
    // @Transactional: mantém a sessão do Hibernate aberta pra acessar a coleção lazy
    // tarefa.getResponsaveis() (senão dá LazyInitializationException fora do @Scheduled).
    @Scheduled(cron = "${todaily.lembretes.cron:0 0 8 * * *}")
    @Transactional
    public void enviarLembretes() {
        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(1);

        List<Tarefa> tarefas = tarefaRepository.buscarTarefasParaLembrete(hoje, limite);

        for (Tarefa tarefa : tarefas) {
            // Sem responsável não tem pra quem mandar; deixa lembreteEnviado=false pra
            // tentar de novo no próximo dia (até alguém ser atribuído ou a tarefa vencer).
            if (tarefa.getResponsaveis().isEmpty()) {
                continue;
            }

            for (Usuario responsavel : tarefa.getResponsaveis()) {
                // quem desligou o aviso por e-mail nas preferências não recebe
                if (!responsavel.isNotificarPorEmail()) {
                    continue;
                }
                try {
                    emailService.enviarEmailLembreteTarefa(responsavel, tarefa);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            tarefa.setLembreteEnviado(true);
            tarefaRepository.save(tarefa);
        }
    }
}
