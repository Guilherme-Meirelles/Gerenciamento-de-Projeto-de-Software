# Bugs corrigidos — sessão de revisão do ToDaily

Registro dos bugs encontrados e corrigidos numa sessão de revisão geral do projeto (branch `matheusfoltran`), a partir de: verificar se o banco antigo ainda funcionava após trocar de repositório, um raio-x geral do código, e correção dos bugs de segurança encontrados.

## Verificação de e-mail (não existia)

- Cadastro salvava o usuário direto e liberava login sem nenhuma confirmação de e-mail.
- Causa raiz do "e-mail não sendo enviado certo": todos os e-mails eram enviados com `From: noreply@todaily.com`, mas a conta configurada é um Gmail real — o Gmail rejeita/reescreve o remetente quando ele não é a própria conta autenticada. Corrigido para usar o e-mail real da conta (`spring.mail.username`).
- Implementado: campo `emailVerificado` em `Usuario`, geração de token (reaproveitando a entidade `Token` já usada na recuperação de senha), envio de e-mail de confirmação, endpoint `GET /verificarEmail`, bloqueio de login até confirmar, bloqueio de cadastro com e-mail duplicado.
- Descoberto no caminho: `spring.jpa.hibernate.ddl-auto` nunca tinha sido configurado, então o `generateDdl(true)` do `ConfigBancoDeDados` não tinha efeito nenhum e o schema nunca era atualizado sozinho. Adicionado `spring.jpa.hibernate.ddl-auto=update`.

## Reenvio de confirmação de e-mail (token expirava sem chance de recuperar)

- Token de confirmação expira em 30 minutos e não existia nenhuma forma de gerar outro. Quem perdesse o prazo (ex: abriu o link de confirmação num dispositivo diferente do que rodava o servidor — o link usa `app.base-url`, que em dev aponta pro `localhost` da máquina que enviou o e-mail, então só abre nela mesma) ficava com a conta travada pra sempre: `/cadastro` bloqueia e-mail duplicado, `/login` bloqueia sem `emailVerificado`.
- Implementado `POST /reenviarVerificacao` em `LoginCadastroControle`, espelhando o padrão já usado em `RecuperarSenhaController` (gera token novo, reenvia o mesmo template de e-mail de confirmação).
- UX: em vez de uma página pedindo o e-mail de novo, o botão "Reenviar e-mail de confirmação" só aparece na tela de login depois de uma tentativa de login que falha por e-mail não confirmado, com o e-mail já preenchido num campo oculto do form — não precisa digitar de novo.
- Bug introduzido e corrigido na mesma sessão: a rota nova não estava na lista de exclusão do `LoginInterceptor`, então batia um redirect 302 silencioso pro `/login` em vez de reenviar — página "quebrada" sem erro nenhum no console.

## Segurança

- **Senha em texto puro** — salva e comparada sem hash (`findByEmailAndSenha`), e o form de edição de perfil chegava a devolver a senha atual em texto no campo. Corrigido com BCrypt (`spring-security-crypto`) no cadastro, login, edição de perfil, redefinição de senha e exclusão de conta. Contas antigas com senha em texto puro precisam passar por "esqueci minha senha" pra gerar um hash novo (não dá pra migrar sem saber a senha original).
- **Cookie de sessão falsificável** — o login setava um cookie `usuarioId` puro, sem assinatura nem `HttpOnly`; bastava editar o cookie no navegador (`usuarioId=1`) pra virar outro usuário, sem senha nenhuma. Testado e confirmado antes da correção. Substituído por `HttpSession` (cookie `JSESSIONID`, assinado pelo servlet container, `HttpOnly`).
- **IDOR em `/listas/**` e `/tarefas/**`** — qualquer usuário autenticado conseguia ler/editar/apagar tarefas e listas de áreas de trabalho às quais não pertencia, só sabendo o ID (não tinha checagem de participação, ao contrário do `AreaTrabalhoController`). Testado com dois usuários reais: dono acessa normal, intruso toma 403 em tudo (leitura, edição, criação, exclusão).
- **Vazamento do hash da senha no JSON** — endpoints que devolviam a entidade `Usuario` (aninhada dentro de `Lista`/`AreaTrabalho`) mandavam o hash BCrypt pro navegador. Corrigido com `@JsonIgnore` no campo `senha`.

## Bugs encontrados durante os testes (efeito colateral de testar de verdade)

- **Serialização circular travando a criação de lista.** `Lista → area → dono(Usuario) → areasCriadas → [AreaTrabalho...]` (e outros ciclos parecidos entre `AreaTrabalho`/`Usuario`/`Tarefa`) faziam o Jackson tentar serializar infinitamente até estourar o limite de profundidade (erro real, reproduzido: criar uma lista pela UI já vinha quebrando antes dessa sessão). Corrigido com `@JsonIgnore` nas coleções "de volta" das entidades.
- **Proxy do Hibernate quebrando `GET /listas/{id}`** — relacionamento `@ManyToOne(LAZY)` sem suporte do Jackson pro proxy do Hibernate causava 500. Corrigido adicionando `jackson-datatype-hibernate6` e registrando o `Hibernate6Module`.

## Autorização (achados numa varredura geral pós-sessão de e-mail)

- **Troca de email sem checar duplicata, e sem invalidar verificação** (`edicaoUsuario.java`) — dava pra editar o perfil pro e-mail de outro usuário sem checagem nenhuma (ao contrário do `/cadastro`), e `emailVerificado` continuava `true` mesmo com um e-mail que a pessoa não é dona. Duas contas com o mesmo e-mail quebravam o login das duas (`findByEmail` estoura exceção ao achar mais de uma linha). Corrigido: bloqueia e-mail já em uso, e zera `emailVerificado` quando o e-mail muda (o fluxo de reenvio de confirmação já existente cuida do resto).
- **Convite de área de trabalho não era amarrado ao destinatário nem à área** (`CompartilharAreadeTrabalho.java`) — `GET /compartilhamentoArea` validava só se o token existia/não expirou, mas `token`, `destID` e `areaID` vinham como três parâmetros independentes na URL sem relação entre si, e a entidade `Token` nem guardava a área. Dava pra pegar qualquer convite legítimo (ainda não usado) e trocar o `areaID` na URL pra entrar como EDITOR numa área totalmente diferente. Corrigido: `Token` ganhou o campo `areaId`, gravado na criação do convite; o resgate agora confere se `areaID`/e-mail do destinatário batem com o que foi gravado no token. Também: `POST /notificacaoArea` deixava qualquer usuário logado convidar gente pra áreas das quais ele nem participava — agora confere participação antes de gerar o convite.
- **IDOR na página do workspace** (`AreaTrabalhoController.areasTrabalhoId`, `GET /areasTrabalho/{id}/{name}`) — não checava participação, ao contrário dos endpoints irmãos (`/membros`, `/listas`, `/tarefas`), então qualquer usuário logado via o menu de qualquer área só incrementando o ID na URL. Corrigido com a mesma checagem de participação que os outros já tinham.
- **`/remover-membro` sem autorização nenhuma** (`AreaTrabalhoController.removerMembro`) — nem pedia sessão, então qualquer usuário logado removia qualquer membro de qualquer área, sabendo/adivinhando os IDs. Corrigido: exige login e que quem está chamando seja ADMIN da área.

## Bugs pequenos

- `==` em vez de `.equals()` comparando `Long` em `excluirAreas` — funcionava por acidente com IDs pequenos (cache do Java), quebrava (NPE) com IDs maiores. Corrigido, mais um guard contra NPE quando o usuário não participa da área.
- Rótulo de permissão errado: usuário com permissão `EDITOR` aparecia na lista de membros como "visualizar" em vez de "editar".
- Campo de data de nascimento não vinha preenchido ao abrir a tela de editar perfil (usuário tinha que redigitar toda vez; se ficasse vazio, o backend estourava exceção). Corrigido, e a validação de senha na edição de perfil passou a ser opcional (antes, deixar em branco quebrava a troca de outros dados, porque a senha vinha pré-preenchida com o hash antigo).
- Mensagem de erro do cadastro nunca aparecia: `LoginCadastroControle.cadastroUsuario` fazia `return "redirect:/cadastro"` quando `result.hasErrors()`, e um redirect descarta o `model.addAttribute("mensagem", ...)`. Trocado por render direto da view.
- `AreaCompartilhamentoRepository.findAreaCompartilhamentoById` tinha assinatura errada — retornava `AreaCompartilhamentoRepository` (o próprio repositório) em vez de `AreaCompartilhamento`.
- `toggleTarefaConcluida` (`menu.js`) criava variáveis globais implícitas (`titulo`, `listaId`, `concluida`, `responsavelId` atribuídas sem `let/const`).
- URL base dos e-mails hardcoded (`http://localhost:8080` fixo em `email_template.html`, `verificacaoEmail.html`, `compartilhamentoAreaTrabalho.html`) — extraída para a propriedade `app.base-url` em `application.properties`, injetada no `EmailService`.

## Estrutural (não é bug, mas mexeu bastante)

- O repositório tinha o projeto inteiro aninhado dentro de `Trabalho-Construcao-de-Software-main/`, com o `.git` na pasta pai (`Gerenciamento-de-Projeto-de-Software`). Movido tudo pra raiz do repo com `git mv` (preserva histórico).
- `.idea/` estava sendo versionada e enviada pro remoto, porque o `.gitignore` só vivia dentro da pasta aninhada antes e nunca cobria a `.idea/` da raiz. Destrackeada.
- README reescrito com descrição do projeto, stack, setup de banco e de e-mail, e como rodar.

## Ainda pendente

Ver `PENDENCIAS.md` na raiz do repositório do projeto — inclui funcionalidades incompletas (checklists e categorias de tarefa nunca foram persistidos de verdade, "compartilhar via link" não faz nada) e a senha de app do Gmail exposta no histórico do git (aguardando conversa com o Gui, é a conta dele). Os bugs pequenos que faltavam já foram todos corrigidos.
