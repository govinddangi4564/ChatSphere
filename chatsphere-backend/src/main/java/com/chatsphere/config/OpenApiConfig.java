package com.chatsphere.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chatSphereOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ChatSphere REST API")
                        .description("Real-time full-stack messaging application API documentation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ChatSphere Development Team")
                                .email("dev@chatsphere.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
