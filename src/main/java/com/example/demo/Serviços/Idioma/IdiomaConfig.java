package com.example.demo.Serviços.Idioma;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class IdiomaConfig implements WebMvcConfigurer {

    @Autowired
    private IdiomaInterceptor idiomaInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idiomaInterceptor)
                .excludePathPatterns("/css/**", "/js/**", "/img/**", "/favicon.ico");
    }
}
