package com.example.demo.Controles;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A data de nascimento do perfil aparece como dd/mm/aaaa (português e espanhol) ou mm/dd/aaaa (inglês) em todas as
 * telas: menu, áreas de trabalho, edição de conta e erros.
 */
class DataNascimentoExibicaoTest {

    @Test
    void converteDeAaaaMmDdParaDdMmAaaa() {
        assertThat(MenuController.dataParaExibicao("2002-04-05")).isEqualTo("05/04/2002");
    }

    @Test
    void emInglesUsaMesDiaAno() {
        org.springframework.context.i18n.LocaleContextHolder.setLocale(java.util.Locale.ENGLISH);
        try {
            assertThat(MenuController.dataParaExibicao("2002-04-05")).isEqualTo("04/05/2002");
        } finally {
            org.springframework.context.i18n.LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void valoresForaDoPadraoNaoQuebram() {
        assertThat(MenuController.dataParaExibicao(null)).isNull();
        assertThat(MenuController.dataParaExibicao("05/04/2002")).isEqualTo("05/04/2002");
    }
}
