package az.qazan.backend.media.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * Raises the servlet multipart limits (Spring Boot defaults to 1MB) so
 * photo uploads work. Declared here instead of in {@code application.yml}
 * to keep the core config file untouched.
 */
@Configuration
public class MediaMultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(6));
        factory.setMaxRequestSize(DataSize.ofMegabytes(8));
        return factory.createMultipartConfig();
    }
}
