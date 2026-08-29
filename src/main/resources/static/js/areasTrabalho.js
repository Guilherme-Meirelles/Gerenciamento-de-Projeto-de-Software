let areaAtualContexto = { id: null, nome: null };
let modoEdicao = false; // false = criar, true = renomear
let areaSelecionadaCompartilhar = null;
let areasSelecionadasExcluir = [];
let areaSelecionadaGerenciar = null;
let nomeAreaGerenciar = null;
let membroAtualPermissao = { id: null, nome: null, permissaoAtual: null };
let membroAtualId = null;    // ID do usuário logado
let membroCriadorId = null;  // ID do criador da área selecionada

const btnParaHoje = document.querySelector('.menu-item:nth-child(1)');
const btnAgendadas = document.querySelector('.menu-item:nth-child(2)');
const btnTodasTarefas = document.querySelector('.menu-item:nth-child(3)');

window.abrirModalNovaArea = function () {
    modoEdicao = false;
    document.getElementById('tituloModalArea').textContent = 'Nova Área de Trabalho';
    document.getElementById('inputNomeArea').value = '';
    document.getElementById('inputNomeArea').placeholder = 'Ex: Projetos Pessoais';
    document.querySelector('.btn-criar').textContent = 'Criar';
    document.getElementById('modalArea').style.display = 'flex';
    document.getElementById('inputNomeArea').focus();
    setTimeout(() => lucide.createIcons(), 10);
}

window.fecharModalArea = function () {
    document.getElementById('modalArea').style.display = 'none';
    document.getElementById('inputNomeArea').value = '';
}

window.adicionarCardArea = function (id, nome) {
    const grid = document.getElementById("areasGrid");

    const card = document.createElement("div");
    card.className = "area-card";
    card.setAttribute("data-nome", nome);
    card.setAttribute("data-id", id);
    card.onclick = () => abrirArea(card);
    card.oncontextmenu = (e) => abrirMenuContexto(e, card);

    const h3 = document.createElement("h3");
    h3.textContent = nome;

    card.appendChild(h3);
    grid.insertBefore(card, grid.lastElementChild); // insere antes do "+"

    lucide.createIcons(); // atualiza ícones se necessário
}

window.atualizarNomeNoCard = function (id, novoNome) {
    const card = document.querySelector(`.area-card[data-id="${id}"]`);
    if (card) {
        const h3 = card.querySelector("h3");
        if (h3) h3.textContent = novoNome;
        card.dataset.nome = novoNome;
    }
}

window.salvarArea = function (event) {
    event.preventDefault();

    const id = document.getElementById("areaIdEdicao").value;
    const nome = document.getElementById("inputNomeArea").value.trim();
    if (!nome) {
        alert("Nome inválido");
        return;
    }

    if (!id) {
        fetch("/areasTrabalho/criar", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `nome=${encodeURIComponent(nome)}`
        })
        .then(res => res.text())
        .then(newId => {
            if (newId === "NOT_LOGGED") {
                alert("Você precisa estar logado.");
                window.location.href = "/login";
                return;
            }
            adicionarCardArea(newId, nome);
            fecharModalArea();
            document.getElementById("areaIdEdicao").value = "";
        });
        return;
    }

    console.log(id);
    console.log(encodeURIComponent(id))

    // RENOMEAR (com id)
    fetch(`/areasTrabalho/${encodeURIComponent(id)}/renomear`, {
        method: "PUT",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `nome=${encodeURIComponent(nome)}`
    })
    .then(res => {
        if (res.status === 200) return res.text();
        if (res.status === 404) throw new Error("NOT_FOUND");
        if (res.status === 403) throw new Error("NOT_OWNER");
        throw new Error("UNKNOWN");
    })
    .then(_ => {
        atualizarNomeNoCard(id, nome);
        fecharModalArea();
        document.getElementById("areaIdEdicao").value = "";
    })
    .catch(err => {
        if (err.message === "NOT_FOUND") alert("Área não encontrada.");
        else if (err.message === "NOT_OWNER") alert("Você não tem permissão para renomear esta área.");
        else alert("Erro ao renomear área.");
        fecharModalArea();
    });
}

window.abrirArea = function (elemento) {
    const id = elemento.dataset.id;
    const name = elemento.dataset.nome
    window.location.href = `/areasTrabalho/${id}/${name}`;
}

// Menu de contexto (botão direito)
window.abrirMenuContexto = function (event, elemento) {
    event.preventDefault();
    event.stopPropagation();

    const id = elemento.dataset.id;
    const nome = elemento.dataset.nome;

    areaAtualContexto.id = id;
    areaAtualContexto.nome = nome;

    const menu = document.getElementById('menuContexto');
    menu.style.display = 'block';
    menu.style.left = event.pageX + 'px';
    menu.style.top = event.pageY + 'px';

    setTimeout(() => lucide.createIcons(), 10);
}

window.fecharMenuContexto = function () {
    document.getElementById('menuContexto').style.display = 'none';
}

window.renomearAreaContexto = function () {
    fecharMenuContexto();
    modoEdicao = true;
    
    document.getElementById('tituloModalArea').textContent = 'Renomear Área de Trabalho';
    document.getElementById('inputNomeArea').value = areaAtualContexto.nome;
    document.getElementById('inputNomeArea').placeholder = areaAtualContexto.nome;
    document.querySelector('.btn-criar').textContent = 'Salvar';
    document.getElementById('areaIdEdicao').value = areaAtualContexto.id;
    document.getElementById('modalArea').style.display = 'flex';

    setTimeout(() => {
        document.getElementById('inputNomeArea').focus();
        document.getElementById('inputNomeArea').select();
        lucide.createIcons();
    }, 10);
}

// Compartilhamento

window.carregarAreasCompartilhamento = function (areas) {
    const container = document.querySelector('.lista-areas-compartilhar');
    container.innerHTML = ''; // limpa antes de gerar

    areas.forEach(area => {
        const div = document.createElement('div');
        div.classList.add('area-item-compartilhar');
        div.onclick = () => selecionarAreaCompartilhar(area.id);

        const input = document.createElement('input');
        input.type = 'radio';
        input.name = 'areaCompartilhar';
        input.id = `area-${area.id}`;
        input.value = area.id;
        input.onchange = atualizarSelecaoArea;

        const label = document.createElement('label');
        label.htmlFor = input.id;
        label.textContent = area.nome;

        div.appendChild(input);
        div.appendChild(label);
        container.appendChild(div);
    });
}

window.abrirModalEmail = function(){
    // Atualizado para o ID sem espaço
    document.getElementById('ModalCompartilhamentoOverlay').style.display = 'flex';
}

window.fecharModalEmail = function(){
    document.getElementById('ModalCompartilhamentoOverlay').style.display = 'none';
}

function enviarViaAjax() {
    // 1. Pega os valores
    var emailInput = document.getElementById('email').value;
    var msgFeedback = document.getElementById('msgFeedback');

    // Validação básica
    if (!emailInput) {
        alert("Por favor, preencha o email.");
        return;
    }

    // Verifica a variável global do ID da Área
    if (typeof areaSelecionadaCompartilhar === 'undefined' || !areaSelecionadaCompartilhar) {
        alert("Erro: ID da área não identificado.");
        return;
    }

    // 2. Monta o objeto para enviar (JSON)
    var dadosParaEnviar = {
        email: emailInput,
        id_area: areaSelecionadaCompartilhar
    };

    // 3. Faz o POST via Fetch (Sem recarregar a página)
    fetch('/notificacaoArea', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(dadosParaEnviar)
    })
        .then(response => response.json()) // Converte a resposta do Java para objeto JS
        .then(data => {
            // 4. Trata o sucesso ou erro visualmente
            msgFeedback.style.display = 'block';
            msgFeedback.innerText = data.message;

            if (data.success) {
                msgFeedback.style.color = 'lightgreen';
                // Opcional: Limpar o campo de email
                document.getElementById('email').value = '';

                // Se quiser fechar o modal automaticamente após 2 segundos:
                // setTimeout(fecharModalEmail, 2000);
            } else {
                msgFeedback.style.color = '#ff6b6b';
            }
        })
        .catch(error => {
            console.error('Erro:', error);
            msgFeedback.style.display = 'block';
            msgFeedback.innerText = "Erro de conexão com o servidor.";
            msgFeedback.style.color = '#ff6b6b';
        });
}

window.abrirModalCompartilhar = function () {
    document.getElementById('modalCompartilhar').style.display = 'flex';
    areaSelecionadaCompartilhar = null;

    // AQUI ESTÁ A MUDANÇA PRINCIPAL
    const areas = Array.from(document.querySelectorAll('.area-card'))
        .filter(card => {
            // Verifica se tem ID, Nome e se a permissão é ADMIN
            // Ajuste 'ADMIN' para corresponder exatamente ao nome do seu Enum em Java
            return card.dataset.id &&
                card.dataset.nome &&
                card.dataset.permissao === 'ADMIN';
        })
        .map(card => ({ id: card.dataset.id, nome: card.dataset.nome }));

    // Se a lista estiver vazia, pode ser útil avisar o usuário
    if (areas.length === 0) {
        alert("Você não possui permissão de administrador em nenhuma área para compartilhar.");
        fecharModalCompartilhar();
        return;
    }

    carregarAreasCompartilhamento(areas);

    // ... (resto da função de limpar radios e botões permanece igual)
}

window.fecharModalCompartilhar = function () {
    document.getElementById('modalCompartilhar').style.display = 'none';
    areaSelecionadaCompartilhar = null;
}

window.selecionarAreaCompartilhar = function (areaId) {
    areaSelecionadaCompartilhar = areaId;

    // Marca o radio correspondente
    const radio = document.getElementById('area-' + areaId);
    if (radio) {
        radio.checked = true;
    }

    // Atualiza visual
    atualizarSelecaoArea();
}

window.atualizarSelecaoArea = function () {
    const radioSelecionado = document.querySelector('input[name="areaCompartilhar"]:checked');

    // Remove seleção visual de todos
    document.querySelectorAll('.area-item-compartilhar').forEach(item => {
        item.classList.remove('selecionada');
    });

    if (radioSelecionado) {
        areaSelecionadaCompartilhar = radioSelecionado.value;

        // Adiciona classe selecionada ao item clicado
        radioSelecionado.closest('.area-item-compartilhar').classList.add('selecionada');

        // Habilita os botões de compartilhamento
        document.getElementById('btnGoogle').disabled = false;
        document.getElementById('btnMicrosoft').disabled = false;
        document.getElementById('btnGithub').disabled = false;
        document.getElementById('btnLink').disabled = false;
    } else {
        areaSelecionadaCompartilhar = null;

        // Desabilita os botões de compartilhamento
        document.getElementById('btnGoogle').disabled = true;
        document.getElementById('btnMicrosoft').disabled = true;
        document.getElementById('btnGithub').disabled = true;
        document.getElementById('btnLink').disabled = true;
    }
}

window.compartilharVia = function (metodo) {
    if (!areaSelecionadaCompartilhar) {
        alert('Selecione uma área para compartilhar');
        return;
    }

    fetch('/areasTrabalho/compartilhar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            areaId: areaSelecionadaCompartilhar,
            metodo: metodo
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            alert(`Área compartilhada via ${metodo.toUpperCase()}!`);
            fecharModalCompartilhar();
        } else {
            alert('Erro ao compartilhar a área: ' + data.message);
        }
    })
    .catch(err => {
        console.error(err);
        alert('Erro ao compartilhar a área. Tente novamente.');
    });

}

window.gerarLinkCompartilhamento = function () {
    if (!areaSelecionadaCompartilhar) {
        alert('Selecione uma área para compartilhar');
        return;
    }

    fetch(`/areasTrabalho/gerar-link/${areaSelecionadaCompartilhar}`)
        .then(res => res.json())
        .then(data => {
            if (data.link) {
                navigator.clipboard.writeText(data.link)
                    .then(() => alert('Link copiado para a área de transferência!'))
                    .catch(() => alert('Link gerado: ' + data.link));
                fecharModalCompartilhar();
            } else {
                alert('Erro ao gerar o link.');
            }
        })
        .catch(err => {
            console.error(err);
            alert('Erro ao gerar o link. Tente novamente.');
        });
}

// Exclusão de áreas
window.abrirModalExcluir = function () {
    document.getElementById('modalExcluir').style.display = 'flex';
    areasSelecionadasExcluir = [];

    const areas = Array.from(document.querySelectorAll('.area-card'))
        .filter(card => card.dataset.id && card.dataset.nome)
        .map(card => ({
            id: card.dataset.id,
            nome: card.dataset.nome
    }));

    // Preenche dinamicamente
    popularListaAreasExcluir(areas); // 'areas' deve ser a lista atual de áreas do usuário

    // Desabilita botão de excluir
    const btnExcluir = document.getElementById('btnConfirmarExclusao');
    btnExcluir.disabled = true;

    setTimeout(() => lucide.createIcons(), 10);
}

window.fecharModalExcluir = function () {
    document.getElementById('modalExcluir').style.display = 'none';
    areasSelecionadasExcluir = [];
}

window.atualizarSelecaoExclusao = function () {
    const checkboxesSelecionados = document.querySelectorAll('input[name="areaExcluir"]:checked');
    areasSelecionadasExcluir = Array.from(checkboxesSelecionados).map(cb => cb.value);

    // Remove seleção visual de todos
    document.querySelectorAll('.area-item-excluir').forEach(item => {
        item.classList.remove('selecionada');
    });

    // Adiciona seleção visual aos marcados
    checkboxesSelecionados.forEach(checkbox => {
        checkbox.closest('.area-item-excluir').classList.add('selecionada');
    });

    // Habilita/desabilita botão de excluir
    const btnExcluir = document.getElementById('btnConfirmarExclusao');
    btnExcluir.disabled = areasSelecionadasExcluir.length === 0;

    // Atualiza texto do botão
    if (areasSelecionadasExcluir.length > 0) {
        btnExcluir.innerHTML = `<i data-lucide="trash-2"></i> Excluir ${areasSelecionadasExcluir.length} ${areasSelecionadasExcluir.length === 1 ? 'Área' : 'Áreas'}`;
    } else {
        btnExcluir.innerHTML = '<i data-lucide="trash-2"></i> Excluir Selecionadas';
    }

    setTimeout(() => lucide.createIcons(), 10);
}

window.confirmarExclusao = function () {
    if (areasSelecionadasExcluir.length === 0) {
        alert('Selecione pelo menos uma área para excluir');
        return;
    }

    const confirmacao = confirm(`Tem certeza que deseja excluir ${areasSelecionadasExcluir.length} ${areasSelecionadasExcluir.length === 1 ? 'área' : 'áreas'}?\n\nEsta ação não pode ser desfeita.`);

    if (!confirmacao) return;

    fetch('/areasTrabalho/excluir', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ids: areasSelecionadasExcluir })
    })
    .then(res => {
        if (!res.ok) throw new Error('Erro ao excluir áreas');
        return res.json();
    })
    .then(resultado => {
        // Remove os cards da tela
        areasSelecionadasExcluir.forEach(areaId => {
            const card = document.querySelector(`.area-card[data-id="${areaId}"]`);
            if (card) card.remove();
        });
        alert(`${areasSelecionadasExcluir.length} ${areasSelecionadasExcluir.length === 1 ? 'área excluída' : 'áreas excluídas'} com sucesso!`);
        fecharModalExcluir();
    })
    .catch(err => {
        alert(err.message);
    });
}

window.popularListaAreasExcluir = function (areas) {
    const lista = document.getElementById('listaAreasExcluir');
    lista.innerHTML = ''; // limpa antes

    areas.forEach(area => {
        const item = document.createElement('div');
        item.className = 'area-item-excluir';

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.name = 'areaExcluir';
        checkbox.id = 'excluir-' + area.id;
        checkbox.value = area.id;
        checkbox.onchange = atualizarSelecaoExclusao;
        checkbox.onclick = (event) => event.stopPropagation(); // evita duplo clique

        const label = document.createElement('label');
        label.setAttribute('for', 'excluir-' + area.id);
        label.textContent = area.nome;

        // Clique no item ignora label e checkbox
        item.onclick = (event) => {
            if (event.target !== checkbox && event.target !== label) {
                checkbox.checked = !checkbox.checked;
                atualizarSelecaoExclusao();
            }
        };

        item.appendChild(checkbox);
        item.appendChild(label);
        lista.appendChild(item);
    });
}

// Gerenciamento de áreas
window.abrirModalGerenciamento = function () {
    document.getElementById('modalGerenciamento').style.display = 'flex';
    areaSelecionadaGerenciar = null;
    nomeAreaGerenciar = null;

    // Mostra etapa de seleção
    document.getElementById('etapaSelecaoArea').style.display = 'block';
    document.getElementById('etapaDetalhesArea').style.display = 'none';
    document.getElementById('etapaMembros').style.display = 'none';

    // Desmarca todos os radios
    document.querySelectorAll('input[name="areaGerenciar"]').forEach(radio => {
        radio.checked = false;
    });

    // Remove seleção visual de todos
    document.querySelectorAll('.area-item-gerenciar').forEach(item => {
        item.classList.remove('selecionada');
    });

    // Desabilita botão de avançar
    document.getElementById('btnAvancarGerenciar').disabled = true;

    const listaAreas = document.querySelector('.lista-areas-gerenciar');
    listaAreas.innerHTML = ''; // limpa antes de preencher

    const areas = Array.from(document.querySelectorAll('.area-card'))
        .filter(card => card.dataset.id && card.dataset.nome)
        .map(card => ({
            id: card.dataset.id,
            nome: card.dataset.nome
    }));

    areas.forEach(area => {
        const div = document.createElement('div');
        div.classList.add('area-item-gerenciar');
        div.onclick = () => selecionarAreaGerenciar(area.id);

        div.innerHTML = `
            <div class="info-area">
                <input type="radio" name="areaGerenciar" id="gerenciar-${area.id}" value="${area.id}" onchange="atualizarSelecaoGerenciar()">
                <label for="gerenciar-${area.id}">${area.nome}</label>
                ${area.compartilhado ? `<div class="badge-compartilhado-mini"><i data-lucide="share-2"></i></div>` : ''}
            </div>
        `;
        listaAreas.appendChild(div);
    });


    setTimeout(() => lucide.createIcons(), 10);
}

window.fecharModalGerenciamento = function () {
    document.getElementById('modalGerenciamento').style.display = 'none';
    areaSelecionadaGerenciar = null;
    nomeAreaGerenciar = null;
}

window.selecionarAreaGerenciar = function (areaId) {
    const radio = document.getElementById('gerenciar-' + areaId);
    if (radio) {
        radio.checked = true;
        atualizarSelecaoGerenciar();
    }
}

window.atualizarSelecaoGerenciar = function () {
    const radioSelecionado = document.querySelector('input[name="areaGerenciar"]:checked');

    // Remove seleção visual de todos
    document.querySelectorAll('.area-item-gerenciar').forEach(item => {
        item.classList.remove('selecionada');
    });

    if (radioSelecionado) {
        areaSelecionadaGerenciar = radioSelecionado.value;
        nomeAreaGerenciar = radioSelecionado.nextElementSibling.textContent;

        // Adiciona classe selecionada ao item clicado
        radioSelecionado.closest('.area-item-gerenciar').classList.add('selecionada');

        // Habilita botão de avançar
        document.getElementById('btnAvancarGerenciar').disabled = false;
    } else {
        areaSelecionadaGerenciar = null;
        nomeAreaGerenciar = null;

        // Desabilita botão de avançar
        document.getElementById('btnAvancarGerenciar').disabled = true;
    }
}

window.avancarParaDetalhes = function () {
    if (!areaSelecionadaGerenciar) {
        alert('Selecione uma área para gerenciar');
        return;
    }

    console.log('Carregando detalhes da área:', areaSelecionadaGerenciar);
    // Aqui você faria a chamada ao backend para carregar os dados da área

    // Atualiza título
    document.getElementById('tituloAreaDetalhes').textContent = nomeAreaGerenciar;

    // Troca de etapa
    document.getElementById('etapaSelecaoArea').style.display = 'none';
    document.getElementById('etapaDetalhesArea').style.display = 'block';

    setTimeout(() => lucide.createIcons(), 10);
}

window.voltarParaSelecao = function () {
    document.getElementById('etapaDetalhesArea').style.display = 'none';
    document.getElementById('etapaSelecaoArea').style.display = 'block';

    setTimeout(() => lucide.createIcons(), 10);
}

window.abrirMembros = function () {
    if (!areaSelecionadaGerenciar) return;

    const listaMembros = document.getElementById('listaMembros');
    listaMembros.innerHTML = ''; // limpa lista
    
    fetch(`/areasTrabalho/${areaSelecionadaGerenciar}/membros`)
        .then(res => res.json())
        .then(membros => {

                if (!membros || membros.length === 0) return;

                // Define quem é o usuário logado e o criador da área
                
                membroAtualId = parseInt(/*[[${usuarioId}]]*/ '0');
                // Por agora
                membroCriadorId = membroAtualId;

                listaMembros.innerHTML = '';

            membros.forEach(membro => {
                const div = document.createElement('div');
                div.classList.add('membro-item');
                div.setAttribute('data-membro-id', membro.id);

                const btnAlterar =  membro.permissao !== 'editar'?
                    `<button class="btn-acao-membro" onclick="abrirModalPermissao('${membro.id}','${membro.nome}','${membro.permissao}')">
                        <i data-lucide="shield"></i> Alterar Permissão
                    </button>` :
                    `<button class="btn-acao-membro" disabled title="Você não pode alterar suas próprias permissões">
                        <i data-lucide="shield"></i> Alterar Permissão
                    </button>`;

                const btnRemover = membro.permissao !== 'editar'  ?
                    `<button class="btn-acao-membro remover" onclick="removerMembro('${membro.id}','${membro.nome}','${areaSelecionadaGerenciar}')">
                        <i data-lucide="user-minus"></i> Remover
                    </button>` :
                    `<button class="btn-acao-membro remover" disabled title="O criador não pode ser removido">
                        <i data-lucide="user-minus"></i> Remover
                    </button>`;

                div.innerHTML = `
                    <div class="membro-header">
                        <div class="membro-info">
                            <div class="membro-nome">${membro.nome}</div>
                            <div class="membro-email">${membro.email}</div>
                        </div>
                        <div class="membro-badges">
                            <span class="badge-permissao ${membro.permissao}" data-permissao="${membro.permissao}">
                                ${membro.permissao === 'editar' ? 'Administrador' : 'Editor'}
                            </span>
                        </div>
                    </div>
                    <div class="membro-acoes">
                        ${btnAlterar}
                        ${btnRemover}
                    </div>
                `;
                listaMembros.appendChild(div);
            });

            // Atualiza contador de membros
            document.getElementById('contadorMembros').textContent = membros.length;
            setTimeout(() => lucide.createIcons(), 10);
        })
        .catch(err => console.error(err));

    // Troca de etapa
    document.getElementById('etapaDetalhesArea').style.display = 'none';
    document.getElementById('etapaMembros').style.display = 'block';
}

window.voltarParaDetalhes = function () {
    document.getElementById('etapaMembros').style.display = 'none';
    document.getElementById('etapaDetalhesArea').style.display = 'block';

    setTimeout(() => lucide.createIcons(), 10);
}

window.abrirSolicitacoes = function () {
    console.log('Carregando solicitações da área:', areaSelecionadaGerenciar);
    // Aqui você implementaria a tela de solicitações
    alert('Funcionalidade de Solicitações em desenvolvimento');
}

// Gerenciamento de permissões de membros
window.abrirModalPermissao = function (membroId, membroNome, permissaoAtual) {
    membroAtualPermissao = { id: membroId, nome: membroNome, permissaoAtual: permissaoAtual };

    document.getElementById('tituloModalPermissao').textContent = `Alterar Permissão`;
    document.getElementById('descModalPermissao').textContent = `Selecione o nível de permissão para ${membroNome}`;

    // Marca a permissão atual
    document.querySelectorAll('input[name="permissaoMembro"]').forEach(radio => {
        radio.checked = false;
    });

    const radioAtual = document.getElementById('perm-' + permissaoAtual);
    if (radioAtual) {
        radioAtual.checked = true;
        radioAtual.closest('.opcao-permissao').classList.add('selecionada');
    }

    document.getElementById('modalPermissao').style.display = 'flex';
    setTimeout(() => lucide.createIcons(), 10);
}

window.fecharModalPermissao = function () {
    document.getElementById('modalPermissao').style.display = 'none';
    membroAtualPermissao = { id: null, nome: null, permissaoAtual: null };
}

window.selecionarPermissao = function (permissao) {
    const radio = document.getElementById('perm-' + permissao);
    if (radio) {
        radio.checked = true;

        // Remove seleção visual de todos
        document.querySelectorAll('.opcao-permissao').forEach(opcao => {
            opcao.classList.remove('selecionada');
        });

        // Adiciona seleção visual ao clicado
        radio.closest('.opcao-permissao').classList.add('selecionada');
    }
}

window.salvarPermissao = function () {
    const permissaoSelecionada = document.querySelector('input[name="permissaoMembro"]:checked');

    if (!permissaoSelecionada) {
        alert('Selecione uma permissão');
        return;
    }

    const novaPermissao = permissaoSelecionada.value;

    console.log('Alterando permissão do membro:', membroAtualPermissao.id, 'para:', novaPermissao);
    // Aqui você implementa a chamada ao backend

    // Atualiza a UI
    const membroCard = document.querySelector(`.membro-item[data-membro-id="${membroAtualPermissao.id}"]`);
    if (membroCard) {
        const badgePermissao = membroCard.querySelector('.badge-permissao');
        if (badgePermissao) {
            // Remove classes antigas
            badgePermissao.classList.remove('criador', 'editar');
            badgePermissao.setAttribute('data-permissao', novaPermissao);

            // Atualiza texto e classe
            if (novaPermissao === 'editar') {
                badgePermissao.textContent = 'Administrador';
                badgePermissao.classList.add('editar');
            } else if (novaPermissao === 'visualizar') {
                badgePermissao.textContent = 'Editor';
            }
        }

        // Atualiza o botão de alterar permissão para refletir nova permissão
        const btnAlterar = membroCard.querySelector('.btn-acao-membro:not(.remover)');
        if (btnAlterar) {
            btnAlterar.setAttribute('onclick', `abrirModalPermissao('${membroAtualPermissao.id}', '${membroAtualPermissao.nome}', '${novaPermissao}')`);
        }
    }

    alert(`Permissão de ${membroAtualPermissao.nome} alterada para "${novaPermissao === 'editar' ? 'Administrador' : 'Editor'}"`);
    fecharModalPermissao();
}

// Adicionamos 'async' para poder esperar a resposta do servidor
window.removerMembro = async function (membroId, membroNome, idArea) {

    // Confirmação antes de qualquer coisa
    const confirmacao = confirm(`Tem certeza que deseja remover ${membroNome} desta área?\n\nEle perderá acesso a todas as tarefas e informações.`);

    if (!confirmacao) return; // Se cancelar, para aqui.

    try {
        // --- 1. Chamada ao Backend ---
        // Substitua '/api/remover-membro' pela URL real do seu backend
        const resposta = await fetch('/remover-membro', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                // Se precisar de token, adicione aqui: 'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                areaId: idArea,    // Enviando o ID da área
                usuarioId: membroId // Enviando o ID do membro
            })
        });

        // Verifica se a requisição deu erro (ex: 404, 500)
        if (!resposta.ok) {
            throw new Error('Erro ao processar a remoção no servidor.');
        }

        // --- 2. Sucesso: Atualiza a Interface ---
        console.log('Membro removido com sucesso:', membroId);

        const membroCard = document.querySelector(`.membro-item[data-membro-id="${membroId}"]`);

        if (membroCard) {
            // Animação de saída
            membroCard.style.transition = 'all 0.3s ease';
            membroCard.style.opacity = '0';
            membroCard.style.transform = 'translateX(-20px)';

            setTimeout(() => {
                membroCard.remove();

                // Atualiza contador de membros
                const contador = document.getElementById('contadorMembros');
                if (contador) {
                    const numeroAtual = parseInt(contador.textContent) || 0;
                    contador.textContent = Math.max(0, numeroAtual - 1);
                }
            }, 300); // Espera a animação terminar
        }

        alert(`${membroNome} foi removido da área de trabalho.`);

    } catch (erro) {
        // --- 3. Tratamento de Erro ---
        console.error('Falha ao remover:', erro);
        alert('Ocorreu um erro ao tentar remover o membro. Tente novamente. ' + erro);
    }
};

// Fecha o menu de contexto ao clicar em qualquer lugar
document.addEventListener('click', function(event) {
    const menu = document.getElementById('menuContexto');
    if (menu && !menu.contains(event.target)) {
        fecharMenuContexto();
    }
});

window.navegarPara = function (e, url) {

    // Redireciona
    window.location.href = url;
}

btnParaHoje.addEventListener('click', function() {
    navegarPara("", "/menu")
});

btnAgendadas.addEventListener('click', function() {
    navegarPara("", "/menu")
});

btnTodasTarefas.addEventListener('click', function() {
    navegarPara("", "/menu")
});

// Inicializa os ícones do Lucide ao carregar a página
document.addEventListener('DOMContentLoaded', function() {
    lucide.createIcons();
});