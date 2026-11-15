package com.example.demo.Serviços;

import com.example.demo.Serviços.Idioma.Mensagens;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ConsultasBD.ChecklistRepository;
import com.example.demo.ConsultasBD.ItemChecklistRepository;
import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.Entidades.Checklist;
import com.example.demo.Entidades.ItemChecklist;
import com.example.demo.Entidades.Tarefa;

@Service
public class ChecklistService {

    @Autowired
    private ChecklistRepository checklistRepository;

    @Autowired
    private ItemChecklistRepository itemChecklistRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    public Checklist getChecklistDaTarefa(Long tarefaId) {
        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new RuntimeException(Mensagens.get("msg.tarefa.naoEncontrada")));
        return tarefa.getChecklist();
    }

    @Transactional
    public Checklist obterOuCriarChecklist(Long tarefaId) {
        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new RuntimeException(Mensagens.get("msg.tarefa.naoEncontrada")));

        if (tarefa.getChecklist() == null) {
            Checklist checklist = new Checklist();
            checklistRepository.save(checklist);
            tarefa.setChecklist(checklist);
            tarefaRepository.save(tarefa);
        }

        return tarefa.getChecklist();
    }

    @Transactional
    public ItemChecklist adicionarItem(Long tarefaId, String descricao) {
        Checklist checklist = obterOuCriarChecklist(tarefaId);

        ItemChecklist item = new ItemChecklist();
        item.setChecklist(checklist);
        item.setDescricao(descricao);
        item.setConcluido(false);

        return itemChecklistRepository.save(item);
    }

    public ItemChecklist editarItem(Long itemId, String descricao, Boolean concluido) {
        ItemChecklist item = itemChecklistRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException(Mensagens.get("msg.checklist.itemNaoEncontrado")));

        if (descricao != null) item.setDescricao(descricao);
        if (concluido != null) item.setConcluido(concluido);

        return itemChecklistRepository.save(item);
    }

    public void removerItem(Long itemId) {
        itemChecklistRepository.deleteById(itemId);
    }
}
