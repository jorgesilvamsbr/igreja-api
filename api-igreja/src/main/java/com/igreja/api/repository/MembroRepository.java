package com.igreja.api.repository;

import com.igreja.api.model.Membro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDate;

public interface MembroRepository extends JpaRepository<Membro, Long> {
    List<Membro> findByNomeContainingIgnoreCase(String nome);
    @Query("SELECT m FROM Membro m WHERE EXTRACT(MONTH FROM m.dataNascimento) = EXTRACT(MONTH FROM CAST(:data AS date)) AND EXTRACT(DAY FROM m.dataNascimento) = EXTRACT(DAY FROM CAST(:data AS date))")
    List<Membro> findByDataNascimento(@Param("data") LocalDate data);
}
