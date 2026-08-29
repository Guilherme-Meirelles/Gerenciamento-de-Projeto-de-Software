# Pendências do ToDaily

Lista do que ainda falta corrigir/implementar.

## Funcionalidades incompletas ou falsas

- [ ] **Checklists de tarefa não são salvos.** Em `src/main/resources/static/js/menu.js`, o array `checklists` só existe na memória do navegador (`id: Date.now()`) — some ao dar F5. No backend, `TarefaService.criarTarefa`/`editarTarefa` têm a linha `//tarefa.setChecklistId(checklistId);` comentada, e a entidade `Tarefa` nem tem esse campo. Precisa: adicionar `checklistId`/relação em `Tarefa`, endpoints de CRUD de checklist, e persistir de verdade.
- [ ] **Categorias de tarefa nunca foram implementadas.** Existe a entidade `Categoria` e a tabela `categorias_tarefa`, mas não há `CategoriaController`, `CategoriaRepository` nem nada na UI que crie ou atribua uma categoria a uma tarefa.
- [ ] **"Compartilhar via [link/outro método]" não faz nada.** `window.compartilharVia` (`areasTrabalho.js`) chama `POST /areasTrabalho/compartilhar`; o controller (`AreaTrabalhoController.compartilharArea`) tem a lógica de criar a participação toda comentada e só retorna `success: true` fake. (O compartilhamento por e-mail, via `/notificacaoArea`, esse funciona de verdade.)
- [ ] **"Gerar link" de compartilhamento é inútil.** `GET /areasTrabalho/gerar-link/{id}` gera uma URL tipo `https://todaily.app/shared/{token}` — domínio que não existe, e nenhum endpoint trata esse token depois.

## Segurança

- [ ] **Senha de app do Gmail exposta no histórico do git** (`src/main/resources/application.properties`, `spring.mail.password`). Sugestão: Revogar a senha e criar um .env
