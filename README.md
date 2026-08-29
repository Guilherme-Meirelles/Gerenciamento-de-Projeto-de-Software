# ToDaily

Protótipo de um gerenciador de tarefas colaborativo, desenvolvido como trabalho da disciplina de Construção de Software. Permite cadastro/login de usuários com confirmação por e-mail, criação de áreas de trabalho compartilháveis, listas e tarefas, além de recuperação de senha por e-mail.

## Stack

- Java 21
- Spring Boot 3.5.6 (Web, Data JPA, Mail, Thymeleaf, Actuator)
- MySQL / MariaDB
- Thymeleaf (server-side rendering) + JS puro no front-end
- BCrypt (spring-security-crypto) para hash de senha
- Maven (via wrapper `mvnw`, não precisa ter o Maven instalado)

## Pré-requisitos

- JDK 21
- Um servidor MySQL ou MariaDB rodando localmente na porta padrão (`3306`)

## Configuração do banco de dados

A conexão com o banco está definida em
`src/main/java/com/example/demo/ConfigBancoDeDados.java` (não usa `application.properties` para isso).

Crie o banco e o usuário esperados por essa configuração:

```sql
CREATE DATABASE todaily_db;
CREATE USER 'ToDaily_user'@'localhost' IDENTIFIED BY 'ToDaily_123';
GRANT ALL PRIVILEGES ON todaily_db.* TO 'ToDaily_user'@'localhost';
FLUSH PRIVILEGES;
```

O schema (tabelas) é criado/atualizado automaticamente na primeira execução, graças a `spring.jpa.hibernate.ddl-auto=update` em `application.properties` — não é preciso rodar nenhum script de criação de tabelas à parte.

Se preferir usar outro host, porta, usuário, senha ou nome de banco, edite diretamente o `dataSource()` em `ConfigBancoDeDados.java`.

## Configuração de e-mail

O envio de e-mails (confirmação de cadastro, recuperação de senha, convites de área de trabalho) usa SMTP do Gmail, configurado em `src/main/resources/application.properties`:

```properties
spring.mail.username=<seu-email>@gmail.com
spring.mail.password=<senha-de-app-do-gmail>
```

> **Atenção:** use uma [senha de app](https://myaccount.google.com/apppasswords) do Gmail, nunca a senha normal da conta. O Gmail rejeita/reescreve o remetente quando ele não é a própria conta autenticada, então o `From` dos e-mails enviados é sempre esse mesmo endereço.

## Rodando o projeto

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

## Estrutura do projeto

```
src/main/java/com/example/demo/
├── Controles/        # Controllers MVC/REST
├── ConsultasBD/       # Repositórios Spring Data JPA
├── Entidades/          # Entidades JPA
├── Serviços/            # Regras de negócio, autenticação e envio de e-mail
└── ToDailyApplication.java
src/main/resources/
├── templates/         # Views Thymeleaf
├── static/             # CSS e JS
└── application.properties
```

## Branches

Cada integrante do grupo trabalha na própria branch (`matheusfoltran`, `alexandre`, `enzo`, `guijose`, `matheuscampaner`); `main` recebe o que já foi integrado.
