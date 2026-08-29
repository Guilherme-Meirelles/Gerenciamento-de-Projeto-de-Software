package com.example.demo.Serviços;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Sem isso, o Jackson tenta serializar o proxy interno do Hibernate
    // (bytecode gerado, não a entidade) sempre que um relacionamento @ManyToOne/@OneToMany
    // preguiçoso ainda não foi inicializado, e quebra com um erro obscuro de ByteBuddy.
    @Bean
    public Hibernate6Module hibernate6Module() {
        return new Hibernate6Module();
    }
}
