package com.igreja.api.repository;

import com.igreja.api.model.Membro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MembroRepository extends JpaRepository<Membro, Long> {
    List<Membro> findByNomeContainingIgnoreCase(String nome);
}