package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.AreaTrabalhoRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Usuario;
import org.springframework.stereotype.Service;

@Service
public class AreaTrabalhoService {

    private final AreaTrabalhoRepository repo;

    public AreaTrabalhoService(AreaTrabalhoRepository repo) {
        this.repo = repo;
    }

    public AreaTrabalho criarArea(String nome, Usuario dono) {
        AreaTrabalho area = new AreaTrabalho();
        area.setNome(nome);
        area.setDono(dono);
        return repo.save(area);
    }
}