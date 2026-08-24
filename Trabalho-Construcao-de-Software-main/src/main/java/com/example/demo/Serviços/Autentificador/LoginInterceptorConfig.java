package com.example.demo.Serviços.Autentificador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LoginInterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginIntecerptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginIntecerptor)
                .excludePathPatterns(
                        "/login",
                        "/cadastro",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/favicon.ico",
                        "/recuperarSenhaEmail",
                        "/recuperar-senha",
                        "/redefinicaoSenha",
                        "/redefinirSenha"
                );

    }

}
