/**
 * Menu de acessibilidade (botão no canto inferior direito, em todas as telas).
 * As escolhas ficam guardadas no navegador (localStorage) e valem para todas as páginas.
 *
 * Este script é carregado no <head> (junto com acessibilidade.css e acessibilidade-temas.css): as
 * preferências são aplicadas antes de a página aparecer (sem piscar) e o botão/painel são criados
 * quando o corpo da página já existe.
 *
 *   texto      0 a 3   tamanho do texto (zoom de 100%, 115%, 130% e 150%)
 *   tema       'escuro' | 'claro' | 'sistema'
 *   contraste  alto contraste (fundo preto, texto e bordas brancos)
 *   semAnim    reduzir animações (null = segue a preferência do sistema operacional)
 *   foco       destaque forte no item selecionado pelo teclado
 */
(function () {
    'use strict';

    var CHAVE = 'todaily.acessibilidade';
    var ZOOMS = [1, 1.15, 1.3, 1.5];
    var PADRAO = { texto: 0, tema: 'escuro', contraste: false, semAnim: null, foco: false };

    function ler() {
        var salvo = {};
        try { salvo = JSON.parse(localStorage.getItem(CHAVE)) || {}; } catch (e) { salvo = {}; }
        var p = {};
        for (var k in PADRAO) p[k] = (k in salvo) ? salvo[k] : PADRAO[k];
        if (!(p.texto >= 0 && p.texto < ZOOMS.length)) p.texto = 0;
        if (['escuro', 'claro', 'sistema'].indexOf(p.tema) < 0) p.tema = 'escuro';
        return p;
    }

    function gravar(p) {
        try { localStorage.setItem(CHAVE, JSON.stringify(p)); } catch (e) { /* sem armazenamento: vale só nesta página */ }
    }

    var prefs = ler();
    var consultaClaro = window.matchMedia ? window.matchMedia('(prefers-color-scheme: light)') : null;
    var consultaMovimento = window.matchMedia ? window.matchMedia('(prefers-reduced-motion: reduce)') : null;

    function temaEfetivoClaro() {
        if (prefs.contraste) return false; // o alto contraste é feito sobre o tema escuro
        if (prefs.tema === 'claro') return true;
        if (prefs.tema === 'sistema') return !!(consultaClaro && consultaClaro.matches);
        return false;
    }

    function semAnimacao() {
        if (prefs.semAnim === null) return !!(consultaMovimento && consultaMovimento.matches);
        return !!prefs.semAnim;
    }

    function aplicar() {
        var html = document.documentElement;
        var zoom = ZOOMS[prefs.texto];
        html.classList.toggle('acess-zoom', zoom !== 1);
        html.style.zoom = zoom === 1 ? '' : String(zoom);
        html.style.setProperty('--az', String(zoom));
        html.classList.toggle('tema-claro', temaEfetivoClaro());
        html.classList.toggle('acess-contraste', !!prefs.contraste);
        html.classList.toggle('acess-sem-animacao', semAnimacao());
        html.classList.toggle('acess-foco', !!prefs.foco);
        atualizarPainel();
    }

    function mudar(parte) {
        for (var k in parte) prefs[k] = parte[k];
        gravar(prefs);
        aplicar();
    }

    aplicar(); // já na primeira pintura

    if (consultaClaro && consultaClaro.addEventListener) consultaClaro.addEventListener('change', aplicar);
    if (consultaMovimento && consultaMovimento.addEventListener) consultaMovimento.addEventListener('change', aplicar);
    // Com "reduzir animações", a troca de página (esmaecimento do navegador) também é pulada
    function pularTransicao(e) {
        if (e.viewTransition && semAnimacao()) {
            try { e.viewTransition.skipTransition(); } catch (erro) { /* navegador sem suporte */ }
        }
    }
    window.addEventListener('pageswap', pularTransicao);
    window.addEventListener('pagereveal', pularTransicao);

    // Mudança feita em outra aba/janela
    window.addEventListener('storage', function (e) {
        if (e.key === CHAVE) { prefs = ler(); aplicar(); }
    });

    // ===== Botão e painel =====
    var botao, painel;

    var ICONE = '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" ' +
        'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/>' +
        '<circle cx="12" cy="7.5" r="1"/><path d="M7.5 10.5l4.5.8 4.5-.8"/><path d="M12 11.3v3.2"/>' +
        '<path d="M9.5 18l2.5-3.5 2.5 3.5"/></svg>';

    function el(tag, classe, texto) {
        var e = document.createElement(tag);
        if (classe) e.className = classe;
        if (texto) e.textContent = texto;
        return e;
    }

    function linhaInterruptor(rotulo, descricao, id, aoMudar) {
        var linha = el('label', 'acess-linha');
        var t = el('span', 'acess-linha-texto');
        t.appendChild(el('span', 'acess-linha-rotulo', rotulo));
        if (descricao) t.appendChild(el('span', 'acess-linha-desc', descricao));
        var caixa = document.createElement('input');
        caixa.type = 'checkbox';
        caixa.className = 'acess-interruptor';
        caixa.id = id;
        caixa.addEventListener('change', function () { aoMudar(caixa.checked); });
        linha.append(t, caixa);
        return linha;
    }

    function criarPainel() {
        botao = el('button', 'acess-botao');
        botao.type = 'button';
        botao.id = 'acessBotao';
        botao.title = t('js.acess.titulo');
        botao.setAttribute('aria-label', t('js.acess.abrir'));
        botao.setAttribute('aria-haspopup', 'dialog');
        botao.setAttribute('aria-expanded', 'false');
        botao.innerHTML = ICONE;

        painel = el('div', 'acess-painel');
        painel.id = 'acessPainel';
        painel.setAttribute('role', 'dialog');
        painel.setAttribute('aria-label', t('js.acess.titulo'));
        painel.hidden = true;

        painel.appendChild(el('h2', 'acess-titulo', t('js.acess.titulo')));

        // Tamanho do texto
        var grupoTexto = el('div', 'acess-grupo');
        grupoTexto.appendChild(el('span', 'acess-grupo-rotulo', t('js.acess.tamanhoTexto')));
        var controle = el('div', 'acess-texto-controle');
        var menos = el('button', 'acess-btn', 'A−');
        menos.type = 'button'; menos.id = 'acessTextoMenos'; menos.setAttribute('aria-label', t('js.acess.diminuir'));
        var valor = el('span', 'acess-texto-valor'); valor.id = 'acessTextoValor'; valor.setAttribute('aria-live', 'polite');
        var mais = el('button', 'acess-btn', 'A+');
        mais.type = 'button'; mais.id = 'acessTextoMais'; mais.setAttribute('aria-label', t('js.acess.aumentar'));
        menos.addEventListener('click', function () { if (prefs.texto > 0) mudar({ texto: prefs.texto - 1 }); });
        mais.addEventListener('click', function () { if (prefs.texto < ZOOMS.length - 1) mudar({ texto: prefs.texto + 1 }); });
        controle.append(menos, valor, mais);
        grupoTexto.appendChild(controle);
        var limite = el('p', 'acess-limite');
        limite.id = 'acessTextoLimite';
        limite.hidden = true;
        grupoTexto.appendChild(limite);
        painel.appendChild(grupoTexto);

        // Tema
        var grupoTema = el('div', 'acess-grupo');
        grupoTema.appendChild(el('span', 'acess-grupo-rotulo', t('js.acess.tema')));
        var seg = el('div', 'acess-segmentos');
        seg.setAttribute('role', 'group');
        seg.setAttribute('aria-label', t('js.acess.tema'));
        [['escuro', t('js.acess.escuro')], ['claro', t('js.acess.claro')], ['sistema', t('js.acess.sistema')]].forEach(function (op) {
            var b = el('button', 'acess-segmento', op[1]);
            b.type = 'button';
            b.dataset.tema = op[0];
            b.addEventListener('click', function () { mudar({ tema: op[0] }); });
            seg.appendChild(b);
        });
        grupoTema.appendChild(seg);
        var nota = el('span', 'acess-linha-desc', t('js.acess.notaTema'));
        nota.id = 'acessNotaTema';
        nota.hidden = true;
        grupoTema.appendChild(nota);
        painel.appendChild(grupoTema);

        painel.appendChild(linhaInterruptor(t('js.acess.contraste'), t('js.acess.contrasteDesc'), 'acessContraste',
            function (v) { mudar({ contraste: v }); }));
        painel.appendChild(linhaInterruptor(t('js.acess.semAnim'), t('js.acess.semAnimDesc'), 'acessSemAnim',
            function (v) { mudar({ semAnim: v }); }));
        painel.appendChild(linhaInterruptor(t('js.acess.foco'), t('js.acess.focoDesc'), 'acessFoco',
            function (v) { mudar({ foco: v }); }));

        var restaurar = el('button', 'acess-restaurar', t('js.acess.restaurar'));
        restaurar.type = 'button';
        restaurar.id = 'acessRestaurar';
        restaurar.addEventListener('click', function () {
            prefs = {};
            for (var k in PADRAO) prefs[k] = PADRAO[k];
            gravar(prefs);
            aplicar();
        });
        painel.appendChild(restaurar);

        botao.addEventListener('click', function (e) { e.stopPropagation(); alternar(); });
        painel.addEventListener('click', function (e) { e.stopPropagation(); });
        document.addEventListener('click', function () { alternar(false); });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && !painel.hidden) {
                alternar(false);
                botao.focus();
            }
        });

        document.body.append(botao, painel);
        atualizarPainel();
    }

    function alternar(abrir) {
        if (!painel) return;
        var mostrar = abrir === undefined ? painel.hidden : abrir;
        painel.hidden = !mostrar;
        botao.setAttribute('aria-expanded', String(mostrar));
        if (mostrar) {
            var primeiro = painel.querySelector('button, input');
            if (primeiro) primeiro.focus({ preventScroll: true });
        }
    }

    function atualizarPainel() {
        if (!painel) return;
        document.getElementById('acessTextoValor').textContent = Math.round(ZOOMS[prefs.texto] * 100) + '%';
        var noMinimo = prefs.texto === 0, noMaximo = prefs.texto === ZOOMS.length - 1;
        var bMenos = document.getElementById('acessTextoMenos'), bMais = document.getElementById('acessTextoMais');
        bMenos.disabled = noMinimo;
        bMais.disabled = noMaximo;
        bMenos.title = noMinimo ? t('js.acess.minimoDica') : t('js.acess.diminuir');
        bMais.title = noMaximo ? t('js.acess.maximoDica', Math.round(ZOOMS[ZOOMS.length - 1] * 100)) : t('js.acess.aumentar');
        var limite = document.getElementById('acessTextoLimite');
        limite.hidden = !(noMinimo || noMaximo);
        limite.textContent = noMinimo ? t('js.acess.minimoMsg') : (noMaximo ? t('js.acess.maximoMsg') : '');
        painel.querySelectorAll('.acess-segmento').forEach(function (b) {
            var ativo = b.dataset.tema === prefs.tema;
            b.classList.toggle('ativo', ativo);
            b.setAttribute('aria-pressed', String(ativo));
        });
        document.getElementById('acessNotaTema').hidden = !prefs.contraste;
        document.getElementById('acessContraste').checked = !!prefs.contraste;
        document.getElementById('acessSemAnim').checked = semAnimacao();
        document.getElementById('acessFoco').checked = !!prefs.foco;
    }

    // O botão é criado assim que o <body> existe, para já aparecer na primeira pintura (sem "piscar" ao trocar de página)
    if (document.body) {
        criarPainelUmaVez();
    } else {
        var observador = new MutationObserver(function () {
            if (document.body) { observador.disconnect(); criarPainelUmaVez(); }
        });
        observador.observe(document.documentElement, { childList: true });
        document.addEventListener('DOMContentLoaded', criarPainelUmaVez);
    }

    function criarPainelUmaVez() {
        if (!document.getElementById('acessBotao')) criarPainel();
    }

    window.Acessibilidade = { alternar: alternar, mudar: mudar };
})();
