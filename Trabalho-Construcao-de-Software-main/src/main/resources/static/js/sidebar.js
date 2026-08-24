// Inicializa ícones Lucide
lucide.createIcons();

// Variável para guardar qual lista será removida
let listaParaRemover = null;
let listaSelecionada = null;  // lista escolhida para editar
let listaParaRemoverId = null; // id da lista para remover

/**
 * Abre um modal pelo ID
 */
window.abrirModalPerfil = function (nomeModal) {
    document.getElementById(nomeModal).style.display = 'flex';
    setTimeout(() => {
        const modal = document.getElementById(nomeModal);
        if (modal.querySelectorAll('[data-lucide]').length > 0) {
            lucide.createIcons();
        }
    }, 10);
}

function fazerLogout() {
    // Fecha modal se estiver aberto
    if (typeof fecharModalConfig === "function") {
        fecharModalConfig();
    }

    // Redireciona para a rota de logout
    window.location.href = "/logout";
}

/**
 * Fecha um modal pelo ID
 */
window.fecharModalPerfil = function (nomeModal) {
    const modal = document.getElementById(nomeModal);
    modal.style.display = 'none';

    // Remove mensagem de erro ao fechar o modal
    const erroMsg = modal.querySelector('.mensagem-erro');
    if (erroMsg) {
        erroMsg.textContent = '';
    }

    const nome = document.getElementById("nome-lista");
    if(nome){
        nome.value = "";
    }

    const desc = document.getElementById("descricao-lista");
    if(desc){
        desc.value = "";
    }

    // Limpa o campo de senha, se existir
    const inputSenha = modal.querySelector('input[type="password"]');
    if (inputSenha) {
        inputSenha.value = '';
    }
}

/**
 * Abre o modal de configurações
 */
window.abrirModalConfig = function () {
    document.getElementById('modalConfig').style.display = 'flex';
    setTimeout(() => {
        lucide.createIcons();
    }, 10);
}

/**
 * Fecha o modal de configurações
 */
window.fecharModalConfig = function () {
    document.getElementById('modalConfig').style.display = 'none';
}

/**
 * Mostra o menu de contexto (clique direito)
 */
window.mostrarMenuContexto = function (event, menuId, elemento) {
    event.preventDefault();

    // Guarda o botão da lista clicada
    listaParaRemover = elemento;

    // Esconde todos os menus de contexto
    document.querySelectorAll('.context-menu').forEach(menu => menu.style.display = 'none');

    // Mostra o menu específico na posição do mouse
    const menu = document.getElementById(menuId);
    menu.style.display = 'block';
    menu.style.left = (event.pageX + 5) + 'px';
    menu.style.top = (event.pageY + 5) + 'px';
}

/**
 * Fecha menus de contexto ao clicar fora
 */
window.addEventListener('click', (e) => {
    if (!e.target.closest('.context-menu-item')) {
        document.querySelectorAll('.context-menu').forEach(menu => {
            menu.style.display = 'none';
        });
    }
});

/**
 * Fecha menus de contexto ao rolar a página
 */
window.addEventListener('scroll', () => {
    document.querySelectorAll('.context-menu').forEach(menu => {
        menu.style.display = 'none';
    });
}, true);

/**
 * Fecha modais com ESC
 */
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        fecharModalConfig();
        fecharModalPerfil('modalPerfil');
        fecharModalPerfil('modalVerificacao');
        fecharModalPerfil('modalAddLista');
        fecharModalPerfil('modalEditLista');
        fecharModalPerfil('modalRemoveLista');
    }
});

/**
 * Navega para qualquer página e marca como ativo
 */
window.navegarPara = function (elementoClicado, url) {

    // Redireciona
    window.location.href = url;
}

/**
 * Atualiza o título da página principal quando já está nela
 */
window.atualizarTituloMenu = function (elementoClicado) {
    const novoTitulo = elementoClicado.innerText.trim();
    const tituloH2 = document.getElementById('titulo-principal');
    const subtituloSpan = document.getElementById('subtitulo-principal');

    // Atualiza o título
    if (tituloH2) {
        tituloH2.innerText = novoTitulo;
    }

    // Atualiza o subtítulo (só mostra data para "Para Hoje")
    if (subtituloSpan) {
        if (novoTitulo === "Para Hoje") {
            const hoje = new Date();
            const dia = String(hoje.getDate()).padStart(2, "0");
            const mes = String(hoje.getMonth() + 1).padStart(2, "0");
            const ano = String(hoje.getFullYear()).slice(-2); // pega só os dois últimos dígitos
            const dataFormatada = `${dia}/${mes}/${ano}`;
            subtituloSpan.innerText = `- ${dataFormatada}`;
        } else {
            subtituloSpan.innerText = "";
        }
    }
}

/**
 * Marca o item correto da sidebar como ativo ao carregar a página
 */
window.marcarItemAtivoSidebar = function () {
    const path = window.location.pathname;

    // Remove active de todos os itens
    document.querySelectorAll('.menu-item, .lista').forEach(btn => {
        btn.classList.remove('active');
    });

    // Marca o item correto baseado na URL
    if (path === '/menu' || path === '/') {
        const btnParaHoje = document.querySelector('.menu-item');
        if (btnParaHoje) btnParaHoje.classList.add('active');
    } else if (path === '/agendadas') {
        const btnAgendadas = document.querySelectorAll('.menu-item')[1];
        if (btnAgendadas) btnAgendadas.classList.add('active');
    } else if (path === '/todasTarefas') {
        const btnTodasTarefas = document.querySelectorAll('.menu-item')[2];
        if (btnTodasTarefas) btnTodasTarefas.classList.add('active');
    } else if (path.startsWith('/areasTrabalho')) {
        const btnAreasTrabalho = document.querySelectorAll('.menu-item')[3];
        if (btnAreasTrabalho) btnAreasTrabalho.classList.add('active');
    }
}

// Executa ao carregar a página
marcarItemAtivoSidebar();

/**
 * Executa a remoção da lista
 */
window.executarRemocaoLista = function () {
    // Remove o item da lista (se ele estiver guardado)
    if (listaParaRemover) {
        listaParaRemover.remove();
        listaParaRemover = null;
    }

    // Fecha o modal de confirmação
    fecharModalPerfil('modalRemoveLista');

    // Simula um clique em "Para Hoje" para resetar a tela
    const btnParaHoje = document.querySelector('.menu-item');
    if (btnParaHoje) {
        btnParaHoje.click();
    }
}

window.abrirModalAcao = function (tipo) {
    const modal = document.getElementById('modalVerificacao');
    const mensagem = document.getElementById('mensagemVerificacao');
    const form = document.getElementById('formConfirmacao');

    if (tipo === 'editar') {
        mensagem.textContent = 'Deseja realmente editar sua conta?';
        form.action = '/editarConta';
    } else if (tipo === 'remover') {
        mensagem.textContent = 'Deseja realmente remover sua conta?';
        form.action = '/removerConta';
    }

    // Limpa o campo de senha e mostra o modal
    document.getElementById('senhaConfirmacao').value = '';
    modal.style.display = 'flex';
}

window.criarLista = async function (areaId, nome) {
    const response = await fetch("/listas", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ areaId, nome })
    });

    const lista = await response.json();
    adicionarListaNaTela(lista);
}

window.adicionarListaNaTela = function (lista) {

    const container = document.getElementById("listasContainer");
    const btn = document.createElement("button");
    btn.className = "lista";
    btn.dataset.id = lista.id;
    btn.dataset.desc = lista.descricao
    btn.oncontextmenu = (e) => mostrarMenuContexto(e, "menuContextoLista", btn);

    btn.innerHTML = `
        <i data-lucide="list"></i>
        <span>${lista.nome}</span>
    `;

    container.appendChild(btn);
    lucide.createIcons(); // recarrega ícones
}

window.editarLista = async function (id, novoNome, descricao) {
    const response = await fetch(`/listas/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ nome: novoNome, desc: descricao})
    });

    const atualizada = await response.json();

    const botao = document.querySelector(`button.lista[data-id='${id}']`);
    
    botao.querySelector("span").textContent = atualizada.nome;
    botao.dataset.desc = descricao
    
}

window.deletarLista = async function (id) {
    await fetch(`/listas/${id}`, { method: "DELETE" });

    const btn = document.querySelector(`button.lista[data-id='${id}']`);
    btn.remove();
}

window.abrirModalEditarLista = function () {
    if (!listaParaRemover) return;

    listaSelecionada = listaParaRemover;
    const nomeAtual = listaSelecionada.querySelector("span").textContent;
    const desc = listaSelecionada.dataset.desc;

    document.getElementById("nome-lista-edit").value = nomeAtual;
    document.getElementById("descricao-lista-edit").value = desc;

    document.querySelectorAll('.context-menu').forEach(menu => menu.style.display = 'none');

    abrirModalPerfil("modalEditLista");
};

window.abrirModalRemoverLista = function () {
    if (!listaParaRemover) return;

    listaParaRemoverId = listaParaRemover.dataset.id;

    document.querySelectorAll('.context-menu').forEach(menu => menu.style.display = 'none');

    abrirModalPerfil("modalRemoveLista");
};

window.confirmarCriacaoLista = async function () {
    const nome = document.getElementById("nome-lista").value.trim();
    document.getElementById("nome-lista").value = "";
    const descricao = document.getElementById("descricao-lista").value.trim();
    const areaId = window.areaAtualId; // ID da área carregada na página
    
    if (!nome) {
        alert("O nome da lista é obrigatório!");
        return;
    }

    const response = await fetch("/listas", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ areaId, nome, descricao })
    });

    const novaLista = await response.json();

    adicionarListaNaTela(novaLista);

    fecharModalPerfil("modalAddLista");
};

window.confirmarEdicaoLista = async function () {
    if (!listaSelecionada) return;

    const id = listaSelecionada.dataset.id;
    const nome = document.getElementById("nome-lista-edit").value.trim();
    const desc = document.getElementById("descricao-lista-edit").value.trim();

    if (!nome) {
        alert("O nome é obrigatório!");
        return;
    }

    const response = await fetch(`/listas/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ nome: nome, descricao: desc})
    });

    const atualizada = await response.json();

    listaSelecionada.querySelector("span").textContent = atualizada.nome;
    listaSelecionada.dataset.desc = desc

    fecharModalPerfil("modalEditLista");
};

window.confirmarRemocaoLista = async function () {
    if (!listaParaRemoverId) return;

    await fetch(`/listas/${listaParaRemoverId}`, { method: "DELETE" });

    document.querySelector(`button.lista[data-id='${listaParaRemoverId}']`)?.remove();

    listaParaRemoverId = null;
    listaParaRemover = null;

    fecharModalPerfil("modalRemoveLista");
};

// Abre o modal de confirmação de logout
window.abrirModalLogout = function () {
    document.getElementById('modalConfirmarLogout').style.display = 'flex';
}

// Confirma o logout e redireciona
window.confirmarLogout = function () {
    window.location.href = "/logout";
}

// Função de logout (antiga - não será mais usada diretamente)
function fazerLogout() {
    abrirModalLogout();
}