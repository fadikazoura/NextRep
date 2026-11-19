package com.fadikazoura.nextrep.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** API documentation at /swagger-ui.html, with an "Authorize" button for the JWT. */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI nextRepOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NextRep API")
                        .version("v1")
                        .description("""
                                Smart gym workout tracker: personalised weekly plans (rule-based or AI), \
                                workout logging, automatic weight progression, personal records and progress charts.

                                Register and log in with the auth endpoints, then click **Authorize** \
                                and paste the access token."""))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
