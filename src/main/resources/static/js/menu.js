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

// Listas
let listas = document.querySelectorAll('.lista');

async function carregarTarefas(listaId) {
    const response = await fetch(`/listas/${listaId}/tarefas`);
    if (!response.ok) return [];
    const tarefasJson = await response.json();
    tarefas = tarefasJson; // preenche a variável global

    // renderiza apenas as tarefas da lista filtrada
    renderizarTarefas(+listaId);
}

async function carregarTarefasHoje() {

    let response

    if(window.areaAtualId){
        response = await fetch(`/areasTrabalho/${window.areaAtualId}/tarefas`);
    }else{
        response = await fetch(`/user/tarefas`);
    }

    if (!response.ok) return [];
    const tarefasJson = await response.json();
    tarefas = tarefasJson; // preenche a variável global

    // renderiza apenas as tarefas da lista filtrada
    renderizarTarefas("hoje");
}

async function carregarTarefasTodas() {

    let response

    if(window.areaAtualId){
        response = await fetch(`/areasTrabalho/${window.areaAtualId}/tarefas`);
    }else{
        response = await fetch(`/user/tarefas`);
    }

    if (!response.ok) return [];
    const tarefasJson = await response.json();
    tarefas = tarefasJson; // preenche a variável global

    // renderiza apenas as tarefas da lista filtrada
    renderizarTarefas("todas");
}

async function carregarTarefasAgendadas() {

    let response

    if(window.areaAtualId){
        response = await fetch(`/areasTrabalho/${window.areaAtualId}/tarefas`);
    }else{
        response = await fetch(`/user/tarefas`);
    }

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

    // Simula click no botão hoje
    {
        document.querySelectorAll('.menu-item').forEach(i => i.classList.remove('active'));
        document.querySelectorAll('.lista').forEach(i => i.classList.remove('active'));
        btnParaHoje.classList.add('active');

        listaAtual = "hoje"
        resetFiltro()
        resetOrdem()

        carregarTarefasHoje()
        atualizarCamposModalTarefa()

        mudarTituloPrincipal(btnParaHoje)
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
        const checklistId = checklistSelecionadoTarefa || null;

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
            checklistId,
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
        checklistSelecionadoTarefa = null;
    });

    window.limparFormulario = function () {
        document.querySelectorAll("#modalAddTarefa input, #modalAddTarefa textarea, #modalAddTarefa select")
            .forEach(el => el.disabled = false);
        document.getElementById("btnAnexo").disabled = false;
        document.getElementById("btnChecklist").disabled = false;

        document.getElementById('tarefaLista').disabled = false;
        document.getElementById('tarefaLista').value = '';
        document.getElementById('tarefaTitulo').value = '';
        document.getElementById('tarefaDescricao').value = '';
        document.getElementById('tarefaDataFim').value = '';
        document.getElementById('tarefaResponsavel').value = '';
        document.getElementById('tarefaNotificacoes').checked = false;
        checklistSelecionadoTarefa = null;
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
        checklistSelecionadoTarefa = tarefa.checklistId || null;
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

        document.querySelectorAll("#modalAddTarefa input, #modalAddTarefa textarea, #modalAddTarefa select")
            .forEach(el => el.disabled = true);
        document.getElementById("btnAnexo").disabled = true;
        document.getElementById("btnChecklist").disabled = true;

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

            // Barra de cor do checklist
            let corBarra = '';
            if (tarefa.checklistId) {
                const checklist = checklists.find(c => c.id === tarefa.checklistId);
                if (checklist) {
                    corBarra = `<div class="tarefa-checklist-barra" style="background-color: ${checklist.cor};"></div>`;
                }
            }

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

            if (tarefa.dataFim || tarefa.responsavel || tarefa.checklistId) {
                const info = document.createElement('div');
                info.className = 'tarefa-info';

                if (tarefa.checklistId) {
                    const checklist = checklists.find(c => c.id === tarefa.checklistId);
                    if (checklist) {
                        const badge = document.createElement('span');
                        badge.className = 'tarefa-checklist-badge';
                        badge.style.borderLeftColor = checklist.cor;
                        badge.innerHTML = `<i data-lucide="list"></i> ${checklist.nome}`;
                        info.appendChild(badge);
                    }
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

            if (corBarra) {
                tarefaEl.innerHTML = corBarra;
            }
            tarefaEl.appendChild(checkbox);
            tarefaEl.appendChild(conteudo);
            tarefaEl.appendChild(btnEditar);
            tarefaEl.appendChild(btnExcluir);
            containerTarefas.appendChild(tarefaEl);
        });

        lucide.createIcons();
    }

    btnParaHoje.addEventListener('click', function() {
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

            titulo = tarefa.titulo
            listaId = tarefa.listaId
            concluida = tarefa.concluida
            responsavelId = tarefa.responsavel

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

    // ===== SISTEMA DE CHECKLISTS =====
    let checklists = [];
    let checklistSelecionado = null;
    let checklistSelecionadoTarefa = null;
    let corSelecionada = '#4caf50';

    // Elementos do DOM
    const btnChecklist = document.getElementById('btnChecklist');
    const modalGerenciarChecklist = document.getElementById('modalGerenciarChecklist');
    const modalAddChecklist = document.getElementById('modalAddChecklist');
    const modalRemoverChecklist = document.getElementById('modalRemoverChecklist');
    const listaChecklistsEl = document.getElementById('listaChecklists');
    const btnFecharGerenciar = document.getElementById('btnFecharGerenciar');
    const btnNovoChecklist = document.getElementById('btnNovoChecklist');
    const btnCancelarChecklist = document.getElementById('btnCancelarChecklist');
    const btnSalvarChecklist = document.getElementById('btnSalvarChecklist');
    const btnAbrirPaleta = document.getElementById('btnAbrirPaleta');
    const paletaCores = document.getElementById('paletaCores');
    const corPreview = document.getElementById('corPreview');
    const checklistNomeInput = document.getElementById('checklistNome');
    const tituloModalChecklist = document.getElementById('tituloModalChecklist');
    const btnCancelarRemoverChecklist = document.getElementById('btnCancelarRemoverChecklist');
    const btnConfirmarRemoverChecklist = document.getElementById('btnConfirmarRemoverChecklist');
    let checklistParaRemover = null;

    // Abrir modal de gerenciar checklists
    btnChecklist.addEventListener('click', function() {
        modalGerenciarChecklist.style.display = 'flex';
        renderizarChecklists();
        setTimeout(() => lucide.createIcons(), 10);
    });

    // Fechar modal de gerenciar
    btnFecharGerenciar.addEventListener('click', function() {
        modalGerenciarChecklist.style.display = 'none';
    });

    // Abrir modal de novo checklist
    btnNovoChecklist.addEventListener('click', function() {
        checklistSelecionado = null;
        corSelecionada = '#4caf50';
        tituloModalChecklist.textContent = 'Novo Checklist';
        checklistNomeInput.value = '';
        corPreview.style.backgroundColor = corSelecionada;
        modalAddChecklist.style.display = 'flex';
    });

    // Cancelar adicionar/editar checklist
    btnCancelarChecklist.addEventListener('click', function() {
        modalAddChecklist.style.display = 'none';
        checklistNomeInput.value = '';
    });

    // Salvar checklist
    btnSalvarChecklist.addEventListener('click', function() {
        const nome = checklistNomeInput.value.trim();

        if (!nome) {
            alert('Por favor, digite um nome para o checklist');
            return;
        }

        if (checklistSelecionado) {
            // Editando
            checklistSelecionado.nome = nome;
            checklistSelecionado.cor = corSelecionada;
        } else {
            // Criando novo
            const novoChecklist = {
                id: Date.now(),
                nome: nome,
                cor: corSelecionada
            };
            checklists.push(novoChecklist);
        }

        modalAddChecklist.style.display = 'none';
        renderizarChecklists();
        checklistNomeInput.value = '';
    });

    // Abrir/Fechar paleta de cores
    btnAbrirPaleta.addEventListener('click', function(e) {
        e.stopPropagation();
        paletaCores.style.display = paletaCores.style.display === 'none' ? 'grid' : 'none';
    });

    // Selecionar cor
    document.querySelectorAll('.cor-opcao').forEach(btn => {
        btn.addEventListener('click', function() {
            corSelecionada = this.dataset.cor;
            corPreview.style.backgroundColor = corSelecionada;

            // Remove classe selecionada de todos
            document.querySelectorAll('.cor-opcao').forEach(b => b.classList.remove('selecionada'));
            this.classList.add('selecionada');

            paletaCores.style.display = 'none';
        });
    });

    // Fechar paleta ao clicar fora
    document.addEventListener('click', function(e) {
        if (!e.target.closest('.seletor-cor')) {
            paletaCores.style.display = 'none';
        }
    });

    // Fechar modais ao clicar no overlay
    modalGerenciarChecklist.addEventListener('click', function(e) {
        if (e.target === modalGerenciarChecklist) {
            modalGerenciarChecklist.style.display = 'none';
        }
    });

    modalAddChecklist.addEventListener('click', function(e) {
        if (e.target === modalAddChecklist) {
            modalAddChecklist.style.display = 'none';
            checklistNomeInput.value = '';
        }
    });

    modalRemoverChecklist.addEventListener('click', function(e) {
        if (e.target === modalRemoverChecklist) {
            modalRemoverChecklist.style.display = 'none';
            checklistParaRemover = null;
        }
    });

    // Cancelar remoção
    btnCancelarRemoverChecklist.addEventListener('click', function() {
        modalRemoverChecklist.style.display = 'none';
        checklistParaRemover = null;
    });

    // Confirmar remoção
    btnConfirmarRemoverChecklist.addEventListener('click', function() {
        if (checklistParaRemover) {
            checklists = checklists.filter(c => c.id !== checklistParaRemover.id);
            modalRemoverChecklist.style.display = 'none';
            renderizarChecklists();
            checklistParaRemover = null;
        }
    });

    // Renderizar lista de checklists
    window.renderizarChecklists = function () {
        listaChecklistsEl.innerHTML = '';

        if (checklists.length === 0) {
            listaChecklistsEl.innerHTML = `
                <div class="lista-checklists-vazia">
                    <i data-lucide="list"></i>
                    <p>Nenhum checklist criado</p>
                    <span>Clique em "Novo Checklist" para começar</span>
                </div>
            `;
            setTimeout(() => lucide.createIcons(), 10);
            return;
        }

        checklists.forEach(checklist => {
            const item = document.createElement('div');
            item.className = 'checklist-item';
            item.innerHTML = `
                <div class="checklist-cor" style="background-color: ${checklist.cor};"></div>
                <div class="checklist-info">
                    <div class="checklist-nome">${checklist.nome}</div>
                </div>
                <div class="checklist-acoes">
                    <button class="btn-editar-checklist" data-id="${checklist.id}">
                        <i data-lucide="edit-2"></i>
                    </button>
                    <button class="btn-excluir-checklist" data-id="${checklist.id}">
                        <i data-lucide="trash-2"></i>
                    </button>
                </div>
            `;

            // Editar checklist
            item.querySelector('.btn-editar-checklist').addEventListener('click', function() {
                checklistSelecionado = checklist;
                tituloModalChecklist.textContent = 'Editar Checklist';
                checklistNomeInput.value = checklist.nome;
                corSelecionada = checklist.cor;
                corPreview.style.backgroundColor = corSelecionada;
                modalAddChecklist.style.display = 'flex';
            });

            // Remover checklist
            item.querySelector('.btn-excluir-checklist').addEventListener('click', function() {
                checklistParaRemover = checklist;
                modalRemoverChecklist.style.display = 'flex';
            });

            // Selecionar checklist para tarefa (clique no item)
            item.addEventListener('click', function(e) {
                if (!e.target.closest('.btn-editar-checklist') && !e.target.closest('.btn-excluir-checklist')) {
                    checklistSelecionadoTarefa = checklist.id;
                    modalGerenciarChecklist.style.display = 'none';

                    // Atualiza visual do botão de checklist
                    btnChecklist.style.borderColor = checklist.cor;
                    btnChecklist.style.backgroundColor = checklist.cor + '33';
                }
            });

            listaChecklistsEl.appendChild(item);
        });

        setTimeout(() => lucide.createIcons(), 10);
    }

    lucide.createIcons();
});