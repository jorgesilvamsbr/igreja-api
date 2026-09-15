package com.igreja.api.controller;

import com.igreja.api.controller.NotificacaoDiariaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final NotificacaoDiariaService notificacaoService;

    // Defina uma chave secreta no seu application.properties / ENV
    @Value("${job.secret.token:minhaChaveSecreta123}")
    private String secretToken;

    public JobController(NotificacaoDiariaService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @PostMapping("/notificacao-diaria")
    public ResponseEntity<String> executarNotificacaoDiaria(@RequestHeader("X-Job-Token") String token) {
        // Valida se quem está chamando a API é o seu agendador autorizado
        // if (!secretToken.equals(token)) {
        //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        // }

        notificacaoService.verificarENotificar();
        return ResponseEntity.ok("Job de notificação diária executado com sucesso.");
    }
}
