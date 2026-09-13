package com.igreja.api.controller;

import com.igreja.api.controller.dto.DashboardStatsDTO;
import com.igreja.api.model.Membro;
import com.igreja.api.repository.EventoRepository;
import com.igreja.api.repository.MembroRepository;
import java.time.temporal.TemporalAdjusters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private MembroRepository membroRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @GetMapping("/stats")
    public DashboardStatsDTO obterEstatisticas() {
        List<Membro> membros = membroRepository.findAll();
        LocalDate hoje = LocalDate.now();

        // Totais dos Cards
        long totalMembros = membros.size();

        LocalDate primeiroDiaMes = hoje.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate ultimoDiaMes = hoje.with(TemporalAdjusters.lastDayOfMonth());
        long eventosNoMes = eventoRepository.findAll().stream()
                .filter(e -> e.getDataEvento() != null
                        && !e.getDataEvento().isBefore(primeiroDiaMes)
                        && !e.getDataEvento().isAfter(ultimoDiaMes))
                .count();

        long novosMembrosAno = membros.stream()
                .filter(m -> m.getInicioMembresia() != null && m.getInicioMembresia().getYear() == hoje.getYear())
                .count();

        // Distribuição de Gênero
        Map<String, Long> generos = membros.stream()
                .filter(m -> m.getGenero() != null)
                .collect(Collectors.groupingBy(Membro::getGenero, Collectors.counting()));

        // Agrupamento por Idade
        Map<String, Long> idades = new LinkedHashMap<>();
        idades.put("0-12", 0L); idades.put("13-17", 0L); idades.put("18-30", 0L); idades.put("31-50", 0L); idades.put("51+", 0L);

        // Agrupamento por Tempo de Membresia
        Map<String, Long> membresia = new LinkedHashMap<>();
        membresia.put("< 1 Ano", 0L); membresia.put("1-3 Anos", 0L); membresia.put("3-5 Anos", 0L); membresia.put("5-10 Anos", 0L); membresia.put("10+ Anos", 0L);

        // Agrupamento por Tempo de Batismo
        Map<String, Long> batismo = new LinkedHashMap<>();
        batismo.put("Não Batizados", 0L); batismo.put("< 1 Ano", 0L); batismo.put("1-5 Anos", 0L); batismo.put("5-10 Anos", 0L); batismo.put("10+ Anos", 0L);

        for (Membro m : membros) {
            // Processa Idade
            if (m.getDataNascimento() != null) {
                int idade = Period.between(m.getDataNascimento(), hoje).getYears();
                if (idade <= 12) idades.put("0-12", idades.get("0-12") + 1);
                else if (idade <= 17) idades.put("13-17", idades.get("13-17") + 1);
                else if (idade <= 30) idades.put("18-30", idades.get("18-30") + 1);
                else if (idade <= 50) idades.put("31-50", idades.get("31-50") + 1);
                else idades.put("51+", idades.get("51+") + 1);
            }

            // Processa Tempo de Membresia
            if (m.getInicioMembresia() != null) {
                int anos = Period.between(m.getInicioMembresia(), hoje).getYears();
                if (anos < 1) membresia.put("< 1 Ano", membresia.get("< 1 Ano") + 1);
                else if (anos <= 3) membresia.put("1-3 Anos", membresia.get("1-3 Anos") + 1);
                else if (anos <= 5) membresia.put("3-5 Anos", membresia.get("3-5 Anos") + 1);
                else if (anos <= 10) membresia.put("5-10 Anos", membresia.get("5-10 Anos") + 1);
                else membresia.put("10+ Anos", membresia.get("10+ Anos") + 1);
            }

            // Processa Tempo de Batismo
            if (m.getDataBatismo() == null) {
                batismo.put("Não Batizados", batismo.get("Não Batizados") + 1);
            } else {
                int anos = Period.between(m.getDataBatismo(), hoje).getYears();
                if (anos < 1) batismo.put("< 1 Ano", batismo.get("< 1 Ano") + 1);
                else if (anos <= 5) batismo.put("1-5 Anos", batismo.get("1-5 Anos") + 1);
                else if (anos <= 10) batismo.put("5-10 Anos", batismo.get("5-10 Anos") + 1);
                else batismo.put("10+ Anos", batismo.get("10+ Anos") + 1);
            }
        }

        return DashboardStatsDTO.builder()
                .totalMembros(totalMembros)
                .eventosNoMes(eventosNoMes)
                .novosMembrosAno(novosMembrosAno)
                .distribuicaoGenero(generos)
                .distribuicaoIdade(idades)
                .tempoMembresia(membresia)
                .tempoBatismo(batismo)
                .build();
    }
}