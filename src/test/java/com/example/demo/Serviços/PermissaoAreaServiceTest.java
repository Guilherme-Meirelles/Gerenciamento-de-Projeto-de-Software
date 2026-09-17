package com.example.demo.Serviços;

import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

// Fixa a política de autorização de PermissaoAreaService, que já foi fonte de
// bugs de IDOR corrigidos no projeto (ex.: endpoint deixando qualquer
// participante administrar uma área, ou nem checar participação). Um
// regression test barato (sem banco) para não deixar essa política de
// visualizar/editar/administrar regredir silenciosamente.
@ExtendWith(MockitoExtension.class)
class PermissaoAreaServiceTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long AREA_ID = 10L;

    @Mock
    private ParticipacaoAreaRepository participacaoAreaRepository;

    @InjectMocks
    private PermissaoAreaService permissaoAreaService;

    private void mockarParticipacao(PermissaoArea permissao) {
        lenient().when(participacaoAreaRepository.existsByUsuarioIdAndAreaId(USUARIO_ID, AREA_ID))
                .thenReturn(true);
        ParticipacaoArea participacao = new ParticipacaoArea(null, null, permissao);
        lenient().when(participacaoAreaRepository.findByUsuarioIdAndAreaId(USUARIO_ID, AREA_ID))
                .thenReturn(participacao);
    }

    @Test
    void usuarioSemParticipacao_naoPodeVisualizarEditarNemAdministrar() {
        when(participacaoAreaRepository.existsByUsuarioIdAndAreaId(USUARIO_ID, AREA_ID)).thenReturn(false);
        when(participacaoAreaRepository.findByUsuarioIdAndAreaId(USUARIO_ID, AREA_ID)).thenReturn(null);

        assertThat(permissaoAreaService.podeVisualizar(USUARIO_ID, AREA_ID)).isFalse();
        assertThat(permissaoAreaService.podeEditar(USUARIO_ID, AREA_ID)).isFalse();
        assertThat(permissaoAreaService.podeAdministrar(USUARIO_ID, AREA_ID)).isFalse();
    }

    @Test
    void observador_soPodeVisualizar() {
        mockarParticipacao(PermissaoArea.OBSERVADOR);

        assertThat(permissaoAreaService.podeVisualizar(USUARIO_ID, AREA_ID)).isTrue();
        assertThat(permissaoAreaService.podeEditar(USUARIO_ID, AREA_ID)).isFalse();
        assertThat(permissaoAreaService.podeAdministrar(USUARIO_ID, AREA_ID)).isFalse();
    }

    @Test
    void editor_podeVisualizarEEditarMasNaoAdministrar() {
        mockarParticipacao(PermissaoArea.EDITOR);

        assertThat(permissaoAreaService.podeVisualizar(USUARIO_ID, AREA_ID)).isTrue();
        assertThat(permissaoAreaService.podeEditar(USUARIO_ID, AREA_ID)).isTrue();
        assertThat(permissaoAreaService.podeAdministrar(USUARIO_ID, AREA_ID)).isFalse();
    }

    @Test
    void admin_podeTudo() {
        mockarParticipacao(PermissaoArea.ADMIN);

        assertThat(permissaoAreaService.podeVisualizar(USUARIO_ID, AREA_ID)).isTrue();
        assertThat(permissaoAreaService.podeEditar(USUARIO_ID, AREA_ID)).isTrue();
        assertThat(permissaoAreaService.podeAdministrar(USUARIO_ID, AREA_ID)).isTrue();
    }

    // Cobertura exaustiva: para todo nível de permissão que não seja ADMIN,
    // podeAdministrar tem que ser false — é a checagem que já vazou antes
    // (bug corrigido: endpoint de geração de link não exigia ADMIN de verdade).
    @ParameterizedTest
    @EnumSource(value = PermissaoArea.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void apenasAdminPodeAdministrar(PermissaoArea permissaoNaoAdmin) {
        mockarParticipacao(permissaoNaoAdmin);

        assertThat(permissaoAreaService.podeAdministrar(USUARIO_ID, AREA_ID)).isFalse();
    }
}
