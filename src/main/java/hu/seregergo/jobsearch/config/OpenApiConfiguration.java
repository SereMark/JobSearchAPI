package hu.seregergo.jobsearch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI jobSearchOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Job Search API")
                .version("0.1.0")
                .description("Local API for evaluating and tracking job opportunities"));
    }
}
