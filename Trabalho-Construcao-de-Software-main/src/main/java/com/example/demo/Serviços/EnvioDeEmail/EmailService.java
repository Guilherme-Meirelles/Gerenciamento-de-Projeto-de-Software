package com.example.demo.Serviços.EnvioDeEmail;

import com.example.demo.Entidades.AreaTrabalho;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import com.example.demo.Entidades.Usuario;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarEmailDeTeste(String destinatario) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destinatario);
        mensagem.setSubject("Recuperação de Senha");
        mensagem.setText("Este é um teste de envio de e-mail usando Mailtrap + Spring Boot.");

        mailSender.send(mensagem);
    }

    public void enviarEmailRecuperacaoSenha(String destinatarioEmail, String destinatarioNome, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8"); // UTF-8 e Multipart ativados

            helper.setFrom("noreply@todaily.com");
            helper.setSubject("Redefinição de senha ToDaily");
            helper.setTo(destinatarioEmail);

            String template = carregaTemplateEmail("templates/email_template.html");

            // Garante que o replace só acontece se tiver o nome (evita erro de Null)
            template = template.replace("{token}", token);
            if (destinatarioNome != null) {
                template = template.replace("{nome}", destinatarioNome);
            }

            helper.setText(template, true); // O 'true' avisa que é HTML

            mailSender.send(message);
            System.out.println("Email enviado com sucesso!");

        } catch (Exception e) {
            // ISSO É O IMPORTANTE: Imprime o erro real no console
            e.printStackTrace();
            // Joga o erro para o Controller, para o Front saber que falhou
            throw new RuntimeException("Erro ao enviar email: " + e.getMessage());
        }
    }

    public void enviarEmailCompartilharAreaTrabalho(Usuario destinatario, String nomeUsuario, AreaTrabalho area, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8"); // UTF-8 e Multipart ativados

            helper.setFrom("noreply@todaily.com");
            helper.setSubject("Compartilhamento de Área de Trabalho ToDaily");
            helper.setTo(destinatario.getEmail());

            String template = carregaTemplateEmail("templates/compartilhamentoAreaTrabalho.html");

            // Garante que o replace só acontece se tiver o nome (evita erro de Null)



            template = template.replace("{nome}", destinatario.getNome());
            template = template.replace("{token}", token);
            template = template.replace("{usuario}", nomeUsuario);
            template = template.replace("{area}", area.getNome());
            template = template.replace("{areaID}", area.getId().toString());
            template = template.replace("{destID}", destinatario.getId().toString());


            helper.setText(template, true); // O 'true' avisa que é HTML

            mailSender.send(message);
            System.out.println("Email enviado com sucesso!");

        } catch (Exception e) {
            // ISSO É O IMPORTANTE: Imprime o erro real no console
            e.printStackTrace();
            // Joga o erro para o Controller, para o Front saber que falhou
            throw new RuntimeException("Erro ao enviar email: " + e.getMessage());
        }
    }

    public String carregaTemplateEmail(String arquivoHTML) throws IOException {
        // Certifique-se que o arquivo está em: src/main/resources/templates/email_template.html
        ClassPathResource resource = new ClassPathResource(arquivoHTML);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}



