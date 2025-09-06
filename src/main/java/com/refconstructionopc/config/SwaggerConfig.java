package com.pos.streamline.config.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Case Management Service", description = "Claim Event Information", version = "1.0"))
public class SwaggerConfig {

    private static final String SCHEME_NAME = "bearerAuth";
    private static final String BEARER_FORMAT = "JWT";
    private static final String SCHEME = "bearer";

    @Bean
    public OpenAPI caseOpenAPI() {
        // Define the security scheme (Bearer Token)
        SecurityScheme securityScheme = new SecurityScheme()
                .name(SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)  // HTTP scheme for Bearer Authentication
                .scheme(SCHEME)  // Bearer authentication scheme
                .bearerFormat(BEARER_FORMAT)  // JWT format
                .in(SecurityScheme.In.HEADER);  // Token is expected in the header

        // Define the security requirement (to apply this security scheme globally)
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(SCHEME_NAME);

        // Return OpenAPI configuration with the security scheme and requirement
        return new OpenAPI()
                .addSecurityItem(securityRequirement)  // Add the security requirement
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, securityScheme)  // Register the security scheme
                );
    }
}
