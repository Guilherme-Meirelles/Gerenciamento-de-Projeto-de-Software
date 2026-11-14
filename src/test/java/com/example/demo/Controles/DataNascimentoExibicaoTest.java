package com.example.demo.Controles;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** A data de nascimento do perfil aparece como dd/mm/aaaa em todas as telas (menu, áreas de trabalho, erros). */
class DataNascimentoExibicaoTest {

    @Test
    void converteDeAaaaMmDdParaDdMmAaaa() {
        assertThat(MenuController.dataParaExibicao("2002-04-05")).isEqualTo("05/04/2002");
    }

    @Test
    void valoresForaDoPadraoNaoQuebram() {
        assertThat(MenuController.dataParaExibicao(null)).isNull();
        assertThat(MenuController.dataParaExibicao("05/04/2002")).isEqualTo("05/04/2002");
    }
}
