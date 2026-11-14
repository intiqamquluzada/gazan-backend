package az.qazan.backend.common.security;

import az.qazan.backend.common.i18n.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTests {

    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(
            new ObjectMapper().findAndRegisterModules(),
            Mockito.mock(MessageService.class),
            2);

    private int post(String path, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", path);
        req.addHeader("X-Forwarded-For", ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void blocksAfterTheLimitPerIpAndPath() throws Exception {
        assertThat(post("/api/v1/auth/login", "1.1.1.1")).isEqualTo(200);
        assertThat(post("/api/v1/auth/login", "1.1.1.1")).isEqualTo(200);
        assertThat(post("/api/v1/auth/login", "1.1.1.1")).isEqualTo(429);
        assertThat(post("/api/v1/auth/login", "2.2.2.2")).isEqualTo(200);
        assertThat(post("/api/v1/auth/register", "1.1.1.1")).isEqualTo(200);
    }

    @Test
    void leavesOtherRoutesAlone() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(post("/api/v1/nfc/tap", "3.3.3.3")).isEqualTo(200);
        }
    }
}
