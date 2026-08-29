package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.ListaRepository;
import com.example.demo.ConsultasBD.AreaTrabalhoRepository;
import com.example.demo.Entidades.Lista;

import lombok.RequiredArgsConstructor;

import com.example.demo.Entidades.AreaTrabalho;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ListaService {

    @Autowired
    private ListaRepository listaRepository;

    @Autowired
    private AreaTrabalhoRepository areaRepository;

    public Lista criarLista(Long areaId, String nome, String descricao) {
        AreaTrabalho area = areaRepository.findById(areaId)
                .orElseThrow(() -> new RuntimeException("Área não encontrada"));

        Lista lista = new Lista();
        lista.setNome(nome);
        lista.setDescricao(descricao);
        lista.setArea(area);

        return listaRepository.save(lista);
    }

    public Lista getLista(long listaId){
        Lista lista = listaRepository.findById(listaId).orElseThrow(() -> new RuntimeException("Lista não encontrada"));
        return lista;
    }

    public Lista editarLista(Long id, String nome, String desc) {
        Lista lista = listaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lista não encontrada"));

        lista.setNome(nome);
        lista.setDescricao(desc);
        return listaRepository.save(lista);
    }

    public void deletarLista(Long id) {
        listaRepository.deleteById(id);
    }

    public List<Lista> listarPorArea(Long areaId) {
        return listaRepository.findByAreaId(areaId);
    }
}