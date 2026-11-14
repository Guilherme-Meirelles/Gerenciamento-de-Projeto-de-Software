package com.example.demo;

import org.springframework.context.annotation.*;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;

@Configuration
public class ConfigBancoDeDados {

    @Bean
    public DataSource dataSource(){

        // Com pool de conexões: a conexão com o MySQL é aberta uma vez e reaproveitada. Antes (DriverManagerDataSource)
        // cada consulta abria uma conexão nova, e de vez em quando o MySQL demorava segundos só para aceitá-la.
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/todaily_db?useTimezone=true&serverTimezone=UTC");
        dataSource.setUsername("ToDaily_user");
        dataSource.setPassword("ToDaily_123");
        dataSource.setPoolName("ToDailyPool");
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setKeepaliveTime(300_000);   // testa conexões paradas a cada 5 min
        dataSource.setMaxLifetime(1_500_000);   // renova conexões a cada 25 min (antes do limite do MySQL)
        return  dataSource;
    }
    @Bean
    public JpaVendorAdapter jpaVendorAdapter(){

        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setDatabase(Database.MYSQL);
        adapter.setShowSql(true);
        adapter.setGenerateDdl(true);
        adapter.setDatabasePlatform("org.hibernate.dialect.MySQLDialect");
        adapter.setPrepareConnection(true);
        return adapter;
    }
}