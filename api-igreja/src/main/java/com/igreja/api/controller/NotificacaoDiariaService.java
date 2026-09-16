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
import com.resend.*;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
    
        List<Membro> aniversariantesHoje = membroRepository.findByDataNascimento(hoje);
        List<Evento> eventosHoje = eventoRepository.findByDataEvento(hoje);
        List<Evento> eventosAmanha = eventoRepository.findByDataEvento(amanha);
    
        if (aniversariantesHoje.isEmpty() && eventosHoje.isEmpty() && eventosAmanha.isEmpty()) {
            System.out.println("[JOB] Sem aniversariantes ou eventos hoje/amanhã. E-mail não enviado.");
            return;
        }
    
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder html = new StringBuilder();
    
        // Container Principal do E-mail
        html.append("<div style='max-width: 600px; margin: 0 auto; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; color: #1e293b; background-color: #f8fafc; padding: 20px;'>");
        
        // Cabeçalho
        html.append("<div style='background-color: #2563eb; color: #ffffff; padding: 20px; border-radius: 12px 12px 0 0; text-align: center;'>");
        html.append("<h2 style='margin: 0; font-size: 20px; font-weight: 700;'>⛪ Relatório Diário - SGM</h2>");
        html.append("<p style='margin: 4px 0 0 0; font-size: 14px; opacity: 0.9;'>").append(hoje.format(fmt)).append("</p>");
        html.append("</div>");
    
        html.append("<div style='background-color: #ffffff; padding: 24px; border-radius: 0 0 12px 12px; border: 1px solid #e2e8f0; border-top: none;'>");
    
        // 1. Seção de Aniversariantes
        if (!aniversariantesHoje.isEmpty()) {
            html.append("<div style='margin-bottom: 24px;'>");
            html.append("<h3 style='color: #d97706; margin: 0 0 12px 0; font-size: 16px; border-bottom: 2px solid #fef3c7; padding-bottom: 6px;'>🎉 Aniversariantes de Hoje</h3>");
            
            for (Membro m : aniversariantesHoje) {
                String linkWa = gerarLinkWhatsapp(m.getTelefone(), m.getNome());
                
                html.append("<div style='background-color: #fffbeb; border: 1px solid #fde68a; border-radius: 8px; padding: 12px 16px; margin-bottom: 10px;'>");
                html.append("<div style='font-weight: 600; font-size: 15px; color: #78350f;'>👤 ").append(m.getNome()).append("</div>");
                
                if (linkWa != null) {
                    html.append("<div style='margin-top: 8px;'>");
                    html.append("<a href='").append(linkWa).append("' target='_blank' style='display: inline-block; background-color: #25d366; color: #ffffff; text-decoration: none; padding: 8px 14px; border-radius: 6px; font-weight: 600; font-size: 13px;'>💬 Chamar no WhatsApp (").append(m.getTelefone()).append(")</a>");
                    html.append("</div>");
                } else {
                    html.append("<div style='font-size: 13px; color: #b45309; margin-top: 4px;'>📞 Telefone não cadastrado</div>");
                }
                html.append("</div>");
            }
            html.append("</div>");
        }
    
        // 2. Seção de Eventos de Hoje
        if (!eventosHoje.isEmpty()) {
            html.append("<div style='margin-bottom: 24px;'>");
            html.append("<h3 style='color: #059669; margin: 0 0 12px 0; font-size: 16px; border-bottom: 2px solid #d1fae5; padding-bottom: 6px;'>📅 Eventos de Hoje</h3>");
            
            for (Evento e : eventosHoje) {
                String hora = e.getHorario() != null ? e.getHorario().toString() : "Horário não informado";
                html.append("<div style='background-color: #ecfdf5; border-left: 4px solid #10b981; padding: 10px 14px; margin-bottom: 8px; border-radius: 0 6px 6px 0;'>");
                html.append("<div style='font-weight: 600; color: #065f46;'>").append(e.getTitulo()).append("</div>");
                html.append("<div style='font-size: 13px; color: #047857; margin-top: 2px;'>⏰ <b>").append(hora).append("</b>");
                if (e.getDescricao() != null && !e.getDescricao().isBlank()) {
                    html.append(" — ").append(e.getDescricao());
                }
                html.append("</div></div>");
            }
            html.append("</div>");
        }
    
        // 3. Seção de Eventos de Amanhã
        if (!eventosAmanha.isEmpty()) {
            html.append("<div>");
            html.append("<h3 style='color: #4f46e5; margin: 0 0 12px 0; font-size: 16px; border-bottom: 2px solid #e0e7ff; padding-bottom: 6px;'>📌 Eventos de Amanhã (").append(amanha.format(fmt)).append(")</h3>");
            
            for (Evento e : eventosAmanha) {
                String hora = e.getHorario() != null ? e.getHorario().toString() : "Horário não informado";
                html.append("<div style='background-color: #eef2ff; border-left: 4px solid #6366f1; padding: 10px 14px; margin-bottom: 8px; border-radius: 0 6px 6px 0;'>");
                html.append("<div style='font-weight: 600; color: #3730a3;'>").append(e.getTitulo()).append("</div>");
                html.append("<div style='font-size: 13px; color: #4338ca; margin-top: 2px;'>⏰ <b>").append(hora).append("</b>");
                if (e.getDescricao() != null && !e.getDescricao().isBlank()) {
                    html.append(" — ").append(e.getDescricao());
                }
                html.append("</div></div>");
            }
            html.append("</div>");
        }
    
        html.append("</div></div>");
    
        enviarEmailViaHttp(destinatario, "⛪ [SIB] -  Notificação Diária - " + hoje.format(fmt), html.toString());
    }
    
    /**
     * Trata o número e constrói a URL com mensagem pré-formatada para o WhatsApp
     */
    private String gerarLinkWhatsapp(String telefone, String nome) {
        if (telefone == null || telefone.isBlank()) return null;
    
        // Remove caracteres especiais (espaços, traços, parênteses)
        String apenasNumeros = telefone.replaceAll("[^0-9]", "");
        if (apenasNumeros.isEmpty()) return null;
    
        // Adiciona o DDI do Brasil (55) se a string contiver apenas o DDD + Número (10 ou 11 dígitos)
        if (apenasNumeros.length() == 10 || apenasNumeros.length() == 11) {
            apenasNumeros = "55" + apenasNumeros;
        }
    
        String mensagem = "Olá " + nome + ", parabéns pelo seu aniversário! Que Deus abençoe grandemente o seu dia! 🎉👏";
        return "https://wa.me/" + apenasNumeros + "?text=" + java.net.URLEncoder.encode(mensagem, java.nio.charset.StandardCharsets.UTF_8);
    }

    public void enviarEmailViaHttp(String para, String assunto, String corpo) {
        String apiKey = System.getenv("RESEND_KEY_API");
        Resend resend = new Resend(apiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Info SIB <sib@resend.dev>")
                .to(para)
                .subject(assunto)
                .html(corpo)
                .build();

         try {
            CreateEmailResponse data = resend.emails().send(params);
            System.out.println(data.getId());
        } catch (ResendException e) {
            e.printStackTrace();
        }
    }
}
