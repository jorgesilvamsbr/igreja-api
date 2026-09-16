package com.igreja.api.repository;

import com.igreja.api.model.Membro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDate;

public interface MembroRepository extends JpaRepository<Membro, Long> {
    List<Membro> findByNomeContainingIgnoreCase(String nome);
    @Query("SELECT m FROM Membro m WHERE MONTH(m.dataNascimento) = MONTH(:data) AND DAY(m.dataNascimento) = DAY(:data)")
    List<Membro> findByDataNascimento(@Param("data")LocalDate dataEvento);
}
