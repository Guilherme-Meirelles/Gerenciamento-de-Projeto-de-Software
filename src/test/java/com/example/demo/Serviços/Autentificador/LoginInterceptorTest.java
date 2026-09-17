package com.example.demo.Serviços.Autentificador;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

// LoginInterceptor é o portão que protege praticamente todas as rotas da
// aplicação (ver exclusões em LoginInterceptorConfig) — vale a pena garantir
// que ele bloqueia/libera exatamente quando deveria.
class LoginInterceptorTest {

    private final LoginInterceptor interceptor = new LoginInterceptor();

    @Test
    void semSessao_bloqueiaERedirecionaParaLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean prossegue = interceptor.preHandle(request, response, new Object());

        assertThat(prossegue).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login");
    }

    @Test
    void comSessaoAutenticada_liberaOAcesso() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        SessaoUtil.autenticar(request, 1L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean prossegue = interceptor.preHandle(request, response, new Object());

        assertThat(prossegue).isTrue();
        assertThat(response.getRedirectedUrl()).isNull();
    }
}
