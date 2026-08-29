package com.example.demo.ConsultasBD;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.Entidades.Lista;
import com.example.demo.Entidades.AreaTrabalho;

public interface ListaRepository extends JpaRepository<Lista, Long> {
    List<Lista> findByAreaId(Long areaId);
}