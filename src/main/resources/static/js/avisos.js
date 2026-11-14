// Avisos e confirmações no padrão visual do projeto (mesmo estilo dos avisos de login e cadastro).
// Substituem alert() e confirm() do navegador, que aparecem como "localhost:8080 diz".
//
//   Aviso.mostrar('Link copiado!', 'sucesso');        // 'erro' (padrão) | 'sucesso' | 'alerta'
//   if (await Aviso.confirmar({ mensagem: 'Remover?', perigo: true })) { ... }
(function () {
    let avisoAtual = null;
    let temporizador = null;

    function fecharAviso() {
        clearTimeout(temporizador);
        if (avisoAtual) {
            avisoAtual.remove();
            avisoAtual = null;
        }
    }

    // Mostra um aviso flutuante no topo da tela. Só um por vez; some sozinho ou ao clicar.
    // duracao: tempo em ms até sumir (padrão 4,5 s); 0 mantém até o usuário clicar.
    function mostrar(texto, tipo, duracao) {
        fecharAviso();

        const aviso = document.createElement('p');
        aviso.className = 'aviso ' + (tipo || 'erro') + ' aviso-flutuante';
        aviso.setAttribute('role', tipo === 'sucesso' ? 'status' : 'alert');
        aviso.textContent = texto;
        aviso.addEventListener('click', fecharAviso);
        document.body.appendChild(aviso);
        avisoAtual = aviso;

        const tempo = duracao === undefined ? 4500 : duracao;
        if (tempo > 0) {
            temporizador = setTimeout(fecharAviso, tempo);
        }
    }

    // Pede confirmação em um modal. Retorna uma Promise<boolean> (true = confirmou).
    // opcoes: { titulo, mensagem, confirmar, cancelar, perigo }
    function confirmar(opcoes) {
        const config = opcoes || {};

        return new Promise(function (resolve) {
            const overlay = document.createElement('div');
            overlay.className = 'aviso-overlay';

            const caixa = document.createElement('div');
            caixa.className = 'aviso-caixa' + (config.perigo ? ' perigo' : '');
            caixa.setAttribute('role', 'dialog');
            caixa.setAttribute('aria-modal', 'true');

            const titulo = document.createElement('h3');
            titulo.className = 'aviso-titulo';
            titulo.textContent = config.titulo || 'Confirmar';

            const texto = document.createElement('p');
            texto.className = 'aviso-texto';
            texto.textContent = config.mensagem || '';

            const botoes = document.createElement('div');
            botoes.className = 'aviso-botoes';

            const btnCancelar = document.createElement('button');
            btnCancelar.type = 'button';
            btnCancelar.className = 'aviso-btn cancelar';
            btnCancelar.textContent = config.cancelar || 'Cancelar';

            const btnConfirmar = document.createElement('button');
            btnConfirmar.type = 'button';
            btnConfirmar.className = 'aviso-btn confirmar' + (config.perigo ? ' perigo' : '');
            btnConfirmar.textContent = config.confirmar || 'Confirmar';

            botoes.appendChild(btnCancelar);
            botoes.appendChild(btnConfirmar);
            caixa.appendChild(titulo);
            caixa.appendChild(texto);
            caixa.appendChild(botoes);
            overlay.appendChild(caixa);

            function responder(resposta) {
                document.removeEventListener('keydown', aoTeclar, true);
                overlay.remove();
                resolve(resposta);
            }

            function aoTeclar(evento) {
                if (evento.key === 'Escape') {
                    evento.stopPropagation();
                    responder(false);
                }
            }

            btnCancelar.addEventListener('click', function () { responder(false); });
            btnConfirmar.addEventListener('click', function () { responder(true); });
            overlay.addEventListener('click', function (evento) {
                if (evento.target === overlay) responder(false);
            });
            document.addEventListener('keydown', aoTeclar, true);

            document.body.appendChild(overlay);
            // Em ações destrutivas o foco começa em "Cancelar", para um Enter sem querer não apagar nada.
            (config.perigo ? btnCancelar : btnConfirmar).focus();
        });
    }

    /**
     * Deixa o botão apagado (com o símbolo de bloqueado e a mensagem ao passar o mouse)
     * enquanto o campo estiver vazio. Para limpar/preencher o campo por código, dispare
     * input.dispatchEvent(new Event('input')) para o botão acompanhar.
     */
    function exigirTexto(campo, botao, dica) {
        if (!campo || !botao) return;
        var tituloNormal = botao.getAttribute('title');

        function sincronizar() {
            var vazio = campo.value.trim() === '';
            botao.disabled = vazio;
            if (vazio) {
                botao.title = dica;
            } else if (tituloNormal) {
                botao.title = tituloNormal;
            } else {
                botao.removeAttribute('title');
            }
        }

        campo.addEventListener('input', sincronizar);
        sincronizar();
    }

    window.Aviso = { mostrar: mostrar, confirmar: confirmar, exigirTexto: exigirTexto };
})();
