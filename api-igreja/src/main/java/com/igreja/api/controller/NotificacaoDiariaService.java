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
    
        // Container Wrapper
        html.append("<div style='background-color: #f1f5f9; padding: 30px 10px; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; color: #0f172a;'>");
        
        // Card Principal
        html.append("<div style='max-width: 560px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); border: 1px solid #e2e8f0;'>");
    
        // Header
        html.append("<div style='background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%); color: #ffffff; padding: 28px 20px; text-align: center;'>");
        html.append("<div style='font-size: 28px; margin-bottom: 6px;'>⛪</div>");
        html.append("<h1 style='margin: 0; font-size: 20px; font-weight: 700; letter-spacing: -0.5px;'>Relatório Diário — SGM</h1>");
        html.append("<p style='margin: 6px 0 0 0; font-size: 13px; color: #bfdbfe; font-weight: 500;'>").append(hoje.format(fmt)).append("</p>");
        html.append("</div>");
    
        // Corpo
        html.append("<div style='padding: 24px;'>");
    
        // 1. Aniversariantes de Hoje
        if (!aniversariantesHoje.isEmpty()) {
            html.append("<div style='margin-bottom: 24px;'>");
            html.append("<div style='border-bottom: 2px solid #fef3c7; padding-bottom: 6px; margin-bottom: 12px;'>");
            html.append("<h2 style='color: #b45309; margin: 0; font-size: 15px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;'>🎉 Aniversariantes de Hoje</h2>");
            html.append("</div>");
    
            for (Membro m : aniversariantesHoje) {
                String linkWa = gerarLinkWhatsapp(m.getTelefone(), m.getNome());
                String telefoneExibicao = (m.getTelefone() != null && !m.getTelefone().isBlank()) ? m.getTelefone() : "Não informado";
                
                html.append("<div style='background-color: #fffbeb; border: 1px solid #fde68a; border-radius: 12px; padding: 14px 16px; margin-bottom: 10px;'>");
                html.append("<table width='100%' border='0' cellspacing='0' cellpadding='0'><tr>");
                
                // Coluna Esquerda: Nome e Telefone Exposto
                html.append("<td>");
                html.append("<div style='font-weight: 600; font-size: 15px; color: #78350f;'>👤 ").append(m.getNome()).append("</div>");
                html.append("<div style='font-size: 13px; color: #b45309; margin-top: 4px;'>📞 <b>Contato:</b> ").append(telefoneExibicao).append("</div>");
                html.append("</td>");
                
                // Coluna Direita: Botão de Ação
                if (linkWa != null) {
                    html.append("<td align='right' style='vertical-align: middle;'>");
                    html.append("<a href='").append(linkWa).append("' target='_blank' style='display: inline-block; background-color: #25d366; color: #ffffff; text-decoration: none; padding: 8px 14px; border-radius: 20px; font-weight: 600; font-size: 12px;'>💬 WhatsApp</a>");
                    html.append("</td>");
                }
                
                html.append("</tr></table>");
                html.append("</div>");
            }
            html.append("</div>");
        }
    
        // 2. Eventos de Hoje
        if (!eventosHoje.isEmpty()) {
            html.append("<div style='margin-bottom: 24px;'>");
            html.append("<div style='border-bottom: 2px solid #d1fae5; padding-bottom: 6px; margin-bottom: 12px;'>");
            html.append("<h2 style='color: #047857; margin: 0; font-size: 15px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;'>📅 Eventos de Hoje</h2>");
            html.append("</div>");
    
            for (Evento e : eventosHoje) {
                String hora = e.getHorario() != null ? e.getHorario().toString() : "--:--";
                html.append("<div style='background-color: #f0fdf4; border: 1px solid #bbf7d0; border-left: 4px solid #10b981; padding: 12px 16px; margin-bottom: 10px; border-radius: 8px;'>");
                html.append("<div style='font-weight: 600; color: #065f46; font-size: 15px;'>").append(e.getTitulo()).append("</div>");
                html.append("<div style='margin-top: 6px; font-size: 13px; color: #047857;'>");
                html.append("<span style='background-color: #dcfce7; color: #15803d; padding: 2px 8px; border-radius: 4px; font-weight: 700; margin-right: 6px;'>⏰ ").append(hora).append("</span>");
                if (e.getDescricao() != null && !e.getDescricao().isBlank()) {
                    html.append("<span>").append(e.getDescricao()).append("</span>");
                }
                html.append("</div></div>");
            }
            html.append("</div>");
        }
    
        // 3. Eventos de Amanhã
        if (!eventosAmanha.isEmpty()) {
            html.append("<div>");
            html.append("<div style='border-bottom: 2px solid #e0e7ff; padding-bottom: 6px; margin-bottom: 12px;'>");
            html.append("<h2 style='color: #4338ca; margin: 0; font-size: 15px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;'>📌 Eventos de Amanhã (").append(amanha.format(fmt)).append(")</h2>");
            html.append("</div>");
    
            for (Evento e : eventosAmanha) {
                String hora = e.getHorario() != null ? e.getHorario().toString() : "--:--";
                html.append("<div style='background-color: #eef2ff; border: 1px solid #c7d2fe; border-left: 4px solid #6366f1; padding: 12px 16px; margin-bottom: 10px; border-radius: 8px;'>");
                html.append("<div style='font-weight: 600; color: #3730a3; font-size: 15px;'>").append(e.getTitulo()).append("</div>");
                html.append("<div style='margin-top: 6px; font-size: 13px; color: #4338ca;'>");
                html.append("<span style='background-color: #e0e7ff; color: #3730a3; padding: 2px 8px; border-radius: 4px; font-weight: 700; margin-right: 6px;'>⏰ ").append(hora).append("</span>");
                if (e.getDescricao() != null && !e.getDescricao().isBlank()) {
                    html.append("<span>").append(e.getDescricao()).append("</span>");
                }
                html.append("</div></div>");
            }
            html.append("</div>");
        }
    
        html.append("</div>"); // fim do corpo
    
        // Footer
        html.append("<div style='background-color: #f8fafc; border-top: 1px solid #f1f5f9; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8;'>");
        html.append("Sistema de Gestão de Membros • Notificação Automática");
        html.append("</div>");
    
        html.append("</div></div>"); // fim do card e wrapper
    
        enviarEmailViaHttp(destinatario, "⛪ Notificação Diária - " + hoje.format(fmt), html.toString());
    }
    
    /**
     * Gera URL compatível com dispositivos móveis (iOS / Android) usando api.whatsapp.com
     */
    private String gerarLinkWhatsapp(String telefone, String nome) {
        if (telefone == null || telefone.isBlank()) return null;
    
        // Remove qualquer caractere que não seja número
        String apenasNumeros = telefone.replaceAll("[^0-9]", "");
        if (apenasNumeros.isEmpty()) return null;
    
        // Adiciona o DDI 55 (Brasil) caso o número contenha apenas DDD + Telefone (10 ou 11 dígitos)
        if (apenasNumeros.length() == 10 || apenasNumeros.length() == 11) {
            apenasNumeros = "55" + apenasNumeros;
        }
    
        // Pega apenas o primeiro nome para deixar o cumprimento mais pessoal
        String primeiroNome = (nome != null && !nome.isBlank()) ? nome.trim().split(" ")[0] : "";
    
        // Mensagem padrão pessoal e cristã
        String mensagem = "Graça e Paz, " + primeiroNome + "! ✝️🎉 Passando para te desejar um feliz aniversário! "
                + "Que o Senhor abençoe ricamente a sua vida, renove as suas forças e te encha de paz, saúde e alegria neste novo ciclo. Um grande abraço!";
        
        // api.whatsapp.com é nativamente suportado pelos navegadores móbiles para redirecionamento direto
        return "https://api.whatsapp.com/send?phone=" + apenasNumeros + "&text=" + URLEncoder.encode(mensagem, StandardCharsets.UTF_8);
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
