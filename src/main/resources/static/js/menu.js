// Mapeamento dos nomes das listas

const nomesListas = {
    'faculdade': 'Faculdade',
    'casa': 'Casa',
    'remomeada': 'Remomeada'
};

// Array para armazenar as tarefas
let tarefas = [];
let listasCarregadas = [];
let membrosCarregados = [];
let listaAtual = null;
let tarefaSelecionada = null;

// Categorias da área atual (usadas para badges e seleção nas tarefas)
let categorias = [];
let categoriaIdsSelecionadosTarefa = [];

// Listas
let listas = document.querySelectorAll('.lista');

async function carregarCategorias() {
    if (!window.areaAtualId) { categorias = []; return; }
    const resposta = await fetch(`/areasTrabalho/${window.areaAtualId}/categorias`);
    categorias = resposta.ok ? await resposta.json() : [];
}

async function carregarTarefas(listaId) {
    const [response] = await Promise.all([
        fetch(`/listas/${listaId}/tarefas`),
        carregarCategorias()
    ]);
    if (!response.ok) return [];
    const tarefasJson = await response.json();
    tarefas = tarefasJson; // preenche a variável global

    // renderiza apenas as tarefas da lista filtrada
    renderizarTarefas(+listaId);
}

async function carregarTarefasHoje() {

    let response

    const promessaExtras = carregarCategorias();

    if(window.areaAtualId){
        response = await fetch(`/areasTrabalho/${window.areaAtualId}/tarefas`);
    }else{
        response = await fetch(`/user/tarefas`);
    }

    await promessaExtras;

    if (!response.ok) return [];
    const tarefasJson = await response.json();
    tarefas = tarefasJson; // preenche a variável global

    // renderiza apenas as tarefas da lista filtrada
    renderizarTarefas("hoje");
}

async function carregarTarefasTodas() {

    let response

    const promessaExtras = carregarCategorias();

    if(window.areaAtualId){
        response = await fetch(`/areasTrabalho/${window.areaAtualId}/tarefas`);
    }else{
        response = await fetch(`/user/tarefas`);
    }

    await promessaExtras;

    if (!response.ok) return [];
    const tarefasJson = await response.json();
    tarefas = tarefasJson; // preenche a variável global

    // renderiza apenas as tarefas da lista filtrada
    renderizarTarefas("todas");
}

async function carregarTarefasAgendadas() {

    let response

    const promessaExtras = carregarCategorias();

    if(window.areaAtualId){
        response = await fetch(`/areasTrabalho/${window.areaAtualId}/tarefas`);
    }else{
        response = await fetch(`/user/tarefas`);
    }

    await promessaExtras;

    if (!response.ok) return [];
    const tarefasJson = await response.json();
    tarefas = tarefasJson; // preenche a variável global

    // renderiza apenas as tarefas da lista filtrada
    renderizarTarefas("agendadas");
}

// Chamada sempre que abrir o modal de tarefa
async function atualizarCamposModalTarefa() {
    let respListas

    // 1) Carregar listas da área
    if(window.areaAtualId){
        respListas = await fetch(`/areasTrabalho/${window.areaAtualId}/listas`);
    }else{
        respListas = await fetch(`/user/listas`);
    }

    listasCarregadas = await respListas.json();

    const selectLista = document.getElementById("tarefaLista");
    selectLista.innerHTML = `<option value="">Selecione uma lista...</option>`;

    listasCarregadas.forEach(l => {
        selectLista.innerHTML += `
            <option value="${l.id}" ${listaAtual === l.id ? "selected" : ""}>
                ${l.nome}
            </option>
        `;
    });

    // 2) Carregar membros responsáveis
    let respMembros;

    if(window.areaAtualId){
        respMembros = await fetch(`/areasTrabalho/${window.areaAtualId}/membros`);
    }else{
        respMembros = await fetch(`/user/membros`);
    }

    membrosCarregados = await respMembros.json();

    const selectResp = document.getElementById("tarefaResponsavel");
    selectResp.innerHTML = `<option value="">Selecione...</option>`;

    membrosCarregados.forEach(m => {
        selectResp.innerHTML += `
            <option value="${m.id}">${m.nome}</option>
        `;
    });

    // Se estiver editando uma tarefa, preenche tudo
    if (tarefaSelecionada) {
        preencherModalEdicao();
    }
}

// Script para abrir/fechar modal de Adicionar Tarefa
document.addEventListener('DOMContentLoaded', function() {
    const btnAddTarefa = document.getElementById('btnAddTarefa');
    const modalAddTarefa = document.getElementById('modalAddTarefa');
    const btnCancelarTarefa = document.getElementById('btnCancelarTarefa');
    const btnOkTarefa = document.getElementById('btnOkTarefa');
    const containerTarefas = document.getElementById('containerTarefas');
    const tituloModalTarefa = document.querySelector('.titulo-modal-tarefa');
    const modalConfirmarRemocao = document.getElementById('modalConfirmarRemocao');
    const btnCancelarRemocao = document.getElementById('btnCancelarRemocao');
    const btnConfirmarRemocao = document.getElementById('btnConfirmarRemocao');
    const btnParaHoje = document.querySelector('.menu-item:nth-child(1)');
    const btnAgendadas = document.querySelector('.menu-item:nth-child(2)');
    const btnTodasTarefas = document.querySelector('.menu-item:nth-child(3)');
    let tarefaParaRemover = null;

    btnParaHoje.innerText

    const btnFiltro = document.getElementById('btnFiltro');
    const btnOrdenar = document.getElementById('btnOrdenar');
    const menuFiltro = document.getElementById('menuFiltro');
    const menuOrdenar = document.getElementById('menuOrdenar');
    let filtroAtivo = 'todas';
    let ordenacaoAtiva = null;

    // Data de Hoje

    const hoje = new Date();
    const dia = String(hoje.getDate()).padStart(2, "0");
    const mes = String(hoje.getMonth() + 1).padStart(2, "0");
    const ano = String(hoje.getFullYear()).slice(-2); // pega só os dois últimos dígitos

    const dataFormatada = `${dia}/${mes}/${ano}`;

    document.getElementById("subtitulo-principal").textContent = `- ${dataFormatada}`;

    window.mudarTituloPrincipal = function (elementoClicado) {

        const novoTitulo = elementoClicado.innerText.trim();
        const desc = elementoClicado.dataset.desc
        const tituloH2 = document.getElementById('titulo-principal');
        const subtituloSpan = document.getElementById('subtitulo-principal');

        // Atualiza o título
        if (tituloH2) {
            tituloH2.innerText = novoTitulo;
        }

        if(desc){
            subtituloSpan.innerText = desc;
        }else{
            subtituloSpan.innerText = "";
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
            }
        }
    }

    window.resetFiltro = function(){
        filtroAtivo = "todas"
        document.getElementById("filtroTexto").value = ""
        document.querySelectorAll('#menuFiltro .menu-filtro-item').forEach(btn => {
            btn.classList.remove('active');
        });
    }

    window.resetOrdem = function(){
        ordenacaoAtiva = null
        document.querySelectorAll('#menuOrdenar .menu-filtro-item').forEach(btn => {
                btn.classList.remove('active');
        });
    }

    // Ativa a aba inicial (hoje/agendadas/todas). "Para Hoje", "Agendadas" e "Todas as
    // Tarefas" são visões globais (todas as áreas de trabalho): ao navegar para cá vindo de
    // dentro de uma área (via ?view=...), isso também garante uma forma de "sair" da área.
    {
        const viewInicial = new URLSearchParams(window.location.search).get('view');
        const botaoInicial = viewInicial === 'agendadas' ? btnAgendadas
            : viewInicial === 'todas' ? btnTodasTarefas
            : btnParaHoje;

        document.querySelectorAll('.menu-item').forEach(i => i.classList.remove('active'));
        document.querySelectorAll('.lista').forEach(i => i.classList.remove('active'));
        botaoInicial.classList.add('active');

        listaAtual = viewInicial === 'agendadas' ? "agendadas" : viewInicial === 'todas' ? "todas" : "hoje"

        // Mantém a URL refletindo a aba ativa (só faz sentido fora de uma área de trabalho,
        // já que dentro dela a URL é a própria área).
        if (!window.areaAtualId) {
            history.replaceState(null, '', '/menu?view=' + listaAtual);
        }

        resetFiltro()
        resetOrdem()

        if (listaAtual === "agendadas") {
            carregarTarefasAgendadas()
        } else if (listaAtual === "todas") {
            carregarTarefasTodas()
        } else {
            carregarTarefasHoje()
        }
        atualizarCamposModalTarefa()

        mudarTituloPrincipal(botaoInicial)
    }

    window.clickLista = function(lista) {
        listas.forEach(l => l.classList.remove('active'));
        document.querySelectorAll('.menu-item').forEach(i => i.classList.remove('active'));
        lista.classList.add('active');
        const listaId = lista.getAttribute('data-id');
        listaAtual = +listaId

        resetFiltro()
        resetOrdem()

        carregarTarefas(listaId);
        atualizarCamposModalTarefa()
        mudarTituloPrincipal(lista)
    }

    listas.forEach(lista => {
        lista.addEventListener('click', () => clickLista(lista));
    });

    window.adicionarListaNaTela = function (lista) {
        const container = document.getElementById("listasContainer");
        const btn = document.createElement("button");
        btn.className = "lista";
        btn.dataset.id = lista.id;
        btn.dataset.desc = lista.descricao
        btn.onclick = () => clickLista(btn);
        btn.oncontextmenu = (e) => mostrarMenuContexto(e, "menuContextoLista", btn);

        btn.innerHTML = `
            <i data-lucide="list"></i>
            <span>${lista.nome}</span>
        `;

        container.appendChild(btn);

        listas = document.querySelectorAll('.lista');
        lucide.createIcons(); // recarrega ícones
    }

    // Abrir/fechar menus de filtro e ordenação
    btnFiltro.addEventListener('click', function(e) {
        e.stopPropagation();
        const isVisible = menuFiltro.style.display === 'block';
        menuFiltro.style.display = isVisible ? 'none' : 'block';
        menuOrdenar.style.display = 'none';

        if (!isVisible) {
            const rect = btnFiltro.getBoundingClientRect();
            menuFiltro.style.left = (rect.right - 220) + 'px';
            menuFiltro.style.top = (rect.bottom + 5) + 'px';
        }
    });

    btnOrdenar.addEventListener('click', function(e) {
        e.stopPropagation();
        const isVisible = menuOrdenar.style.display === 'block';
        menuOrdenar.style.display = isVisible ? 'none' : 'block';
        menuFiltro.style.display = 'none';

        if (!isVisible) {
            const rect = btnOrdenar.getBoundingClientRect();
            menuOrdenar.style.left = (rect.right - 220) + 'px';
            menuOrdenar.style.top = (rect.bottom + 5) + 'px';
        }
    });

    // Fechar menus ao clicar fora
    document.addEventListener('click', function(e) {
        if (!e.target.closest('.menu-filtro') && !e.target.closest('.btn-filtro') && !e.target.closest('.btn-ordenar')) {
            menuFiltro.style.display = 'none';
            menuOrdenar.style.display = 'none';
        }
    });

    document.getElementById("filtroTexto").addEventListener("keydown", function (e) {
        if (e.key === "Enter") {
            // Mark
            if(listaAtual == "hoje"){
                carregarTarefasHoje();
            }else if(listaAtual == "agendadas"){
                carregarTarefasAgendadas();
            }else if(listaAtual == "todas"){
                carregarTarefasTodas();
            }else{
                carregarTarefas(listaAtual);
            }

            menuFiltro.style.display = 'none';
            lucide.createIcons();
        }
    });

    // Aplicar filtros
    document.querySelectorAll('#menuFiltro .menu-filtro-item').forEach(item => {
        item.addEventListener('click', function() {
            filtroAtivo = this.dataset.filtro;
            document.querySelectorAll('#menuFiltro .menu-filtro-item').forEach(btn => {
                btn.classList.remove('active');
            });
            this.classList.add('active');

            if(listaAtual == "hoje"){
                carregarTarefasHoje();
            }else if(listaAtual == "agendadas"){
                carregarTarefasAgendadas();
            }else if(listaAtual == "todas"){
                carregarTarefasTodas();
            }else{
                carregarTarefas(listaAtual);
            }

            menuFiltro.style.display = 'none';
            lucide.createIcons();
        });
    });

    // Aplicar ordenação
    document.querySelectorAll('#menuOrdenar .menu-filtro-item').forEach(item => {
        item.addEventListener('click', function() {
            ordenacaoAtiva = this.dataset.ordem;
            document.querySelectorAll('#menuOrdenar .menu-filtro-item').forEach(btn => {
                btn.classList.remove('active');
            });
            this.classList.add('active');

            // MARKDOWN
            if(listaAtual == "hoje"){
                carregarTarefasHoje();
            }else if(listaAtual == "agendadas"){
                carregarTarefasAgendadas();
            }else if(listaAtual == "todas"){
                carregarTarefasTodas();
            }else{
                carregarTarefas(listaAtual);
            }

            menuOrdenar.style.display = 'none';
            lucide.createIcons();
        });
    });

    // Modal de confirmação de remoção
    btnCancelarRemocao.addEventListener('click', function() {
        modalConfirmarRemocao.style.display = 'none';
        tarefaParaRemover = null;
    });

    btnConfirmarRemocao.addEventListener('click', function() {
        if (tarefaParaRemover) {
            excluirTarefa(tarefaParaRemover.id, tarefaParaRemover.lista);
            modalConfirmarRemocao.style.display = 'none';
            tarefaParaRemover = null;
        }
    });

    // Fechar modal de confirmação ao clicar no overlay
    modalConfirmarRemocao.addEventListener('click', function(e) {
        if (e.target === modalConfirmarRemocao) {
            modalConfirmarRemocao.style.display = 'none';
            tarefaParaRemover = null;
        }
    });

    // Abrir modal
    btnAddTarefa.addEventListener('click', function() {
        btnOkTarefa.style.display = "block";
        btnCancelarTarefa.textContent = "Cancelar";

        tarefaSelecionada = null;
        document.getElementById("tituloModalTarefa").textContent = "Adicionar Tarefa";
        document.getElementById("btnChecklist").disabled = true;
        document.getElementById("btnChecklist").title = "Salve a tarefa antes de adicionar um checklist";

        atualizarCamposModalTarefa();
        modalAddTarefa.style.display = "flex";
    });

    // Fechar modal ao clicar no overlay
    modalAddTarefa.addEventListener('click', function(e) {
        if (e.target === modalAddTarefa) {
            modalAddTarefa.style.display = 'none';
            tarefaSelecionada = null;
            limparFormulario();
        }
    });

    // Fechar modal ao clicar em Cancelar
    btnCancelarTarefa.addEventListener('click', function() {
        modalAddTarefa.style.display = 'none';
        tarefaSelecionada = null;
        limparFormulario();
    });

    // Ação do botão OK - Adicionar ou Editar tarefa
    btnOkTarefa.addEventListener("click", async function () {

        const listaId = +document.getElementById('tarefaLista').value;
        const titulo = document.getElementById('tarefaTitulo').value.trim();
        const descricao = document.getElementById('tarefaDescricao').value.trim();
        const dataFim = document.getElementById('tarefaDataFim').value || null; // LocalDate
        const responsavelId = +document.getElementById('tarefaResponsavel').value || null;
        const notificacoes = document.getElementById('tarefaNotificacoes').checked;
        const categoriaIds = categoriaIdsSelecionadosTarefa.slice();

        if (!listaId || !titulo) {
            alert("Lista e título são obrigatórios.");
            return;
        }

        // Corpo JSON enviado ao backend
        const corpo = {
            titulo,
            descricao,
            dataFim,            // LocalDate string
            notificacoes,
            categoriaIds,
            listaId,
            responsavelId
        };

        let resposta;
        let tarefaCriadaOuEditada;

        // EDITAR
        if (tarefaSelecionada) {
            resposta = await fetch(`/tarefas/${tarefaSelecionada.id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(corpo)
            });

        // CRIAR
        } else {
            resposta = await fetch(`/tarefas`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(corpo)
            });
        }

        if (!resposta.ok) {
            const txt = await resposta.text();
            throw new Error("Erro ao salvar a tarefa: " + txt);
        }

        tarefaCriadaOuEditada = await resposta.json();

        // Atualiza arrays locais
        if (tarefaSelecionada) {
            const idx = tarefas.findIndex(t => t.id === tarefaCriadaOuEditada.id);
            if (idx !== -1) tarefas[idx] = tarefaCriadaOuEditada;
        } else {
            tarefas.push(tarefaCriadaOuEditada);
        }

        // Renderizar apenas tarefas daquela lista
        if(listaAtual == "hoje"){
            carregarTarefasHoje();
        }else if(listaAtual == "agendadas"){
            carregarTarefasAgendadas();
        }else if(listaAtual == "todas"){
            carregarTarefasTodas();
        }else{
            carregarTarefas(listaId);
        }

        // Reset
        modalAddTarefa.style.display = "none";
        limparFormulario();
        tarefaSelecionada = null;
        categoriaIdsSelecionadosTarefa = [];
    });

    window.limparFormulario = function () {
        document.querySelectorAll("#modalAddTarefa input, #modalAddTarefa textarea, #modalAddTarefa select")
            .forEach(el => el.disabled = false);
        document.getElementById("btnAnexo").disabled = false;
        // Sem tarefa selecionada ainda não há onde guardar itens de checklist.
        document.getElementById("btnChecklist").disabled = true;
        document.getElementById("btnChecklist").title = "Salve a tarefa antes de adicionar um checklist";
        document.getElementById("btnCategoria").disabled = false;

        document.getElementById('tarefaLista').disabled = false;
        document.getElementById('tarefaLista').value = '';
        document.getElementById('tarefaTitulo').value = '';
        document.getElementById('tarefaDescricao').value = '';
        document.getElementById('tarefaDataFim').value = '';
        document.getElementById('tarefaResponsavel').value = '';
        document.getElementById('tarefaNotificacoes').checked = false;
        categoriaIdsSelecionadosTarefa = [];
        btnCategoria.style.borderColor = '';
        btnCategoria.style.backgroundColor = '';
    }

    window.preencherFormulario = async function (tarefa) {

        let respListas

        if(window.areaAtualId){
            respListas = await fetch(`/areasTrabalho/${window.areaAtualId}/listas`);
        }else{
            respListas = await fetch(`/user/listas`);
        }

        listasCarregadas = await respListas.json();

        document.querySelectorAll("#modalAddTarefa input, #modalAddTarefa textarea, #modalAddTarefa select")
            .forEach(el => el.disabled = false);
        document.getElementById("btnAnexo").disabled = false;
        document.getElementById("btnChecklist").disabled = false;
        document.getElementById("btnChecklist").title = "";
        document.getElementById("btnCategoria").disabled = false;

        const selectLista = document.getElementById("tarefaLista");
        selectLista.innerHTML = `<option value="">Selecione uma lista...</option>`;

        listasCarregadas.forEach(l => {
            selectLista.innerHTML += `
                <option value="${l.id}" ${tarefa.listaId === l.id ? "selected" : ""}>
                    ${l.nome}
                </option>
            `;
        });

        document.getElementById('tarefaTitulo').value = tarefa.titulo;
        document.getElementById('tarefaDescricao').value = tarefa.descricao;
        document.getElementById('tarefaDataFim').value = tarefa.dataFim;
        document.getElementById('tarefaResponsavel').value = tarefa.responsavel;
        document.getElementById('tarefaNotificacoes').checked = tarefa.notificacoes;
        categoriaIdsSelecionadosTarefa = (tarefa.categoriaIds || []).slice();
        atualizarVisualBotaoCategoria();
    }

    window.visualizarTarefa = async function(tarefa) {

        let respListas

        if(window.areaAtualId){
            respListas = await fetch(`/areasTrabalho/${window.areaAtualId}/listas`);
        }else{
            respListas = await fetch(`/user/listas`);
        }

        listasCarregadas = await respListas.json();

        const selectLista = document.getElementById("tarefaLista");
        selectLista.innerHTML = `<option value="">Selecione uma lista...</option>`;

        listasCarregadas.forEach(l => {
            selectLista.innerHTML += `
                <option value="${l.id}" ${tarefa.listaId === l.id ? "selected" : ""}>
                    ${l.nome}
                </option>
            `;
        });

        document.getElementById("tarefaTitulo").value = tarefa.titulo;
        document.getElementById("tarefaDescricao").value = tarefa.descricao || "";
        document.getElementById("tarefaDataFim").value = tarefa.dataFim || "";
        document.getElementById("tarefaResponsavel").value = tarefa.responsavel || "";
        document.getElementById("tarefaNotificacoes").checked = tarefa.notificacoes || false;

        categoriaIdsSelecionadosTarefa = (tarefa.categoriaIds || []).slice();
        atualizarVisualBotaoCategoria();

        document.querySelectorAll("#modalAddTarefa input, #modalAddTarefa textarea, #modalAddTarefa select")
            .forEach(el => el.disabled = true);
        document.getElementById("btnAnexo").disabled = true;
        document.getElementById("btnChecklist").disabled = false;
        document.getElementById("btnChecklist").title = "";
        document.getElementById("btnCategoria").disabled = true;

        document.querySelector(".titulo-modal-tarefa").textContent = "Detalhes da Tarefa";
        document.getElementById("btnOkTarefa").style.display = "none";
        document.getElementById("btnCancelarTarefa").textContent = "Fechar";

        modalAddTarefa.style.display = 'flex';
    }

    window.atualizarTituloPagina = function (listaId) {
        const tituloPrincipal = document.getElementById('titulo-principal');
        tituloPrincipal.textContent = nomesListas[listaId] || 'Para Hoje';
    }

    window.renderizarTarefas = function (listaFiltro) {
        // VERIFICAR A PARTIR DAQUI ESSA MERDA

        containerTarefas.innerHTML = '';
        let tarefasFiltradas;

        const textoFiltro = document.getElementById("filtroTexto").value;
        console.log(textoFiltro)

        if(listaFiltro === "hoje"){
            const hoje = new Date();
            const dia = String(hoje.getDate()).padStart(2, "0");
            const mes = String(hoje.getMonth() + 1).padStart(2, "0");
            const ano = String(hoje.getFullYear());
            const dataFormatada = `${ano}-${mes}-${dia}`;
            tarefasFiltradas = tarefas.filter(t => t.dataFim === dataFormatada)

        }else if(listaFiltro === "agendadas"){
            const hoje = new Date();
            const dia = String(hoje.getDate()).padStart(2, "0");
            const mes = String(hoje.getMonth() + 1).padStart(2, "0");
            const ano = String(hoje.getFullYear());
            const dataFormatada = `${ano}-${mes}-${dia}`;
            tarefasFiltradas = tarefas.filter(t => t.dataFim && t.dataFim != dataFormatada)

        }else if(listaFiltro === "todas"){
            tarefasFiltradas = tarefas

        }else{
            tarefasFiltradas = tarefas.filter(t => t.listaId === listaFiltro);
        }

        if (filtroAtivo === 'andamento') {
            tarefasFiltradas = tarefasFiltradas.filter(t => !t.concluida);
        } else if (filtroAtivo === 'concluidas') {
            tarefasFiltradas = tarefasFiltradas.filter(t => t.concluida);
        } else if (filtroAtivo === 'atrasadas') {
            const hoje = new Date();
            hoje.setHours(0, 0, 0, 0);
            tarefasFiltradas = tarefasFiltradas.filter(t => {
                if (!t.dataFim) return false;
                const dataFim = new Date(t.dataFim + 'T00:00:00');
                return dataFim < hoje && !t.concluida;
            });
        }

        if (filtroAtivo === "titulo" && textoFiltro){
            const tex = textoFiltro.toLowerCase();
            tarefasFiltradas = tarefasFiltradas.filter(t =>
                t.titulo?.toLowerCase().includes(tex)
            );
        } else if (filtroAtivo === "responsavel" && textoFiltro){
            const tex = textoFiltro.toLowerCase();
            tarefasFiltradas = tarefasFiltradas.filter(t =>
                t.responsavelNome?.toLowerCase().includes(tex)
            );
        } else if (textoFiltro){
            // Filtro geral: busca em título, descrição e responsável
            const tex = textoFiltro.toLowerCase();
            tarefasFiltradas = tarefasFiltradas.filter(t =>
                (t.titulo && t.titulo.toLowerCase().includes(tex)) ||
                (t.descricao && t.descricao.toLowerCase().includes(tex)) ||
                (t.responsavelNome && t.responsavelNome.toLowerCase().includes(tex))
            );
        }

        // Ordenação original
        tarefasFiltradas.sort((a, b) => a.titulo.localeCompare(b.titulo));

        if (ordenacaoAtiva === 'data-fim') {
            tarefasFiltradas.sort((a, b) => {
                if (!a.dataFim) return 1;
                if (!b.dataFim) return -1;
                return new Date(a.dataFim) - new Date(b.dataFim);
            });
        } else if (ordenacaoAtiva === 'data-adicao') {
            tarefasFiltradas.sort((a, b) => a.id - b.id);
        } else if (ordenacaoAtiva === 'status') {
            tarefasFiltradas.sort((a, b) => a.concluida - b.concluida);
        } else if (ordenacaoAtiva === 'titulo') {
            tarefasFiltradas.sort((a, b) => a.titulo.localeCompare(b.titulo));
        } else if (ordenacaoAtiva === 'responsavel') {
            tarefasFiltradas.sort((a, b) => {
                const nomeA = a.responsavelNome ? a.responsavelNome.toLowerCase() : "";
                const nomeB = b.responsavelNome ? b.responsavelNome.toLowerCase() : "";
                return nomeB.localeCompare(nomeA);
            });
        }

        if (tarefasFiltradas.length === 0) return;

        tarefasFiltradas.forEach(tarefa => {
            const tarefaEl = document.createElement('div');
            tarefaEl.className = 'tarefa-item';
            tarefaEl.dataset.id = tarefa.id;

            tarefaEl.addEventListener('contextmenu', function(e) {
                e.preventDefault();
                tarefaSelecionada = tarefa;
                menuContextoTarefa.style.left = e.pageX + 'px';
                menuContextoTarefa.style.top = e.pageY + 'px';
                menuContextoTarefa.style.display = 'block';
            });

            tarefaEl.addEventListener('click', function(e) {
                // Evita abrir visualização ao clicar nos botões ou no checkbox
                if (e.target.closest('.tarefa-btn-excluir')) return;
                if (e.target.closest('.tarefa-btn-editar')) return;
                if (e.target.closest('.tarefa-checkbox')) return;

                tarefaSelecionada = tarefa;
                visualizarTarefa(tarefa);
            });

            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.className = 'tarefa-checkbox';
            checkbox.checked = tarefa.concluida;
            checkbox.addEventListener('change', function() {
                toggleTarefaConcluida(tarefa.id);
            });

            const conteudo = document.createElement('div');
            conteudo.className = 'tarefa-conteudo';

            const titulo = document.createElement('div');
            titulo.className = 'tarefa-titulo';
            titulo.textContent = tarefa.titulo;
            if (tarefa.concluida) titulo.classList.add('concluida');
            conteudo.appendChild(titulo);

            if (tarefa.descricao) {
                const descricao = document.createElement('div');
                descricao.className = 'tarefa-descricao';
                descricao.textContent = tarefa.descricao;
                conteudo.appendChild(descricao);
            }

            // Nas visões globais (Para Hoje/Agendadas/Todas) as tarefas vêm de várias áreas
            // e listas, então precisa deixar claro de onde cada uma é.
            const mostrarOrigem = typeof listaAtual === 'string' && tarefa.areaNome && tarefa.listaNome;

            if (mostrarOrigem || tarefa.dataFim || tarefa.responsavel || tarefa.checklistTotal || (tarefa.categoriaIds && tarefa.categoriaIds.length)) {
                const info = document.createElement('div');
                info.className = 'tarefa-info';

                if (mostrarOrigem) {
                    const origem = document.createElement('span');
                    origem.className = 'tarefa-origem-badge';
                    origem.innerHTML = `<i data-lucide="briefcase"></i> ${tarefa.areaNome} › ${tarefa.listaNome}`;
                    info.appendChild(origem);
                }

                if (tarefa.checklistTotal) {
                    const badge = document.createElement('span');
                    badge.className = 'tarefa-checklist-badge';
                    badge.innerHTML = `<i data-lucide="list-checks"></i> ${tarefa.checklistConcluidos}/${tarefa.checklistTotal}`;
                    info.appendChild(badge);
                }

                if (tarefa.categoriaIds) {
                    tarefa.categoriaIds.forEach(categoriaId => {
                        const categoria = categorias.find(c => c.id === categoriaId);
                        if (!categoria) return;
                        const badge = document.createElement('span');
                        badge.className = 'tarefa-categoria-badge';
                        badge.style.borderLeftColor = categoria.cor;
                        badge.innerHTML = `<i data-lucide="tag"></i> ${categoria.nome}`;
                        info.appendChild(badge);
                    });
                }

                if (tarefa.dataFim) {
                    const data = document.createElement('span');
                    data.className = 'tarefa-data';
                    data.innerHTML = `<i data-lucide="calendar"></i> ${formatarData(tarefa.dataFim)}`;
                    info.appendChild(data);
                }

                if (tarefa.responsavelNome) {
                    const resp = document.createElement('span');
                    resp.className = 'tarefa-responsavel';
                    resp.innerHTML = `<i data-lucide="user"></i> ${tarefa.responsavelNome}`;
                    info.appendChild(resp);
                }

                conteudo.appendChild(info);
            }

            // Botão editar
            const btnEditar = document.createElement('button');
            btnEditar.className = 'tarefa-btn-editar';
            btnEditar.title = 'Editar tarefa';
            btnEditar.innerHTML = '<i data-lucide="edit-2"></i>';
            btnEditar.addEventListener('click', function(e) {
                e.stopPropagation();
                tarefaSelecionada = tarefa;
                tituloModalTarefa.textContent = 'Editar Tarefa';
                preencherFormulario(tarefa);
                document.getElementById('tarefaLista').disabled = true;
                document.getElementById("btnOkTarefa").style.display = "block";
                document.getElementById("btnCancelarTarefa").textContent = "Cancelar";
                modalAddTarefa.style.display = 'flex';
            });

            // Botão excluir
            const btnExcluir = document.createElement('button');
            btnExcluir.className = 'tarefa-btn-excluir';
            btnExcluir.title = 'Excluir tarefa';
            btnExcluir.innerHTML = '<i data-lucide="trash-2"></i>';
            btnExcluir.addEventListener('click', function(e) {
                e.stopPropagation();
                tarefaParaRemover = { id: tarefa.id, lista: tarefa.listaId };
                modalConfirmarRemocao.style.display = 'flex';
            });

            tarefaEl.appendChild(checkbox);
            tarefaEl.appendChild(conteudo);
            tarefaEl.appendChild(btnEditar);
            tarefaEl.appendChild(btnExcluir);
            containerTarefas.appendChild(tarefaEl);
        });

        lucide.createIcons();
    }

    btnParaHoje.addEventListener('click', function() {
        // Para Hoje/Agendadas/Todas são visões globais (todas as áreas de trabalho). Se
        // estivermos dentro de uma área, navega para o menu pessoal em vez de filtrar só
        // as tarefas dessa área — essa é a única forma de "sair" de uma área de trabalho.
        if (window.areaAtualId) {
            window.location.href = '/menu?view=hoje';
            return;
        }

        history.replaceState(null, '', '/menu?view=hoje');

        document.querySelectorAll('.menu-item').forEach(i => i.classList.remove('active'));
        document.querySelectorAll('.lista').forEach(i => i.classList.remove('active'));
        this.classList.add('active');

        listaAtual = "hoje"
        resetFiltro()
        resetOrdem()

        carregarTarefasHoje()
        atualizarCamposModalTarefa()
        mudarTituloPrincipal(btnParaHoje)
    });

    btnAgendadas.addEventListener('click', function() {
        if (window.areaAtualId) {
            window.location.href = '/menu?view=agendadas';
            return;
        }

        history.replaceState(null, '', '/menu?view=agendadas');

        document.querySelectorAll('.menu-item').forEach(i => i.classList.remove('active'));
        document.querySelectorAll('.lista').forEach(i => i.classList.remove('active'));
        this.classList.add('active');

        listaAtual = "agendadas"
        resetFiltro()
        resetOrdem()

        carregarTarefasAgendadas()
        atualizarCamposModalTarefa()
        mudarTituloPrincipal(btnAgendadas)
    });

    btnTodasTarefas.addEventListener('click', function() {
        if (window.areaAtualId) {
            window.location.href = '/menu?view=todas';
            return;
        }

        history.replaceState(null, '', '/menu?view=todas');

        document.querySelectorAll('.menu-item').forEach(i => i.classList.remove('active'));
        document.querySelectorAll('.lista').forEach(i => i.classList.remove('active'));
        this.classList.add('active');

        listaAtual = "todas"
        resetFiltro()
        resetOrdem()

        carregarTarefasTodas()
        atualizarCamposModalTarefa()
        mudarTituloPrincipal(btnTodasTarefas)
    });

    window.toggleTarefaConcluida = async function (tarefaId) {
        const tarefa = tarefas.find(t => t.id === tarefaId);
        if (tarefa) {
            tarefa.concluida = !tarefa.concluida;

            const titulo = tarefa.titulo
            const listaId = tarefa.listaId
            const concluida = tarefa.concluida
            const responsavelId = tarefa.responsavel

            const corpo = {
                titulo,
                listaId,
                concluida,
                responsavelId
            };

            let resposta;
            let tarefaCriadaOuEditada;

            // EDITAR
            resposta = await fetch(`/tarefas/${tarefa.id}/toggle`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(corpo)
            });

            if (!resposta.ok) {
                const txt = await resposta.text();
                throw new Error("Erro ao salvar a tarefa: " + txt);
            }

            tarefaCriadaOuEditada = await resposta.json();

            const idx = tarefas.findIndex(t => t.id === tarefaCriadaOuEditada.id);
            if (idx !== -1) tarefas[idx] = tarefaCriadaOuEditada;

            // Renderizar apenas tarefas daquela lista
            if(listaAtual == "hoje"){
                carregarTarefasHoje();
            }else if(listaAtual == "agendadas"){
                carregarTarefasAgendadas();
            }else if(listaAtual == "todas"){
                carregarTarefasTodas();
            }else{
                carregarTarefas(listaId);
            }

        }
    }

    window.excluirTarefa = function (tarefaId, list) {
        fetch(`/tarefas/${tarefaId}`, { method: 'DELETE' })
            .then(() => {
                tarefas = tarefas.filter(t => t.id !== tarefaId);
                renderizarTarefas(list);
            });
    };

    window.formatarData = function (dataString) {
        const data = new Date(dataString + 'T00:00:00');
        return data.toLocaleDateString('pt-BR');
    }

    // ===== SISTEMA DE CHECKLIST (subatividades marcáveis de uma tarefa) =====
    let itensChecklistAtual = [];
    let itemChecklistParaRemover = null;

    // Elementos do DOM
    const btnChecklist = document.getElementById('btnChecklist');
    const modalGerenciarChecklist = document.getElementById('modalGerenciarChecklist');
    const modalRemoverChecklist = document.getElementById('modalRemoverChecklist');
    const listaChecklistItensEl = document.getElementById('listaChecklistItens');
    const checklistProgressoEl = document.getElementById('checklistProgresso');
    const btnFecharGerenciar = document.getElementById('btnFecharGerenciar');
    const novoItemChecklistInput = document.getElementById('novoItemChecklistInput');
    const btnAdicionarItemChecklist = document.getElementById('btnAdicionarItemChecklist');
    const btnCancelarRemoverChecklist = document.getElementById('btnCancelarRemoverChecklist');
    const btnConfirmarRemoverChecklist = document.getElementById('btnConfirmarRemoverChecklist');

    function recarregarListaAtual() {
        if (listaAtual == "hoje") {
            carregarTarefasHoje();
        } else if (listaAtual == "agendadas") {
            carregarTarefasAgendadas();
        } else if (listaAtual == "todas") {
            carregarTarefasTodas();
        } else {
            carregarTarefas(listaAtual);
        }
    }

    // Reflete a contagem de itens concluídos no card/badge da tarefa
    function atualizarBadgeChecklistTarefa() {
        if (!tarefaSelecionada) return;
        const total = itensChecklistAtual.length;
        const concluidos = itensChecklistAtual.filter(i => i.concluido).length;
        tarefaSelecionada.checklistTotal = total;
        tarefaSelecionada.checklistConcluidos = concluidos;
        const tarefaNaLista = tarefas.find(t => t.id === tarefaSelecionada.id);
        if (tarefaNaLista) {
            tarefaNaLista.checklistTotal = total;
            tarefaNaLista.checklistConcluidos = concluidos;
        }
        recarregarListaAtual();
    }

    // Abrir checklist da tarefa selecionada
    btnChecklist.addEventListener('click', async function() {
        if (!tarefaSelecionada) {
            alert('Salve a tarefa antes de adicionar um checklist.');
            return;
        }

        const resposta = await fetch(`/tarefas/${tarefaSelecionada.id}/checklist`);
        itensChecklistAtual = resposta.ok ? await resposta.json() : [];

        renderizarChecklistItens();
        modalGerenciarChecklist.style.display = 'flex';
        setTimeout(() => lucide.createIcons(), 10);
    });

    // Fechar modal
    btnFecharGerenciar.addEventListener('click', function() {
        modalGerenciarChecklist.style.display = 'none';
    });

    modalGerenciarChecklist.addEventListener('click', function(e) {
        if (e.target === modalGerenciarChecklist) {
            modalGerenciarChecklist.style.display = 'none';
        }
    });

    // Adicionar item
    async function adicionarItemChecklist() {
        const descricao = novoItemChecklistInput.value.trim();
        if (!descricao || !tarefaSelecionada) return;

        const resposta = await fetch(`/tarefas/${tarefaSelecionada.id}/checklist/itens`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ descricao })
        });

        if (!resposta.ok) {
            alert('Erro ao adicionar o item.');
            return;
        }

        itensChecklistAtual.push(await resposta.json());
        novoItemChecklistInput.value = '';
        renderizarChecklistItens();
        atualizarBadgeChecklistTarefa();
    }

    btnAdicionarItemChecklist.addEventListener('click', adicionarItemChecklist);
    novoItemChecklistInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            adicionarItemChecklist();
        }
    });

    // Marcar/desmarcar item
    async function alternarItemChecklist(item, concluido) {
        const resposta = await fetch(`/checklists/itens/${item.id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ concluido })
        });

        if (!resposta.ok) {
            alert('Erro ao atualizar o item.');
            return;
        }

        item.concluido = concluido;
        renderizarChecklistItens();
        atualizarBadgeChecklistTarefa();
    }

    // Renomear item
    async function renomearItemChecklist(item, descricao) {
        if (!descricao || descricao === item.descricao) {
            renderizarChecklistItens();
            return;
        }

        const resposta = await fetch(`/checklists/itens/${item.id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ descricao })
        });

        if (!resposta.ok) {
            alert('Erro ao renomear o item.');
            return;
        }

        item.descricao = descricao;
        renderizarChecklistItens();
    }

    // Cancelar remoção de item
    btnCancelarRemoverChecklist.addEventListener('click', function() {
        modalRemoverChecklist.style.display = 'none';
        itemChecklistParaRemover = null;
    });

    modalRemoverChecklist.addEventListener('click', function(e) {
        if (e.target === modalRemoverChecklist) {
            modalRemoverChecklist.style.display = 'none';
            itemChecklistParaRemover = null;
        }
    });

    // Confirmar remoção de item
    btnConfirmarRemoverChecklist.addEventListener('click', async function() {
        if (!itemChecklistParaRemover) return;

        await fetch(`/checklists/itens/${itemChecklistParaRemover.id}`, { method: 'DELETE' });
        itensChecklistAtual = itensChecklistAtual.filter(i => i.id !== itemChecklistParaRemover.id);
        modalRemoverChecklist.style.display = 'none';
        itemChecklistParaRemover = null;
        renderizarChecklistItens();
        atualizarBadgeChecklistTarefa();
    });

    // Renderizar itens da checklist
    window.renderizarChecklistItens = function () {
        listaChecklistItensEl.innerHTML = '';

        const total = itensChecklistAtual.length;
        const concluidos = itensChecklistAtual.filter(i => i.concluido).length;
        checklistProgressoEl.textContent = total > 0 ? `${concluidos}/${total} concluídos` : '';

        if (itensChecklistAtual.length === 0) {
            listaChecklistItensEl.innerHTML = `
                <div class="lista-checklists-vazia">
                    <i data-lucide="list-checks"></i>
                    <p>Nenhuma subatividade adicionada</p>
                    <span>Adicione itens usando o campo abaixo</span>
                </div>
            `;
            setTimeout(() => lucide.createIcons(), 10);
            return;
        }

        itensChecklistAtual.forEach(item => {
            const linha = document.createElement('div');
            linha.className = 'checklist-item';

            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.className = 'checklist-item-checkbox';
            checkbox.checked = !!item.concluido;
            checkbox.addEventListener('change', function() {
                alternarItemChecklist(item, checkbox.checked);
            });

            const info = document.createElement('div');
            info.className = 'checklist-info';

            const nome = document.createElement('div');
            nome.className = 'checklist-nome' + (item.concluido ? ' concluido' : '');
            nome.textContent = item.descricao;
            nome.title = 'Clique para editar';
            nome.addEventListener('click', function() {
                const input = document.createElement('input');
                input.type = 'text';
                input.className = 'checklist-item-input';
                input.value = item.descricao;

                const salvar = () => renomearItemChecklist(item, input.value.trim());
                input.addEventListener('blur', salvar);
                input.addEventListener('keydown', function(e) {
                    if (e.key === 'Enter') { e.preventDefault(); input.blur(); }
                    if (e.key === 'Escape') { e.preventDefault(); renderizarChecklistItens(); }
                });

                info.replaceChild(input, nome);
                input.focus();
                input.select();
            });
            info.appendChild(nome);

            const acoes = document.createElement('div');
            acoes.className = 'checklist-acoes';

            const btnExcluirItem = document.createElement('button');
            btnExcluirItem.className = 'btn-excluir-checklist';
            btnExcluirItem.innerHTML = '<i data-lucide="trash-2"></i>';
            btnExcluirItem.addEventListener('click', function() {
                itemChecklistParaRemover = item;
                modalRemoverChecklist.style.display = 'flex';
            });
            acoes.appendChild(btnExcluirItem);

            linha.appendChild(checkbox);
            linha.appendChild(info);
            linha.appendChild(acoes);
            listaChecklistItensEl.appendChild(linha);
        });

        setTimeout(() => lucide.createIcons(), 10);
    }

    // ===== SISTEMA DE CATEGORIAS =====
    let categoriaSelecionada = null;
    let corSelecionadaCategoria = '#4caf50';

    // Elementos do DOM
    const btnCategoria = document.getElementById('btnCategoria');
    const modalGerenciarCategoria = document.getElementById('modalGerenciarCategoria');
    const modalAddCategoria = document.getElementById('modalAddCategoria');
    const modalRemoverCategoria = document.getElementById('modalRemoverCategoria');
    const listaCategoriasEl = document.getElementById('listaCategorias');
    const btnFecharGerenciarCategoria = document.getElementById('btnFecharGerenciarCategoria');
    const btnNovaCategoria = document.getElementById('btnNovaCategoria');
    const btnCancelarCategoria = document.getElementById('btnCancelarCategoria');
    const btnSalvarCategoria = document.getElementById('btnSalvarCategoria');
    const btnAbrirPaletaCategoria = document.getElementById('btnAbrirPaletaCategoria');
    const paletaCoresCategoria = document.getElementById('paletaCoresCategoria');
    const corPreviewCategoria = document.getElementById('corPreviewCategoria');
    const categoriaNomeInput = document.getElementById('categoriaNome');
    const tituloModalCategoria = document.getElementById('tituloModalCategoria');
    const btnCancelarRemoverCategoria = document.getElementById('btnCancelarRemoverCategoria');
    const btnConfirmarRemoverCategoria = document.getElementById('btnConfirmarRemoverCategoria');
    let categoriaParaRemover = null;

    // Aplica destaque no botão da tarefa quando há categorias selecionadas
    window.atualizarVisualBotaoCategoria = function () {
        if (categoriaIdsSelecionadosTarefa.length > 0) {
            const primeira = categorias.find(c => c.id === categoriaIdsSelecionadosTarefa[0]);
            if (primeira) {
                btnCategoria.style.borderColor = primeira.cor;
                btnCategoria.style.backgroundColor = primeira.cor + '33';
                return;
            }
        }
        btnCategoria.style.borderColor = '';
        btnCategoria.style.backgroundColor = '';
    }

    // Abrir modal de gerenciar/atribuir categorias
    btnCategoria.addEventListener('click', async function() {
        if (!window.areaAtualId) {
            alert('Abra uma área de trabalho para gerenciar categorias.');
            return;
        }
        await carregarCategorias();
        modalGerenciarCategoria.style.display = 'flex';
        renderizarCategorias();
        setTimeout(() => lucide.createIcons(), 10);
    });

    // Fechar modal de gerenciar
    btnFecharGerenciarCategoria.addEventListener('click', function() {
        modalGerenciarCategoria.style.display = 'none';
    });

    // Abrir modal de nova categoria
    btnNovaCategoria.addEventListener('click', function() {
        categoriaSelecionada = null;
        corSelecionadaCategoria = '#4caf50';
        tituloModalCategoria.textContent = 'Nova Categoria';
        categoriaNomeInput.value = '';
        corPreviewCategoria.style.backgroundColor = corSelecionadaCategoria;
        modalAddCategoria.style.display = 'flex';
    });

    // Cancelar adicionar/editar categoria
    btnCancelarCategoria.addEventListener('click', function() {
        modalAddCategoria.style.display = 'none';
        categoriaNomeInput.value = '';
    });

    // Salvar categoria
    btnSalvarCategoria.addEventListener('click', async function() {
        const nome = categoriaNomeInput.value.trim();

        if (!nome) {
            alert('Por favor, digite um nome para a categoria');
            return;
        }

        let resposta;
        if (categoriaSelecionada) {
            resposta = await fetch(`/categorias/${categoriaSelecionada.id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: new URLSearchParams({ nome, cor: corSelecionadaCategoria })
            });
        } else {
            resposta = await fetch(`/categorias`, {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: new URLSearchParams({ areaId: window.areaAtualId, nome, cor: corSelecionadaCategoria })
            });
        }

        if (!resposta.ok) {
            alert('Erro ao salvar a categoria.');
            return;
        }

        await carregarCategorias();
        atualizarVisualBotaoCategoria();
        modalAddCategoria.style.display = 'none';
        renderizarCategorias();
        categoriaNomeInput.value = '';
    });

    // Abrir/Fechar paleta de cores
    btnAbrirPaletaCategoria.addEventListener('click', function(e) {
        e.stopPropagation();
        paletaCoresCategoria.style.display = paletaCoresCategoria.style.display === 'none' ? 'grid' : 'none';
    });

    // Selecionar cor
    document.querySelectorAll('#paletaCoresCategoria .cor-opcao').forEach(btn => {
        btn.addEventListener('click', function() {
            corSelecionadaCategoria = this.dataset.cor;
            corPreviewCategoria.style.backgroundColor = corSelecionadaCategoria;

            document.querySelectorAll('#paletaCoresCategoria .cor-opcao').forEach(b => b.classList.remove('selecionada'));
            this.classList.add('selecionada');

            paletaCoresCategoria.style.display = 'none';
        });
    });

    // Fechar paleta ao clicar fora
    document.addEventListener('click', function(e) {
        if (!e.target.closest('.seletor-cor')) {
            paletaCoresCategoria.style.display = 'none';
        }
    });

    // Fechar modais ao clicar no overlay
    modalGerenciarCategoria.addEventListener('click', function(e) {
        if (e.target === modalGerenciarCategoria) {
            modalGerenciarCategoria.style.display = 'none';
        }
    });

    modalAddCategoria.addEventListener('click', function(e) {
        if (e.target === modalAddCategoria) {
            modalAddCategoria.style.display = 'none';
            categoriaNomeInput.value = '';
        }
    });

    modalRemoverCategoria.addEventListener('click', function(e) {
        if (e.target === modalRemoverCategoria) {
            modalRemoverCategoria.style.display = 'none';
            categoriaParaRemover = null;
        }
    });

    // Cancelar remoção
    btnCancelarRemoverCategoria.addEventListener('click', function() {
        modalRemoverCategoria.style.display = 'none';
        categoriaParaRemover = null;
    });

    // Confirmar remoção
    btnConfirmarRemoverCategoria.addEventListener('click', async function() {
        if (categoriaParaRemover) {
            await fetch(`/categorias/${categoriaParaRemover.id}`, { method: 'DELETE' });
            categoriaIdsSelecionadosTarefa = categoriaIdsSelecionadosTarefa.filter(id => id !== categoriaParaRemover.id);
            await carregarCategorias();
            atualizarVisualBotaoCategoria();
            modalRemoverCategoria.style.display = 'none';
            renderizarCategorias();
            categoriaParaRemover = null;
        }
    });

    // Renderizar lista de categorias (com seleção múltipla para a tarefa)
    window.renderizarCategorias = function () {
        listaCategoriasEl.innerHTML = '';

        if (categorias.length === 0) {
            listaCategoriasEl.innerHTML = `
                <div class="lista-checklists-vazia">
                    <i data-lucide="tag"></i>
                    <p>Nenhuma categoria criada</p>
                    <span>Clique em "Nova Categoria" para começar</span>
                </div>
            `;
            setTimeout(() => lucide.createIcons(), 10);
            return;
        }

        categorias.forEach(categoria => {
            const marcada = categoriaIdsSelecionadosTarefa.includes(categoria.id);

            const item = document.createElement('div');
            item.className = 'checklist-item';
            item.innerHTML = `
                <input type="checkbox" class="categoria-checkbox" ${marcada ? 'checked' : ''} />
                <div class="checklist-cor" style="background-color: ${categoria.cor};"></div>
                <div class="checklist-info">
                    <div class="checklist-nome">${categoria.nome}</div>
                </div>
                <div class="checklist-acoes">
                    <button class="btn-editar-checklist" data-id="${categoria.id}">
                        <i data-lucide="edit-2"></i>
                    </button>
                    <button class="btn-excluir-checklist" data-id="${categoria.id}">
                        <i data-lucide="trash-2"></i>
                    </button>
                </div>
            `;

            // Editar categoria
            item.querySelector('.btn-editar-checklist').addEventListener('click', function(e) {
                e.stopPropagation();
                categoriaSelecionada = categoria;
                tituloModalCategoria.textContent = 'Editar Categoria';
                categoriaNomeInput.value = categoria.nome;
                corSelecionadaCategoria = categoria.cor;
                corPreviewCategoria.style.backgroundColor = corSelecionadaCategoria;
                modalAddCategoria.style.display = 'flex';
            });

            // Remover categoria
            item.querySelector('.btn-excluir-checklist').addEventListener('click', function(e) {
                e.stopPropagation();
                categoriaParaRemover = categoria;
                modalRemoverCategoria.style.display = 'flex';
            });

            // Alterna a categoria na tarefa (permite selecionar mais de uma)
            item.addEventListener('click', function(e) {
                if (e.target.closest('.btn-editar-checklist') || e.target.closest('.btn-excluir-checklist')) return;

                const idx = categoriaIdsSelecionadosTarefa.indexOf(categoria.id);
                if (idx === -1) {
                    categoriaIdsSelecionadosTarefa.push(categoria.id);
                } else {
                    categoriaIdsSelecionadosTarefa.splice(idx, 1);
                }
                item.querySelector('.categoria-checkbox').checked = categoriaIdsSelecionadosTarefa.includes(categoria.id);
                atualizarVisualBotaoCategoria();
            });

            listaCategoriasEl.appendChild(item);
        });

        setTimeout(() => lucide.createIcons(), 10);
    }

    lucide.createIcons();
});