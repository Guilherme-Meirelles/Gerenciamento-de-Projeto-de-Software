let areaAtualContexto = { id: null, nome: null };
let modoEdicao = false; // false = criar, true = renomear
let areaSelecionadaCompartilhar = null;
// Nível de permissão do convite (vale para o convite por e-mail e para o link)
let permissaoConviteSelecionada = 'EDITOR';
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
    configurarModalArea('briefcase', 'Dê um nome para a sua nova área.');
    document.getElementById('modalArea').style.display = 'flex';
    document.getElementById('inputNomeArea').focus();
    setTimeout(() => lucide.createIcons(), 10);
}

// Ícone e frase do topo do modal (muda entre "Nova área" e "Renomear") e botão sem nome fica desabilitado
function configurarModalArea(icone, descricao) {
    document.getElementById('iconeModalArea').innerHTML = `<i data-lucide="${icone}"></i>`;
    document.getElementById('descricaoModalArea').textContent = descricao;
    atualizarBotaoSalvarArea();
}

function atualizarBotaoSalvarArea() {
    document.querySelector('#modalArea .btn-criar').disabled =
        document.getElementById('inputNomeArea').value.trim() === '';
}

document.getElementById('inputNomeArea').addEventListener('input', atualizarBotaoSalvarArea);

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
    card.setAttribute("data-permissao", "ADMIN"); // quem cria a área é o administrador
    card.onclick = () => abrirArea(card);
    card.oncontextmenu = (e) => abrirMenuContexto(e, card);

    const h3 = document.createElement("h3");
    h3.textContent = nome;

    // Quem cria a área é o administrador
    const badge = document.createElement("span");
    badge.className = "badge-permissao admin";
    badge.textContent = "Administrador";

    card.appendChild(h3);
    card.appendChild(badge);
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
        Aviso.mostrar('Nome inválido.', 'erro');
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
        if (err.message === "NOT_FOUND") Aviso.mostrar("Área não encontrada.", 'erro');
        else if (err.message === "NOT_OWNER") Aviso.mostrar("Você não tem permissão para renomear esta área.", 'erro');
        else Aviso.mostrar("Erro ao renomear área.", 'erro');
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
    configurarModalArea('pencil', 'Escolha o novo nome da área.');
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

const ROTULOS_PERMISSAO_CONVITE = { OBSERVADOR: 'Observador', EDITOR: 'Editor' };

window.selecionarPermissaoConvite = function (permissao) {
    permissaoConviteSelecionada = permissao;

    const radio = document.querySelector(`input[name="permissaoConvite"][value="${permissao}"]`);
    if (radio) {
        radio.checked = true;
    }

    document.querySelectorAll('.opcoes-permissao-convite .opcao-permissao').forEach(opcao => {
        opcao.classList.remove('selecionada');
    });
    if (radio) {
        radio.closest('.opcao-permissao').classList.add('selecionada');
    }
}

window.abrirModalEmail = function(){
    const campo = document.getElementById('email');
    const aviso = document.getElementById('msgFeedback');

    // Some o aviso/marcação de erro assim que o usuário volta a digitar (registra uma vez só)
    if (!campo.dataset.ouvindo) {
        campo.dataset.ouvindo = '1';
        campo.addEventListener('input', () => {
            Validacao.marcar(campo, false);
            if (aviso.classList.contains('erro')) {
                Validacao.mostrarAviso(aviso, '');
            }
        });
    }

    Validacao.marcar(campo, false);
    Validacao.mostrarAviso(aviso, '');
    campo.value = '';
    document.getElementById('rotuloPermissaoEmail').textContent =
        ROTULOS_PERMISSAO_CONVITE[permissaoConviteSelecionada] || permissaoConviteSelecionada;

    document.getElementById('ModalCompartilhamentoOverlay').style.display = 'flex';
    setTimeout(() => campo.focus(), 10);
}

window.fecharModalEmail = function(){
    document.getElementById('ModalCompartilhamentoOverlay').style.display = 'none';
}

function enviarViaAjax() {
    const campo = document.getElementById('email');
    const aviso = document.getElementById('msgFeedback');
    const botao = document.querySelector('#emailForm .btn-criar');
    const email = campo.value.trim();

    function avisar(texto, tipo) {
        aviso.className = 'aviso ' + tipo;
        Validacao.mostrarAviso(aviso, texto);
    }

    // Validação no padrão das telas de login e cadastro (sem o balão nativo do navegador)
    if (!email) {
        Validacao.marcar(campo, true);
        avisar('Informe o e-mail do usuário que receberá o convite.', 'erro');
        campo.focus();
        return;
    }
    if (!Validacao.emailValido(email)) {
        Validacao.marcar(campo, true);
        avisar('Digite um e-mail válido, como nome@exemplo.com.', 'erro');
        campo.focus();
        return;
    }
    if (!areaSelecionadaCompartilhar) {
        avisar('Erro: ID da área não identificado.', 'erro');
        return;
    }

    Validacao.marcar(campo, false);
    const textoBotao = botao.textContent;
    botao.disabled = true;
    botao.textContent = 'Enviando...';

    fetch('/notificacaoArea', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            email: email,
            id_area: areaSelecionadaCompartilhar,
            permissao: permissaoConviteSelecionada
        })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                avisar(data.message, 'sucesso');
                campo.value = '';
            } else {
                avisar(data.message || 'Erro ao processar.', 'erro');
            }
        })
        .catch(error => {
            console.error('Erro:', error);
            avisar('Erro de conexão com o servidor.', 'erro');
        })
        .finally(() => {
            botao.disabled = false;
            botao.textContent = textoBotao;
        });
}

window.abrirModalCompartilhar = function () {
    document.getElementById('modalCompartilhar').style.display = 'flex';
    areaSelecionadaCompartilhar = null;
    selecionarPermissaoConvite('EDITOR'); // nível padrão a cada abertura do modal

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
        Aviso.mostrar('Você não possui permissão de administrador em nenhuma área para compartilhar.', 'alerta');
        fecharModalCompartilhar();
        return;
    }

    carregarAreasCompartilhamento(areas);

    // Nenhuma área marcada ao abrir: desabilita E-mail e Copiar Link de novo
    atualizarSelecaoArea();
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

        // Habilita os botões de compartilhamento (e tira a dica de "selecione uma área")
        ['btnEmail', 'btnLink'].forEach(id => {
            const botao = document.getElementById(id);
            botao.disabled = false;
            botao.removeAttribute('title');
        });
    } else {
        areaSelecionadaCompartilhar = null;

        // Desabilita os botões de compartilhamento e explica o motivo ao passar o mouse
        ['btnEmail', 'btnLink'].forEach(id => {
            const botao = document.getElementById(id);
            botao.disabled = true;
            botao.title = 'Selecione uma área para compartilhar';
        });
    }
}

window.gerarLinkCompartilhamento = function () {
    if (!areaSelecionadaCompartilhar) {
        Aviso.mostrar('Selecione uma área para compartilhar.', 'alerta');
        return;
    }

    const permissao = permissaoConviteSelecionada;

    fetch(`/areasTrabalho/gerar-link/${areaSelecionadaCompartilhar}?permissao=${permissao}`)
        .then(res => res.json())
        .then(data => {
            if (data.link) {
                navigator.clipboard.writeText(data.link)
                    .then(() => Aviso.mostrar('Link copiado para a área de transferência!', 'sucesso'))
                    .catch(() => Aviso.mostrar('Não foi possível copiar automaticamente. Link gerado: ' + data.link, 'alerta', 20000));
                fecharModalCompartilhar();
            } else {
                Aviso.mostrar('Erro ao gerar o link.', 'erro');
            }
        })
        .catch(err => {
            console.error(err);
            Aviso.mostrar('Erro ao gerar o link. Tente novamente.', 'erro');
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
            nome: card.dataset.nome,
            permissao: card.dataset.permissao
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
        const q = areasSelecionadasExcluir.length;
        const qtdAdmin = areasSelecionadasExcluir.filter(areaEhAdmin).length;
        if (qtdAdmin === 0) {
            btnExcluir.innerHTML = `<i data-lucide="log-out"></i> ${q === 1 ? 'Sair da área' : `Sair de ${q} áreas`}`;
        } else if (qtdAdmin === q) {
            btnExcluir.innerHTML = `<i data-lucide="trash-2"></i> ${q === 1 ? 'Excluir área' : `Excluir ${q} áreas`}`;
        } else {
            btnExcluir.innerHTML = `<i data-lucide="trash-2"></i> Excluir ou sair de ${q} áreas`;
        }
    } else {
        btnExcluir.innerHTML = '<i data-lucide="trash-2"></i> Excluir Selecionadas';
    }

    setTimeout(() => lucide.createIcons(), 10);
}

function areaEhAdmin(areaId) {
    const card = document.querySelector(`.area-card[data-id="${areaId}"]`);
    return !!card && card.dataset.permissao === 'ADMIN';
}

function nomeDaArea(areaId) {
    const card = document.querySelector(`.area-card[data-id="${areaId}"]`);
    return card ? card.dataset.nome : '';
}

// "Teste" | "Teste" e "Outra" | "A", "B" e "C"
function listarNomes(ids) {
    const nomes = ids.map(id => `"${nomeDaArea(id)}"`);
    return nomes.length > 1
        ? nomes.slice(0, -1).join(', ') + ' e ' + nomes[nomes.length - 1]
        : nomes[0];
}

// Evita que nomes com < > & " ' quebrem o HTML (ou injetem código) ao montar listas com innerHTML
function escaparHtml(texto) {
    return String(texto == null ? '' : texto).replace(/[&<>"']/g, c => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
    ));
}

window.confirmarExclusao = async function () {
    if (areasSelecionadasExcluir.length === 0) {
        Aviso.mostrar('Selecione pelo menos uma área para excluir.', 'alerta');
        return;
    }

    const idsAdmin = areasSelecionadasExcluir.filter(areaEhAdmin);
    const idsSaida = areasSelecionadasExcluir.filter(id => !areaEhAdmin(id));
    let aviso = '';
    if (idsAdmin.length > 0) {
        const um = idsAdmin.length === 1;
        aviso += `Você é administrador de ${listarNomes(idsAdmin)}: ${um ? 'ela será excluída' : 'elas serão excluídas'} para todos os membros.\n`;
    }
    if (idsSaida.length > 0) {
        const um = idsSaida.length === 1;
        aviso += `Você não é administrador de ${listarNomes(idsSaida)}: você apenas sairá ${um ? 'dela' : 'delas'}, e ${um ? 'ela continuará' : 'elas continuarão'} existindo para os outros membros.\n`;
    }

    const soSaida = idsAdmin.length === 0;
    const soExclusao = idsSaida.length === 0;
    const varias = areasSelecionadasExcluir.length > 1;

    // Nomes guardados antes de a requisição remover os cards da tela
    const resumo = [];
    if (idsAdmin.length > 0) resumo.push(idsAdmin.length === 1 ? `${listarNomes(idsAdmin)} foi excluída` : `${idsAdmin.length} áreas excluídas`);
    if (idsSaida.length > 0) resumo.push(idsSaida.length === 1 ? `você saiu de ${listarNomes(idsSaida)}` : `você saiu de ${idsSaida.length} áreas`);

    const confirmacao = await Aviso.confirmar({
        titulo: soSaida ? (varias ? 'Sair das áreas' : 'Sair da área')
            : soExclusao ? (varias ? 'Excluir áreas' : 'Excluir área')
            : 'Excluir ou sair das áreas',
        mensagem: `${aviso}\nDeseja continuar?`,
        confirmar: soSaida ? 'Sair' : soExclusao ? 'Excluir' : 'Continuar',
        perigo: idsAdmin.length > 0
    });

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
        Aviso.mostrar(`Concluído: ${resumo.join(' e ')}.`, 'sucesso');
        fecharModalExcluir();
    })
    .catch(err => {
        Aviso.mostrar(err.message, 'erro');
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

        // O que vai acontecer com a área: administrador exclui, os demais só saem
        const dica = document.createElement('span');
        const admin = area.permissao === 'ADMIN';
        dica.className = 'dica-area-excluir' + (admin ? ' exclui' : '');
        dica.textContent = admin ? 'Será excluída' : 'Você sairá';

        // Clique no item ignora label e checkbox
        item.onclick = (event) => {
            if (event.target !== checkbox && event.target !== label) {
                checkbox.checked = !checkbox.checked;
                atualizarSelecaoExclusao();
            }
        };

        item.appendChild(checkbox);
        item.appendChild(label);
        item.appendChild(dica);
        lista.appendChild(item);
    });
}

// Gerenciamento de áreas
window.abrirModalGerenciamento = function () {
    document.getElementById('modalGerenciamento').style.display = 'flex';
    areaSelecionadaGerenciar = null;
    nomeAreaGerenciar = null;

    // Começa pela escolha da área (modal compacto; os membros precisam de mais largura)
    document.querySelector('.modal-gerenciamento').classList.add('compacto');
    document.getElementById('etapaSelecaoArea').style.display = 'block';
    document.getElementById('etapaMembros').style.display = 'none';

    const listaAreas = document.querySelector('.lista-areas-gerenciar');
    listaAreas.innerHTML = ''; // limpa antes de preencher

    const areas = Array.from(document.querySelectorAll('.area-card'))
        .filter(card => card.dataset.id && card.dataset.nome)
        .map(card => ({
            id: card.dataset.id,
            nome: card.dataset.nome,
            permissao: (card.dataset.permissao || '').toLowerCase()
    }));

    if (areas.length === 0) {
        const vazio = document.createElement('p');
        vazio.className = 'descricao-modal';
        vazio.textContent = 'Você ainda não participa de nenhuma área de trabalho.';
        listaAreas.appendChild(vazio);
    }

    // Só dá pra escolher uma área, então clicar nela já abre os membros (sem "Avançar")
    areas.forEach(area => {
        const div = document.createElement('div');
        div.classList.add('area-item-gerenciar');
        div.setAttribute('role', 'button');
        div.tabIndex = 0;
        div.onclick = () => abrirMembros(area.id, area.nome);
        div.onkeydown = (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                abrirMembros(area.id, area.nome);
            }
        };

        const info = document.createElement('div');
        info.className = 'info-area';

        const nome = document.createElement('span');
        nome.className = 'nome-area';
        nome.textContent = area.nome;

        // Papel do usuário nesta área (Administrador, Editor ou Observador)
        const badge = document.createElement('span');
        badge.className = 'badge-permissao ' + area.permissao;
        badge.textContent = rotuloPermissao(area.permissao);

        const seta = document.createElement('i');
        seta.setAttribute('data-lucide', 'chevron-right');
        seta.className = 'seta-area';

        info.appendChild(nome);
        info.appendChild(badge);
        div.appendChild(info);
        div.appendChild(seta);
        listaAreas.appendChild(div);
    });

    setTimeout(() => lucide.createIcons(), 10);
}

window.fecharModalGerenciamento = function () {
    document.getElementById('modalGerenciamento').style.display = 'none';
    areaSelecionadaGerenciar = null;
    nomeAreaGerenciar = null;
}

// Nível real de permissão (admin/editor/observador) -> rótulo exibido
function rotuloPermissao(permissao) {
    return { admin: 'Administrador', editor: 'Editor', observador: 'Observador' }[permissao] || permissao;
}

// Mostra "Nome da área · N membros" abaixo do título da tela de membros
function atualizarSubtituloMembros(total) {
    const subtitulo = document.getElementById('subtituloMembros');
    if (total === null || total === undefined) {
        subtitulo.textContent = nomeAreaGerenciar;
        return;
    }
    subtitulo.textContent = `${nomeAreaGerenciar} · ${total} ${total === 1 ? 'membro' : 'membros'}`;
}

window.abrirMembros = function (areaId, nomeArea) {
    document.querySelector('.modal-gerenciamento').classList.remove('compacto');
    areaSelecionadaGerenciar = areaId;
    nomeAreaGerenciar = nomeArea;

    const listaMembros = document.getElementById('listaMembros');
    listaMembros.innerHTML = ''; // limpa lista
    atualizarSubtituloMembros(null);

    // Só o administrador altera permissões e remove membros (o servidor também exige isso)
    const souAdmin = areaEhAdmin(areaSelecionadaGerenciar);
    
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

                const ehAdmin = membro.permissao === 'admin';
                const dadosMembro = `data-id="${escaparHtml(membro.id)}" data-nome="${escaparHtml(membro.nome)}"`;

                const acoes = souAdmin ? `
                    <div class="membro-acoes">
                        <button class="btn-acao-membro" ${dadosMembro} data-permissao="${escaparHtml(membro.permissao)}"
                                onclick="abrirModalPermissao(this.dataset.id, this.dataset.nome, this.dataset.permissao)"
                                ${ehAdmin ? 'disabled title="Administradores não têm a permissão alterada por aqui"' : ''}>
                            <i data-lucide="shield"></i> Alterar Permissão
                        </button>
                        <button class="btn-acao-membro remover" ${dadosMembro} data-area="${escaparHtml(areaSelecionadaGerenciar)}"
                                onclick="removerMembro(this.dataset.id, this.dataset.nome, this.dataset.area)"
                                ${ehAdmin ? 'disabled title="O administrador não pode ser removido"' : ''}>
                            <i data-lucide="user-minus"></i> Remover
                        </button>
                    </div>` : '';

                div.innerHTML = `
                    <div class="membro-header">
                        <div class="membro-info">
                            <div class="membro-nome">${escaparHtml(membro.nome)}</div>
                            <div class="membro-email">${escaparHtml(membro.email)}</div>
                        </div>
                        <div class="membro-badges">
                            <span class="badge-permissao ${escaparHtml(membro.permissao)}" data-permissao="${escaparHtml(membro.permissao)}">
                                ${rotuloPermissao(membro.permissao)}
                            </span>
                        </div>
                    </div>
                    ${acoes}
                `;
                listaMembros.appendChild(div);
            });

            // Quantidade de membros no subtítulo
            atualizarSubtituloMembros(membros.length);
            setTimeout(() => lucide.createIcons(), 10);
        })
        .catch(err => console.error(err));

    // Troca de etapa
    document.getElementById('etapaSelecaoArea').style.display = 'none';
    document.getElementById('etapaMembros').style.display = 'block';
}

window.voltarParaSelecao = function () {
    document.querySelector('.modal-gerenciamento').classList.add('compacto');
    document.getElementById('etapaMembros').style.display = 'none';
    document.getElementById('etapaSelecaoArea').style.display = 'block';
    areaSelecionadaGerenciar = null;
    nomeAreaGerenciar = null;

    setTimeout(() => lucide.createIcons(), 10);
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

    // O modal só oferece "editar" (nível editor) e "visualizar" (nível observador)
    const idRadioAtual = { editor: 'editar', observador: 'visualizar' }[permissaoAtual];
    const radioAtual = document.getElementById('perm-' + idRadioAtual);
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
        document.querySelectorAll('#modalPermissao .opcao-permissao').forEach(opcao => {
            opcao.classList.remove('selecionada');
        });

        // Adiciona seleção visual ao clicado
        radio.closest('.opcao-permissao').classList.add('selecionada');
    }
}

window.salvarPermissao = function () {
    const permissaoSelecionada = document.querySelector('input[name="permissaoMembro"]:checked');

    if (!permissaoSelecionada) {
        Aviso.mostrar('Selecione uma permissão.', 'alerta');
        return;
    }

    const novaPermissao = permissaoSelecionada.value; // 'editar' ou 'visualizar' (radios do modal)
    const novoNivel = { editar: 'editor', visualizar: 'observador' }[novaPermissao];

    const areaId = areaSelecionadaGerenciar;
    const membro = { ...membroAtualPermissao };

    fetch(`/areasTrabalho/${encodeURIComponent(areaId)}/membros/${encodeURIComponent(membro.id)}/permissao`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ permissao: novoNivel })
    })
        .then(async resposta => {
            const dados = await resposta.json().catch(() => ({}));
            if (!resposta.ok || dados.success === false) {
                Aviso.mostrar(dados.message || 'Não foi possível alterar a permissão.', 'erro');
                return;
            }
            // Atualiza a UI
            const membroCard = document.querySelector(`.membro-item[data-membro-id="${membro.id}"]`);
            if (membroCard) {
                const badgePermissao = membroCard.querySelector('.badge-permissao');
                if (badgePermissao) {
                    // Remove classes antigas
                    badgePermissao.classList.remove('admin', 'editor', 'observador');
                    badgePermissao.setAttribute('data-permissao', novoNivel);
                    badgePermissao.classList.add(novoNivel);
                    badgePermissao.textContent = rotuloPermissao(novoNivel);
                }

                // Atualiza o botão de alterar permissão para refletir nova permissão
                const btnAlterar = membroCard.querySelector('.btn-acao-membro:not(.remover)');
                if (btnAlterar) {
                    btnAlterar.dataset.permissao = novoNivel;
                }
            }

            Aviso.mostrar(`Permissão de ${membro.nome} alterada para "${rotuloPermissao(novoNivel)}".`, 'sucesso');
            fecharModalPermissao();
        })
        .catch(() => Aviso.mostrar('Erro de conexão ao alterar a permissão.', 'erro'));
}

// Adicionamos 'async' para poder esperar a resposta do servidor
window.removerMembro = async function (membroId, membroNome, idArea) {

    // Confirmação antes de qualquer coisa
    const confirmacao = await Aviso.confirmar({
        titulo: 'Remover membro',
        mensagem: `Tem certeza que deseja remover ${membroNome} desta área?\n\nEle perderá acesso a todas as tarefas e informações.`,
        confirmar: 'Remover',
        perigo: true
    });

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

                // Atualiza a quantidade de membros no subtítulo
                atualizarSubtituloMembros(document.querySelectorAll('#listaMembros .membro-item').length);
            }, 300); // Espera a animação terminar
        }

        Aviso.mostrar(`${membroNome} foi removido da área de trabalho.`, 'sucesso');

    } catch (erro) {
        // --- 3. Tratamento de Erro ---
        console.error('Falha ao remover:', erro);
        Aviso.mostrar('Ocorreu um erro ao tentar remover o membro. Tente novamente.', 'erro');
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