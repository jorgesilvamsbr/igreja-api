package com.igreja.api.controller.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class DashboardStatsDTO {
    private long totalMembros;
    private long eventosNoMes;
    private long novosMembrosAno;
    private Map<String, Long> distribuicaoIdade;
    private Map<String, Long> distribuicaoGenero;
    private Map<String, Long> tempoMembresia;
    private Map<String, Long> tempoBatismo;
}