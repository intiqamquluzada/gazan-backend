package az.qazan.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs on real Tomcat: MockMvc defaults JSON to UTF-8 and would hide the
 * bug where the 401 body came out as ISO-8859-1. The app could not parse
 * that 401, never refreshed its token and showed "no internet".
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:encoding;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.jwt.secret=test-secret-please-replace-with-256-bit-key-for-real-tests",
        "qazan.rate-limit.per-minute=1000",
})
class ErrorEncodingTests {

    @LocalServerPort int port;

    @Test
    void unauthorizedBodyIsUtf8InEveryLanguage() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        for (String lang : List.of("az", "tr", "ru", "en")) {
            HttpResponse<byte[]> res = http.send(HttpRequest.newBuilder(
                            URI.create("http://localhost:" + port + "/api/v1/users/me"))
                    .header("Accept-Language", lang).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertThat(res.statusCode()).isEqualTo(401);
            assertThat(res.headers().firstValue("Content-Type").orElse(""))
                    .containsIgnoringCase("utf-8");
            // Throws on bytes that are not valid UTF-8.
            String body = StandardCharsets.UTF_8.newDecoder()
                    .decode(ByteBuffer.wrap(res.body())).toString();
            assertThat(body).contains("\"code\":\"UNAUTHORIZED\"");
        }
    }
}
