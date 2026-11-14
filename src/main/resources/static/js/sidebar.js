// Inicializa ícones Lucide
lucide.createIcons();

// Variável para guardar qual lista será removida
let listaParaRemover = null;
let listaSelecionada = null;  // lista escolhida para editar
let listaParaRemoverId = null; // id da lista para remover

/**
 * Recolhe/expande a barra lateral. Recolhida, mostra só os ícones, e cada um ganha uma dica
 * com o nome. O estado fica guardado só enquanto a aba está aberta (sessionStorage): ela
 * continua recolhida ao navegar entre as telas, mas numa visita nova abre expandida.
 */
function aplicarEstadoSidebar(recolhida) {
    const sidebar = document.querySelector('.sidebar');
    const botao = document.getElementById('btnRecolherSidebar');

    botao.title = recolhida ? 'Expandir barra lateral' : 'Recolher barra lateral';
    botao.innerHTML = '<i data-lucide="' + (recolhida ? 'panel-left-open' : 'panel-left-close') + '"></i>';

    // Com o texto escondido, a dica passa a ser a única forma de saber o que o ícone faz
    sidebar.querySelectorAll('.menu-item, .lista, .nova-lista, .btn-sair-sidebar, .area-atual').forEach(el => {
        const rotulo = el.querySelector('span');
        if (!rotulo) return;
        if (recolhida || el.classList.contains('lista')) {
            el.title = rotulo.textContent.trim();
        } else {
            el.removeAttribute('title');
        }
    });

    lucide.createIcons();
}

window.alternarSidebar = function () {
    const recolhida = document.querySelector('.sidebar').classList.toggle('recolhida');
    try { sessionStorage.setItem('sidebarRecolhida', recolhida ? '1' : '0'); } catch (e) { /* sem armazenamento: vale só nesta tela */ }
    aplicarEstadoSidebar(recolhida);
}

// A classe "recolhida" já vem aplicada pelo sidebar.html (antes da primeira pintura, para a
// barra não abrir e fechar de novo); aqui só acertam o ícone e as dicas.
if (document.querySelector('.sidebar')?.classList.contains('recolhida')) {
    aplicarEstadoSidebar(true);
}


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

/**
 * Fecha um modal pelo ID
 */
// Preenche/limpa um campo por código e avisa o botão "OK" (que fica apagado com o campo vazio)
function definirValorCampo(campo, valor) {
    if (!campo) return;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
}

// Botões "OK" de Adicionar/Editar Lista ficam apagados, com a mensagem, enquanto o nome estiver vazio
document.addEventListener('DOMContentLoaded', function () {
    Aviso.exigirTexto(document.getElementById('nome-lista'), document.getElementById('btnOkAddLista'),
        'Digite o nome da lista para continuar');
    Aviso.exigirTexto(document.getElementById('nome-lista-edit'), document.getElementById('btnOkEditLista'),
        'Digite o nome da lista para continuar');
});

window.fecharModalPerfil = function (nomeModal) {
    const modal = document.getElementById(nomeModal);
    modal.style.display = 'none';

    // Remove mensagem de erro ao fechar o modal
    const erroMsg = modal.querySelector('.mensagem-erro');
    if (erroMsg) {
        erroMsg.textContent = '';
    }

    definirValorCampo(document.getElementById("nome-lista"), "");

    const desc = document.getElementById("descricao-lista");
    if(desc){
        desc.value = "";
    }

    // Limpa a mensagem de erro da foto de perfil, se existir
    modal.querySelectorAll('.perfil-foto-msg').forEach(m => { m.textContent = ''; });

    // Limpa o campo de senha, se existir
    const inputSenha = modal.querySelector('input[type="password"]');
    if (inputSenha) {
        inputSenha.classList.remove('campo-invalido');
        inputSenha.value = '';
    }
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
        fecharModalPerfil('modalPerfil');
        fecharModalPerfil('modalVerificacao');
        fecharModalPerfil('modalAddLista');
        fecharModalPerfil('modalEditLista');
        fecharModalPerfil('modalRemoveLista');
        fecharModalPerfil('modalConfirmarLogout');
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
        // Respeita ?view=agendadas|todas (senão "Para Hoje" aparecia marcado por um instante)
        const visao = new URLSearchParams(window.location.search).get('view');
        const posicao = visao === 'agendadas' ? 1 : visao === 'todas' ? 2 : 0;
        const btnVisao = document.querySelectorAll('.menu-item')[posicao];
        if (btnVisao) btnVisao.classList.add('active');
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

    // Remover conta é destrutivo: ícone em vermelho
    modal.querySelector('.verificacao').classList.toggle('perigo', tipo === 'remover');

    // Limpa o campo de senha (e o aviso de senha incorreta) e mostra o modal
    document.getElementById('senhaConfirmacao').value = '';
    document.getElementById('senhaConfirmacao').classList.remove('campo-invalido');
    modal.querySelectorAll('.mensagem-erro').forEach(m => { m.textContent = ''; });
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
    btn.dataset.desc = lista.descricao || ''
    btn.oncontextmenu = (e) => mostrarMenuContexto(e, "menuContextoLista", btn);

    btn.innerHTML = `
        <i data-lucide="list"></i>
        <span>${lista.nome}</span>
    `;
    if (document.querySelector('.sidebar.recolhida')) btn.title = lista.nome;

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
    botao.dataset.desc = descricao || ''
    
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

    definirValorCampo(document.getElementById("nome-lista-edit"), nomeAtual);
    // Lista sem descrição: campo vazio (antes aparecia "undefined")
    document.getElementById("descricao-lista-edit").value = desc || '';

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
    definirValorCampo(document.getElementById("nome-lista"), "");
    const descricao = document.getElementById("descricao-lista").value.trim();
    const areaId = window.areaAtualId; // ID da área carregada na página
    
    if (!nome) {
        Aviso.mostrar("O nome da lista é obrigatório!", 'erro');
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
        Aviso.mostrar("O nome é obrigatório!", 'erro');
        return;
    }

    const response = await fetch(`/listas/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ nome: nome, descricao: desc})
    });

    const atualizada = await response.json();

    listaSelecionada.querySelector("span").textContent = atualizada.nome;
    listaSelecionada.dataset.desc = desc || ''

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
    try { sessionStorage.removeItem('sidebarRecolhida'); sessionStorage.removeItem('fotoPerfil'); } catch (e) { /* sem armazenamento */ }
    window.location.href = "/logout";
}

// ===== Foto de perfil =====
const URL_FOTO_PERFIL = '/perfil/foto';
const LADO_FOTO_PERFIL = 400;      // a imagem é recortada em quadrado e reduzida antes do envio
const TAMANHO_MAX_FOTO = 20 * 1024 * 1024; // limite só para não tentar processar arquivos enormes
let urlFotoAtual = null;

function mostrarMensagemFoto(texto) {
    document.querySelectorAll('.perfil-foto-msg').forEach(m => { m.textContent = texto || ''; });
}

// Foto guardada em sessionStorage (só nesta aba) para a sidebar já nascer com ela ao trocar de tela
const CHAVE_CACHE_FOTO = 'fotoPerfil';

function emailDoPerfil() {
    const el = document.querySelector('.perfil .email');
    return el ? el.textContent.trim() : '';
}

function lerCacheFoto() {
    try {
        const cache = JSON.parse(sessionStorage.getItem(CHAVE_CACHE_FOTO) || 'null');
        return cache && cache.email === emailDoPerfil() ? cache.dados : null;
    } catch (e) { return null; }
}

function guardarCacheFoto(dados) {
    try {
        if (dados) {
            sessionStorage.setItem(CHAVE_CACHE_FOTO, JSON.stringify({ email: emailDoPerfil(), dados: dados }));
        } else {
            sessionStorage.removeItem(CHAVE_CACHE_FOTO);
        }
    } catch (e) { /* sem armazenamento ou foto grande demais: só não guarda */ }
}

function blobParaDataUrl(blob) {
    return new Promise((resolve, reject) => {
        const leitor = new FileReader();
        leitor.onload = () => resolve(leitor.result);
        leitor.onerror = reject;
        leitor.readAsDataURL(blob);
    });
}

// Aplica (ou remove, com null) a foto no avatar da sidebar e no do modal de Perfil.
// manterAvatarSidebar: o avatar da sidebar já mostra exatamente esta foto (veio do cache), então
// não é mexido, para não piscar.
function definirFotoPerfil(url, manterAvatarSidebar) {
    if (urlFotoAtual) URL.revokeObjectURL(urlFotoAtual);
    urlFotoAtual = url;

    document.querySelectorAll('.foto-img').forEach(img => {
        if (manterAvatarSidebar && img.closest('.perfil')) {
            img.parentElement.classList.add('com-foto');
            return;
        }
        if (url) {
            img.src = url;
        } else {
            img.removeAttribute('src');
        }
        img.parentElement.classList.toggle('com-foto', !!url);
    });

    document.querySelectorAll('.btn-foto-remover').forEach(b => { b.style.display = url ? 'flex' : 'none'; });
    document.querySelectorAll('.btn-foto-texto').forEach(t => { t.textContent = url ? 'Alterar foto' : 'Adicionar foto'; });
}

window.carregarFotoPerfil = async function () {
    try {
        const resposta = await fetch(URL_FOTO_PERFIL, { cache: 'no-cache' });
        if (resposta.status === 200) {
            const blob = await resposta.blob();
            const dados = await blobParaDataUrl(blob);
            const igualAoCache = dados === lerCacheFoto();
            if (!igualAoCache) guardarCacheFoto(dados);
            definirFotoPerfil(URL.createObjectURL(blob), igualAoCache);
        } else {
            guardarCacheFoto(null);
            definirFotoPerfil(null);
        }
    } catch (e) {
        // Sem conexão: mantém a foto guardada, se houver
        if (!lerCacheFoto()) definirFotoPerfil(null);
    }
}

window.escolherFotoPerfil = function () {
    mostrarMensagemFoto('');
    const input = document.querySelector('.input-foto-perfil');
    if (input) input.click();
}

// Recorta a imagem em quadrado, reduz e converte para JPEG
function prepararImagemPerfil(arquivo) {
    return new Promise((resolve, reject) => {
        const url = URL.createObjectURL(arquivo);
        const img = new Image();
        img.onload = () => {
            URL.revokeObjectURL(url);
            const lado = Math.min(img.naturalWidth, img.naturalHeight);
            if (!lado) { reject(new Error('imagem vazia')); return; }
            const saida = Math.min(lado, LADO_FOTO_PERFIL);
            const canvas = document.createElement('canvas');
            canvas.width = saida;
            canvas.height = saida;
            const ctx = canvas.getContext('2d');
            ctx.fillStyle = '#ffffff'; // PNG/GIF transparentes ficam com fundo branco no JPEG
            ctx.fillRect(0, 0, saida, saida);
            ctx.drawImage(img,
                (img.naturalWidth - lado) / 2, (img.naturalHeight - lado) / 2, lado, lado,
                0, 0, saida, saida);
            canvas.toBlob(blob => blob ? resolve(blob) : reject(new Error('falha ao converter')), 'image/jpeg', 0.9);
        };
        img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('imagem ilegível')); };
        img.src = url;
    });
}

window.aoEscolherFotoPerfil = async function (input) {
    const arquivo = input.files && input.files[0];
    input.value = ''; // permite escolher o mesmo arquivo de novo depois
    if (!arquivo) return;

    if (!/^image\/(jpeg|png|gif|webp)$/.test(arquivo.type)) {
        mostrarMensagemFoto('Formato inválido. Use uma imagem JPG, PNG, GIF ou WebP.');
        return;
    }
    if (arquivo.size > TAMANHO_MAX_FOTO) {
        mostrarMensagemFoto('A imagem é grande demais. Escolha uma de até 20 MB.');
        return;
    }

    mostrarMensagemFoto('');
    const botoes = document.querySelectorAll('.btn-foto');
    botoes.forEach(b => { b.disabled = true; });

    try {
        const imagem = await prepararImagemPerfil(arquivo);
        const dados = new FormData();
        dados.append('foto', imagem, 'foto.jpg');

        const resposta = await fetch(URL_FOTO_PERFIL, { method: 'POST', body: dados });
        if (!resposta.ok) {
            const texto = await resposta.text();
            mostrarMensagemFoto(texto && texto.length < 200 ? texto : 'Não foi possível salvar a foto.');
        } else {
            await carregarFotoPerfil();
        }
    } catch (e) {
        mostrarMensagemFoto('Não foi possível ler essa imagem. Tente outro arquivo.');
    } finally {
        botoes.forEach(b => { b.disabled = false; });
    }
}

window.removerFotoPerfil = async function () {
    mostrarMensagemFoto('');
    const botoes = document.querySelectorAll('.btn-foto');
    botoes.forEach(b => { b.disabled = true; });

    try {
        const resposta = await fetch(URL_FOTO_PERFIL, { method: 'DELETE' });
        if (resposta.ok) {
            guardarCacheFoto(null);
            definirFotoPerfil(null);
        } else {
            mostrarMensagemFoto('Não foi possível remover a foto.');
        }
    } catch (e) {
        mostrarMensagemFoto('Não foi possível remover a foto.');
    } finally {
        botoes.forEach(b => { b.disabled = false; });
    }
}

// Carrega a foto ao abrir qualquer página que use a sidebar
carregarFotoPerfil();
