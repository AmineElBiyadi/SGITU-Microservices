package com.ensate.billetterie.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(ApiProperties props) {
        return new OpenAPI()
                .info(new Info()
                        .title(props.getInfo().getTitle())
                        .description(props.getInfo().getDescription())
                        .version(props.getInfo().getVersion())
                        .contact(new Contact()
                                .name(props.getContact().getName())
                                .email(props.getContact().getEmail())
                                .url(props.getContact().getUrl())
                        )
                );
    }
}
