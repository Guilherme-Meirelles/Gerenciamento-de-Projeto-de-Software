package com.example.demo.ConsultasBD;

import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Entidades.AreaTrabalho;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParticipacaoAreaRepository extends JpaRepository<ParticipacaoArea, Long> {
    List<ParticipacaoArea> findByUsuario(Usuario usuario);
    List<ParticipacaoArea> findByArea(AreaTrabalho area);
    ParticipacaoArea findByUsuarioAndArea(Usuario usuario, AreaTrabalho area);
}
