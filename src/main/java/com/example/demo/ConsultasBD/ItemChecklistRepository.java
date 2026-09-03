package com.example.demo.ConsultasBD;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entidades.ItemChecklist;

public interface ItemChecklistRepository extends JpaRepository<ItemChecklist, Long> {
    List<ItemChecklist> findByChecklistIdOrderById(Long checklistId);
}
