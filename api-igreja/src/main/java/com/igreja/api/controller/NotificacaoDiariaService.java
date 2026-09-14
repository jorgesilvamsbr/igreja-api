package com.api.igreja.service;

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

    // Roda todos os dias às 07:00 da manhã
    @Scheduled(cron = "0 0 20 * * *")
    public void verificarENotificar() {
        LocalDate hoje = LocalDate.now();
        LocalDate amanha = hoje.plusDays(1);

        // 1. Buscar Aniversariantes do Dia
        List<Membro> aniversariantesHoje = membroRepository.findAll().stream()
                .filter(m -> m.getDataNascimento() != null &&
                        m.getDataNascimento().getMonth() == hoje.getMonth() &&
                        m.getDataNascimento().getDayOfMonth() == hoje.getDayOfMonth())
                .collect(Collectors.toList());

        // 2. Buscar Eventos de Hoje e de Amanhã
        List<Evento> eventosHoje = eventoRepository.findByDataEvento(hoje);
        List<Evento> eventosAmanha = eventoRepository.findByDataEvento(amanha);

        // Se não houver aniversariantes e nem eventos, cancela o envio
        if (aniversariantesHoje.isEmpty() && eventosHoje.isEmpty() && eventosAmanha.isEmpty()) {
            System.out.println("[JOB] Nenhum aniversariante ou evento registrado para hoje/amanhã. E-mail não enviado.");
            return;
        }

        // 3. Montar Corpo do E-mail em HTML
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        StringBuilder html = new StringBuilder("<h2>📋 Relatório Diário - SGM</h2>");

        if (!aniversariantesHoje.isEmpty()) {
            html.append("<h3>🎉 Aniversariantes de Hoje (").append(hoje.format(fmt)).append(")</h3><ul>");
            for (Membro m : aniversariantesHoje) {
                html.append("<li><b>").append(m.getNome()).append("</b> - ")
                    .append(m.getTelefone() != null ? m.getTelefone() : "Sem telefone").append("</li>");
            }
            html.append("</ul>");
        }

        if (!eventosHoje.isEmpty()) {
            html.append("<h3>📅 Eventos de Hoje (").append(hoje.format(fmt)).append(")</h3><ul>");
            for (Evento e : eventosHoje) {
                html.append("<li><b>").append(e.getTitulo()).append("</b> (")
                    .append(e.getHorario() != null ? e.getHorario() : "Sem horário").append(") - ")
                    .append(e.getDescricao() != null ? e.getDescricao() : "").append("</li>");
            }
            html.append("</ul>");
        }

        if (!eventosAmanha.isEmpty()) {
            html.append("<h3>📌 Eventos de Amanhã (").append(amanha.format(fmt)).append(")</h3><ul>");
            for (Evento e : eventosAmanha) {
                html.append("<li><b>").append(e.getTitulo()).append("</b> (")
                    .append(e.getHorario() != null ? e.getHorario() : "Sem horário").append(") - ")
                    .append(e.getDescricao() != null ? e.getDescricao() : "").append("</li>");
            }
            html.append("</ul>");
        }

        // 4. Disparar E-mail
        enviarEmail(
            "⛪ Agenda & Aniversários - " + hoje.format(fmt), 
            html.toString()
        );
    }

    private void enviarEmail(String assunto, String conteudoHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(conteudoHtml, true);

            mailSender.send(message);
            System.out.println("[JOB] E-mail de notificação enviado com sucesso!");
        } catch (MessagingException e) {
            System.err.println("[JOB] Erro ao enviar e-mail: " + e.getMessage());
        }
    }
}
