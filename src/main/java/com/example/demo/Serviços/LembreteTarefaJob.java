package com.example.demo.Serviços;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

    // Todo dia às 08:00, no fuso do servidor.
    // @Transactional: mantém a sessão do Hibernate aberta pra acessar a coleção lazy
    // tarefa.getResponsaveis() (senão dá LazyInitializationException fora do @Scheduled).
    @Scheduled(cron = "0 0 8 * * *")
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
