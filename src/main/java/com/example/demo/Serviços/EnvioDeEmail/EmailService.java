package com.example.demo.Serviços.EnvioDeEmail;

import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.PermissaoArea;
import com.example.demo.Entidades.Tarefa;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.example.demo.Entidades.Usuario;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;
import com.example.demo.Serviços.Idioma.Idioma;
import com.example.demo.Serviços.Idioma.Mensagens;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // O Gmail rejeita/reescreve o "From" quando ele não é a própria conta autenticada,
    // então o remetente precisa ser o mesmo usuário configurado em spring.mail.username.
    @Value("${mail.from.name}")
    private String remetenteNome;

    @Value("${spring.mail.username}")
    private String remetenteEmail;

    @Value("${app.base-url}")
    private String baseUrl;

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
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(remetenteEmail, remetenteNome);
            Locale idioma = Mensagens.localeAtual(); // quem pediu está usando a tela agora: vale o idioma dela
            helper.setSubject(Mensagens.get(idioma, "email.recup.assunto"));
            helper.setTo(destinatarioEmail);

            String template = traduzirTemplate(carregaTemplateEmail("templates/email_template.html"), idioma);

            template = template.replace("{baseUrl}", baseUrl);
            template = template.replace("{token}", token);
            if (destinatarioNome != null) {
                template = template.replace("{nome}", HtmlUtils.htmlEscape(destinatarioNome));
            }

            helper.setText(template, true);

            mailSender.send(message);
            System.out.println("Email enviado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // Assíncrono: o envio pelo SMTP leva vários segundos e não deve segurar a resposta da tela.
    // Se falhar, o erro fica registrado no log do servidor.
    @Async
    public void enviarEmailCompartilharAreaTrabalho(Usuario destinatario, String nomeUsuario, AreaTrabalho area, String token, PermissaoArea permissao) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(remetenteEmail, remetenteNome);
            // Roda em outra thread (@Async), sem a requisição: vale o idioma salvo no perfil de quem recebe
            Locale idioma = idiomaDoUsuario(destinatario);
            helper.setSubject(Mensagens.get(idioma, "email.compart.assunto"));
            helper.setTo(destinatario.getEmail());

            String template = traduzirTemplate(carregaTemplateEmail("templates/compartilhamentoAreaTrabalho.html"), idioma);

            String nivel = Mensagens.get(idioma, permissao == PermissaoArea.OBSERVADOR
                    ? "email.compart.permObservador"
                    : "email.compart.permEditor");

            template = template.replace("{corpo}", Mensagens.get(idioma, "email.compart.corpo",
                    HtmlUtils.htmlEscape(area.getNome()), HtmlUtils.htmlEscape(nomeUsuario), nivel));
            template = template.replace("{baseUrl}", baseUrl);
            template = template.replace("{nome}", HtmlUtils.htmlEscape(destinatario.getNome()));
            template = template.replace("{token}", token);
            template = template.replace("{areaID}", area.getId().toString());
            template = template.replace("{destID}", destinatario.getId().toString());

            helper.setText(template, true);

            mailSender.send(message);
            System.out.println("Email enviado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void enviarEmailVerificacao(String destinatarioEmail, String destinatarioNome, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(remetenteEmail, remetenteNome);
            Locale idioma = Mensagens.localeAtual(); // a pessoa acabou de se cadastrar: vale o idioma da tela
            helper.setSubject(Mensagens.get(idioma, "email.verif.assunto"));
            helper.setTo(destinatarioEmail);

            String template = traduzirTemplate(carregaTemplateEmail("templates/verificacaoEmail.html"), idioma);

            template = template.replace("{baseUrl}", baseUrl);
            template = template.replace("{token}", token);
            if (destinatarioNome != null) {
                template = template.replace("{nome}", HtmlUtils.htmlEscape(destinatarioNome));
            }

            helper.setText(template, true);

            mailSender.send(message);
            System.out.println("Email enviado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void enviarEmailLembreteTarefa(Usuario destinatario, Tarefa tarefa) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(remetenteEmail, remetenteNome);
            // Job agendado (sem requisição): vale o idioma salvo no perfil de quem recebe
            Locale idioma = idiomaDoUsuario(destinatario);
            helper.setSubject(Mensagens.get(idioma, "email.lembrete.assunto", tarefa.getTitulo()));
            helper.setTo(destinatario.getEmail());

            AreaTrabalho area = tarefa.getListaOrigem().getArea();
            String dataFimFormatada = tarefa.getDataFim()
                    .format(DateTimeFormatter.ofPattern(Mensagens.get(idioma, "email.formatoData")));

            String template = traduzirTemplate(carregaTemplateEmail("templates/lembreteTarefa.html"), idioma);
            template = template.replace("{corpo}", Mensagens.get(idioma, "email.lembrete.corpo",
                    HtmlUtils.htmlEscape(tarefa.getTitulo()), HtmlUtils.htmlEscape(area.getNome()), dataFimFormatada));
            template = template.replace("{baseUrl}", baseUrl);
            template = template.replace("{nome}", HtmlUtils.htmlEscape(destinatario.getNome()));
            template = template.replace("{areaId}", area.getId().toString());
            template = template.replace("{areaUrl}", UriUtils.encodePathSegment(area.getNome(), StandardCharsets.UTF_8));

            helper.setText(template, true);

            mailSender.send(message);
            System.out.println("Email de lembrete enviado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static final Pattern CHAVE_NO_TEMPLATE = Pattern.compile("\\{\\{([\\w.]+)\\}\\}");

    /** Idioma salvo no perfil de quem recebe o e-mail (português se ainda não escolheu). */
    static Locale idiomaDoUsuario(Usuario usuario) {
        return Idioma.deOuPadrao(usuario.getIdioma()).locale();
    }

    /** Troca os {{chave}} do HTML pelos textos do idioma e o {lang} pelo código do idioma. */
    static String traduzirTemplate(String html, Locale idioma) {
        Matcher m = CHAVE_NO_TEMPLATE.matcher(html);
        StringBuilder saida = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(saida, Matcher.quoteReplacement(Mensagens.get(idioma, m.group(1))));
        }
        m.appendTail(saida);
        return saida.toString().replace("{lang}", idioma.toLanguageTag());
    }

    public String carregaTemplateEmail(String arquivoHTML) throws IOException {
        ClassPathResource resource = new ClassPathResource(arquivoHTML);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}



