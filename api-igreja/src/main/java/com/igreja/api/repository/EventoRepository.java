package com.igreja.api.repository;

import java.util.List;
import java.time.LocalDate;
import com.igreja.api.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepository extends JpaRepository<Evento, Long> {
  List<Evento> findByDataEvento(LocalDate dataEvento);
}
