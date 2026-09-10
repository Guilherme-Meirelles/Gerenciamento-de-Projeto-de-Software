package com.example.demo.ConsultasBD;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entidades.Anexo;

public interface AnexoRepository extends JpaRepository<Anexo, Long> {
    List<Anexo> findByTarefaId(Long tarefaId);
}
