// Campo de data de nascimento digitado no formato do idioma da tela:
// português e espanhol DD/MM/AAAA, inglês MM/DD/YYYY. O <input type="date"> do navegador não serve para isso,
// porque ele mostra a data no formato do navegador (não do idioma escolhido no ToDaily).
//
// A página tem três campos juntos:
//   texto    <input type="text">   o que a pessoa digita (com as barras colocadas sozinhas)
//   iso      <input type="hidden"> a data em AAAA-MM-DD (é o que vai para o servidor); vazio se a data é inválida
//   seletor  <input type="date">   escondido, só para abrir o calendário do navegador pelo botão
(function () {
    const CHAVE_IDIOMA = 'todaily.idioma.dataNascimento';

    // 'dma' (dia, mês, ano) ou 'mda' (mês, dia, ano)
    function ordem() {
        return t('js.data.ordem') === 'mda' ? 'mda' : 'dma';
    }

    // "05/04/2002" (no idioma da tela) -> "2002-04-05"; texto incompleto ou data que não existe -> ""
    function paraIso(texto) {
        const digitos = texto.replace(/\D/g, '');
        if (digitos.length !== 8) return '';
        const primeiro = Number(digitos.slice(0, 2));
        const segundo = Number(digitos.slice(2, 4));
        const ano = Number(digitos.slice(4, 8));
        const dia = ordem() === 'mda' ? segundo : primeiro;
        const mes = ordem() === 'mda' ? primeiro : segundo;
        const data = new Date(ano, mes - 1, dia);
        if (ano < 1000 || data.getFullYear() !== ano || data.getMonth() !== mes - 1 || data.getDate() !== dia) return '';
        return String(ano) + '-' + String(mes).padStart(2, '0') + '-' + String(dia).padStart(2, '0');
    }

    // "2002-04-05" -> "05/04/2002" ou "04/05/2002", conforme o idioma
    function deIso(iso) {
        const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso || '');
        if (!m) return '';
        return ordem() === 'mda' ? m[2] + '/' + m[3] + '/' + m[1] : m[3] + '/' + m[2] + '/' + m[1];
    }

    // Deixa só os números e coloca as barras: 05042002 -> 05/04/2002
    function mascarar(digitos) {
        const d = digitos.slice(0, 8);
        if (d.length <= 2) return d;
        if (d.length <= 4) return d.slice(0, 2) + '/' + d.slice(2);
        return d.slice(0, 2) + '/' + d.slice(2, 4) + '/' + d.slice(4);
    }

    function iniciar(campos) {
        const { texto, iso, seletor, botao } = campos;
        texto.placeholder = t('js.data.formato');
        if (botao) botao.title = botao.getAttribute('aria-label') || t('js.data.abrir');

        // Data guardada antes de trocar o idioma (ou, na edição de conta, a que veio do servidor)
        let inicial = iso.value;
        try {
            const guardada = sessionStorage.getItem(CHAVE_IDIOMA);
            if (guardada !== null) {
                sessionStorage.removeItem(CHAVE_IDIOMA);
                inicial = guardada;
            }
        } catch (e) { /* sem armazenamento: usa o que a página trouxe */ }
        if (inicial) {
            texto.value = deIso(inicial);
            iso.value = paraIso(texto.value);
        }

        texto.addEventListener('input', function () {
            let valor = texto.value.trim();
            // Colou uma data no formato AAAA-MM-DD (o que alguns navegadores e gerenciadores de senha usam)
            if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) valor = deIso(valor);
            texto.value = mascarar(valor.replace(/\D/g, ''));
            iso.value = paraIso(texto.value);
        });

        if (seletor && botao) {
            seletor.min = iso.min || '1900-01-01';
            if (iso.max) seletor.max = iso.max;
            botao.addEventListener('click', function () {
                seletor.value = iso.value;
                try { seletor.showPicker(); } catch (e) { seletor.focus(); }
            });
            seletor.addEventListener('change', function () {
                if (!seletor.value) return;
                texto.value = deIso(seletor.value);
                texto.dispatchEvent(new Event('input', { bubbles: true }));
                texto.focus();
            });
        }

        // A troca de idioma recarrega a página: guarda a data em AAAA-MM-DD e esvazia o campo digitado,
        // para a restauração genérica dos campos não recolocar "05/04/2002" num idioma que lê como mês/dia.
        const trocarOriginal = window.trocarIdioma;
        if (typeof trocarOriginal === 'function') {
            window.trocarIdioma = function (lang) {
                try {
                    sessionStorage.setItem(CHAVE_IDIOMA, iso.value);
                    texto.value = '';
                } catch (e) { /* sem armazenamento: o campo só volta vazio */ }
                return trocarOriginal(lang);
            };
        }
    }

    window.DataNascimento = { iniciar, paraIso, deIso };
})();
