package com.example.demo.Serviços.Idioma;

import com.example.demo.ConsultasBD.UsuarioRepository;
import com.example.demo.Serviços.Autentificador.SessaoUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Controller
public class IdiomaController {

    /**
     * Troca de idioma recarrega a página; para o usuário não perder o que já digitou, os campos preenchidos (menos
     * senhas) ficam guardados na aba (sessionStorage) e voltam para a mesma página logo depois.
     */
    private static final String TROCAR_IDIOMA_JS = """
            window.trocarIdioma=function(l){
            try{var d={};document.querySelectorAll('form input').forEach(function(i){
            if(i.id&&i.value&&['password','hidden','file','checkbox','radio'].indexOf(i.type)<0)d[i.id]=i.value;});
            sessionStorage.setItem('todaily.idioma.campos',JSON.stringify({p:location.pathname,d:d}));}catch(e){}
            location.href='/idioma?lang='+encodeURIComponent(l)+'&voltar='+encodeURIComponent(location.pathname+location.search);};
            document.addEventListener('DOMContentLoaded',function(){try{
            var r=JSON.parse(sessionStorage.getItem('todaily.idioma.campos')||'null');if(!r)return;
            sessionStorage.removeItem('todaily.idioma.campos');if(r.p!==location.pathname)return;
            for(var id in r.d){var c=document.getElementById(id);
            if(c&&!c.readOnly&&c.value!==r.d[id]){c.value=r.d[id];c.dispatchEvent(new Event('input'));}}}catch(e){}});
            document.addEventListener('DOMContentLoaded',function(){
            function perto(i,e){var b=i.getBoundingClientRect();return e.clientX>b.right-36&&e.clientX<=b.right;}
            function ehData(e){return e.target&&e.target.type==='date';}
            document.addEventListener('mousemove',function(e){if(!ehData(e))return;
            if(perto(e.target,e)&&!e.target.disabled)e.target.title=window.t('js.data.abrir');else e.target.removeAttribute('title');});
            document.addEventListener('click',function(e){if(!ehData(e))return;var i=e.target;
            if(i.disabled||i.readOnly||!perto(i,e))return;try{i.showPicker();}catch(x){}});});
            """.replace("\n", "");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Troca o idioma: vale na sessão e no cookie e, se houver usuário logado, fica guardado no perfil dele.
     * Depois volta para a página de onde veio (só caminhos do próprio sistema).
     */
    @GetMapping("/idioma")
    public String trocar(@RequestParam("lang") String lang,
                         @RequestParam(value = "voltar", required = false) String voltar,
                         HttpServletRequest request, HttpServletResponse response) {
        Idioma idioma = Idioma.deOuPadrao(lang);
        request.getSession(true).setAttribute(IdiomaLocaleResolver.ATRIBUTO_SESSAO, idioma.codigo());
        IdiomaInterceptor.gravarCookie(response, idioma);

        String usuarioId = SessaoUtil.getUsuarioId(request);
        if (usuarioId != null) {
            usuarioRepository.findById(Long.parseLong(usuarioId)).ifPresent(usuario -> {
                usuario.setIdioma(idioma.codigo());
                usuarioRepository.save(usuario);
            });
        }
        return "redirect:" + destinoSeguro(voltar);
    }

    /** Aceita só caminhos do próprio site ("/menu?view=todas"); qualquer outra coisa vira "/". */
    static String destinoSeguro(String voltar) {
        if (voltar == null || !voltar.matches("^/(?![/\\\\])[^\\r\\n]*$")) return "/";
        return voltar;
    }

    /** Textos usados pelo JavaScript (chaves "js.*") no idioma da requisição, mais a função t(chave, ...argumentos). */
    @GetMapping("/idioma/mensagens.js")
    public ResponseEntity<String> mensagensJs() throws JsonProcessingException {
        Locale locale = Mensagens.localeAtual();
        Map<String, String> textos = Mensagens.comPrefixo(locale, "js.");
        String js = "window.I18N=" + objectMapper.writeValueAsString(textos) + ";"
                + "window.t=function(c){var s=window.I18N[c];if(s===undefined)return c;"
                + "for(var i=1;i<arguments.length;i++){s=s.split('{'+(i-1)+'}').join(arguments[i]);}return s;};"
                + TROCAR_IDIOMA_JS;
        return ResponseEntity.ok()
                .contentType(new MediaType("application", "javascript", StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .body(js);
    }
}
