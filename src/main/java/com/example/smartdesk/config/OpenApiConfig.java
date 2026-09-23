package com.example.smartdesk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartDeskOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Smart Desk Booking REST API")
                .description("High-concurrency desk reservation platform with team neighbourhood clustering, quota enforcement, and automated no-show release.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("LPU Backend Case Study 2026")
                    .email("support@smartdesk.example.com")));
    }
}
