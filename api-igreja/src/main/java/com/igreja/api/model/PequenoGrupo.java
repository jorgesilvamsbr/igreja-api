package com.igreja.api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import com.igreja.api.model.Membro;
import java.util.List;

@Entity
@Table(name = "pequenoGrupo")
@Data
public class PequenoGrupo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;
    @ManyToOne
    @JoinColumn(name = "lider_id")
    private Membro lider;
    @ManyToMany
    private List<Membro> membros;
    private String informacoes;
    private String local;
}
