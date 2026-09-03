package com.example.demo.ConsultasBD;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entidades.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByAreaId(Long areaId);
}
