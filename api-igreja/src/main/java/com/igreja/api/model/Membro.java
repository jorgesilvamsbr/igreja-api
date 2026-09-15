package com.igreja.api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "membros")
@Data
public class Membro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String genero;
    private LocalDate dataNascimento;
    private LocalDate dataBatismo;
    private LocalDate inicioMembresia;
    private String funcao;
    private String telefone;
    private String status;
    private LocalDate dataCasamento;
    private String formaEntrada;
}
