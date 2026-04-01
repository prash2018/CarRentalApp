package com.carrental.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI carRentalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Car Rental System APIs")
                        .description("""
                                A fully simulated Car Rental System built with Spring Boot and Java.                             
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Car Rental System")
                                ));

    }
}

