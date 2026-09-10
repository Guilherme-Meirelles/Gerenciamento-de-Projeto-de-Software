package com.example.demo.Serviços;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.ConsultasBD.AnexoRepository;
import com.example.demo.ConsultasBD.TarefaRepository;
import com.example.demo.Entidades.Anexo;
import com.example.demo.Entidades.Tarefa;

@Service
public class AnexoService {

    @Autowired
    private AnexoRepository anexoRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    public List<Anexo> listarPorTarefa(Long tarefaId) {
        return anexoRepository.findByTarefaId(tarefaId);
    }

    public Anexo adicionar(Long tarefaId, MultipartFile arquivo) throws IOException {
        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new RuntimeException("Tarefa não encontrada"));

        Anexo anexo = new Anexo();
        anexo.setTarefa(tarefa);
        anexo.setNomeArquivo(arquivo.getOriginalFilename());
        anexo.setTipoConteudo(arquivo.getContentType());
        anexo.setTamanho(arquivo.getSize());
        anexo.setDados(arquivo.getBytes());

        return anexoRepository.save(anexo);
    }

    public Anexo buscarPorId(Long anexoId) {
        return anexoRepository.findById(anexoId)
                .orElseThrow(() -> new RuntimeException("Anexo não encontrado"));
    }

    public void remover(Long anexoId) {
        anexoRepository.deleteById(anexoId);
    }
}
