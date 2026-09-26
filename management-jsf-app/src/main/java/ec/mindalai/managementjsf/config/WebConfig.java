package ec.mindalai.managementjsf.config;

import org.springframework.boot.tomcat.TomcatContextCustomizer;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ajustes del contenedor web embebido. La raiz del backoffice resuelve a
 * {@code index.xhtml}, que redirige al login o al dashboard segun exista sesion.
 */
@Configuration
public class WebConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> welcomeFileCustomizer() {
        return factory -> factory.addContextCustomizers((TomcatContextCustomizer) context ->
                context.addWelcomeFile("index.xhtml"));
    }
}
