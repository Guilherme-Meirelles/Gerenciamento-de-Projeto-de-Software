package com.example.demo.ConsultasBD;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entidades.Checklist;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {
}
