package com.bank.fundtransfer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and description shown at the top of the Swagger UI page. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fundTransferOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Fund Transfer System API")
                .version("1.0")
                .description("Customers send their ID and PIN in the X-Customer-Code and X-Customer-Pin headers. "
                        + "Admin endpoints use X-Admin-Username and X-Admin-Password. "
                        + "(This temporary scheme is replaced by JWT login in a later phase.)"));
    }
}