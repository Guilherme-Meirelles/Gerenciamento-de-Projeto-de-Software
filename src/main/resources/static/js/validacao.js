// Validação dos formulários de login, cadastro e recuperação de senha.
// Substitui os balões nativos do navegador por um aviso no padrão do projeto
// e destaca em vermelho os campos que precisam de atenção.
(function () {
    function marcar(campo, invalido) {
        campo.classList.toggle('campo-invalido', invalido);
    }

    function mostrarAviso(elemento, texto) {
        if (texto) {
            // só um aviso por vez (some o que veio do servidor, por exemplo)
            document.querySelectorAll('.aviso').forEach(function (outro) {
                if (outro !== elemento) outro.hidden = true;
            });
        }
        elemento.textContent = texto;
        elemento.hidden = !texto;
    }

    function emailValido(valor) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valor);
    }

    // Retorna os itens que faltam na senha (lista vazia = senha forte).
    function requisitosFaltando(senha) {
        const faltando = [];
        if (senha.length < 8) faltando.push('pelo menos 8 caracteres');
        if (!/\p{Lu}/u.test(senha)) faltando.push('uma letra maiúscula');
        if (!/\p{Ll}/u.test(senha)) faltando.push('uma letra minúscula');
        if (!/\d/.test(senha)) faltando.push('um número');
        if (!/[^\p{L}\p{N}\s]/u.test(senha)) faltando.push('um caractere especial (como ! @ # $ %)');
        return faltando;
    }

    function juntar(itens) {
        return itens.length > 1
            ? itens.slice(0, -1).join(', ') + ' e ' + itens[itens.length - 1]
            : itens[0];
    }

    // Tira a marca vermelha de um campo assim que o usuário volta a digitar nele
    function limparAoDigitar(form, aviso) {
        form.querySelectorAll('input').forEach(function (campo) {
            campo.addEventListener('input', function () {
                marcar(campo, false);
                if (aviso && !form.querySelector('.campo-invalido')) mostrarAviso(aviso, '');
            });
        });
    }

    window.Validacao = {
        marcar: marcar,
        mostrarAviso: mostrarAviso,
        emailValido: emailValido,
        requisitosFaltando: requisitosFaltando,
        juntar: juntar,
        limparAoDigitar: limparAoDigitar
    };
})();
