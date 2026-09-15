package com.igreja.api.controller;

import com.igreja.api.model.*;
import com.igreja.api.repository.EventoRepository;
import com.igreja.api.repository.MembroRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificacaoDiariaService {

    private final MembroRepository membroRepository;
    private final EventoRepository eventoRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    @Value("${notificacao.email.destinatario}")
    private String destinatario;

    public NotificacaoDiariaService(MembroRepository membroRepository, 
                                    EventoRepository eventoRepository, 
                                    JavaMailSender mailSender) {
        this.membroRepository = membroRepository;
        this.eventoRepository = eventoRepository;
        this.mailSender = mailSender;
    }

    // Executa todos os dias às 07:00 da manhã
    @Scheduled(cron = "0 0 7 * * *")
    public void verificarENotificar() {
        LocalDate hoje = LocalDate.now();
        LocalDate amanha = hoje.plusDays(1);

        // 1. Filtrar Aniversariantes do Dia
        List<Membro> aniversariantesHoje = membroRepository.findAll().stream()
                .filter(m -> m.getDataNascimento() != null &&
                        m.getDataNascimento().getMonth() == hoje.getMonth() &&
                        m.getDataNascimento().getDayOfMonth() == hoje.getDayOfMonth())
                .collect(Collectors.toList());

        // 2. Buscar Eventos de Hoje e de Amanhã
        List<Evento> eventosHoje = eventoRepository.findByDataEvento(hoje);
        List<Evento> eventosAmanha = eventoRepository.findByDataEvento(amanha);

        // Regra: Se não houver aniversariantes E nem eventos, não envia o e-mail
        if (aniversariantesHoje.isEmpty() && eventosHoje.isEmpty() && eventosAmanha.isEmpty()) {
            System.out.println("[JOB] Sem aniversariantes ou eventos hoje/amanhã. E-mail não enviado.");
            return;
        }

        // 3. Montagem do Corpo do E-mail (Texto formatado em HTML diretamente no e-mail)
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder html = new StringBuilder();
        
        html.append("<div style='font-family: Arial, sans-serif; color: #333;'>");
        html.append("<h2 style='color: #2563eb;'>⛪ Relatório Diário - SGM</h2>");

        // Seção de Aniversariantes com Nome e Telefone em destaque
        if (!aniversariantesHoje.isEmpty()) {
            html.append("<h3 style='color: #d97706;'>🎉 Aniversariantes de Hoje (").append(hoje.format(fmt)).append(")</h3>");
            html.append("<ul style='line-height: 1.6;'>");
            for (Membro m : aniversariantesHoje) {
                String fone = (m.getTelefone() != null && !m.getTelefone().isBlank()) ? m.getTelefone() : "Telefone não cadastrado";
                html.append("<li>👤 <b>").append(m.getNome()).append("</b> - 📞 Contato: <b>").append(fone).append("</b></li>");
            }
            html.append("</ul>");
        }

        // Seção de Eventos de Hoje
        if (!eventosHoje.isEmpty()) {
            html.append("<h3 style='color: #059669;'>📅 Eventos de Hoje (").append(hoje.format(fmt)).append(")</h3>");
            html.append("<ul style='line-height: 1.6;'>");
            for (Evento e : eventosHoje) {
                String hora = e.getHorario() != null ? e.getHorario().toString() : "Horário não informado";
                String desc = e.getDescricao() != null ? " - " + e.getDescricao() : "";
                html.append("<li><b>").append(e.getTitulo()).append("</b> às <b>").append(hora).append("</b>").append(desc).append("</li>");
            }
            html.append("</ul>");
        }

        // Seção de Eventos de Amanhã
        if (!eventosAmanha.isEmpty()) {
            html.append("<h3 style='color: #4f46e5;'>📌 Eventos de Amanhã (").append(amanha.format(fmt)).append(")</h3>");
            html.append("<ul style='line-height: 1.6;'>");
            for (Evento e : eventosAmanha) {
                String hora = e.getHorario() != null ? e.getHorario().toString() : "Horário não informado";
                String desc = e.getDescricao() != null ? " - " + e.getDescricao() : "";
                html.append("<li><b>").append(e.getTitulo()).append("</b> às <b>").append(hora).append("</b>").append(desc).append("</li>");
            }
            html.append("</ul>");
        }

        html.append("</div>");

        // 4. Disparo do e-mail
        enviarEmailViaHttp(destinatario, "⛪ Notificação Diária - " + hoje.format(fmt), html.toString());
    }

public void enviarEmailViaHttp(String para, String assunto, String corpo) {
    String url = "https://api.resend.com/emails";
    String apiKey = System.getenv("RESEND_API_KEY");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    String body = """
        {
          "from": "sib@sib.dev",
          "to": ["%s"],
          "subject": "%s",
          "html": "<p>%s</p>"
        }
        """.formatted(para, assunto, corpo);

    HttpEntity<String> request = new HttpEntity<>(body, headers);
    RestTemplate restTemplate = new RestTemplate();

    try {
        restTemplate.postForEntity(url, request, String.class);
    } catch (Exception e) {
        System.err.println("Erro ao enviar e-mail via API HTTP: " + e.getMessage());
    }
}
}
