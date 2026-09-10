package com.example.demo.ConsultasBD;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.Entidades.Tarefa;

@Repository
public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
    List<Tarefa> findByListaOrigemId(Long listaId);
    List<Tarefa> findByChecklistId(Long checklistId);
    List<Tarefa> findByCategoriasId(Long categoriaId);

    // Tarefas com notificação ligada, ainda não concluídas, vencendo entre hoje e o limite
    // informado, e que ainda não receberam o e-mail de lembrete — usado pelo job de lembretes.
    // lembreteEnviado/status tratados como "não" quando nulos (linhas antigas de antes da coluna existir).
    @Query("SELECT t FROM Tarefa t WHERE t.notificacoes = true "
            + "AND (t.status IS NULL OR t.status = false) "
            + "AND (t.lembreteEnviado IS NULL OR t.lembreteEnviado = false) "
            + "AND t.dataFim BETWEEN :inicio AND :limite")
    List<Tarefa> buscarTarefasParaLembrete(@Param("inicio") LocalDate inicio, @Param("limite") LocalDate limite);
}
