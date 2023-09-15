package com.alash.medict.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Oyinlola",
                        email = "oyinlolaalasho95@gmail.com",
                        url = "https://www.linkedin.com/in/oyinlolaalasoluyi/"

                ),
                description = "OpenApi documentation for user management service",
                title = "Medical Dictionary User Service",
                version = "1.0"
        ),
        security = {
                @SecurityRequirement(name = "JWTAuth")
        }
)
@SecurityScheme(
        name = "JWTAuth",
        description = "JWT authentication",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        in = SecuritySchemeIn.HEADER,
        bearerFormat = "JWT"
)
public class OpenAPIConfig {
}
