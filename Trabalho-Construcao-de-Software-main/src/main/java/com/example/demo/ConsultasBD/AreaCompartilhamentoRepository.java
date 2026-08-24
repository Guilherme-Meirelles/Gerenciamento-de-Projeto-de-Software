package com.example.demo.ConsultasBD;

import com.example.demo.Entidades.AreaCompartilhamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaCompartilhamentoRepository extends JpaRepository<AreaCompartilhamento, Long> {

    AreaCompartilhamentoRepository findAreaCompartilhamentoById(Long id);
}
