package com.example.demo.ConsultasBD;

import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AreaTrabalhoRepository extends JpaRepository<AreaTrabalho, Long> {
    AreaTrabalho findAreaById(Long id);
    List<AreaTrabalho> findByDono(Usuario dono);
}
