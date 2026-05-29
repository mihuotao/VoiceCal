package com.voicecal.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI voiceCalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VoiceCal 智能语音日历 API")
                        .description("基于 Spring Boot 3 + MyBatis-Plus 的智能语音日历后端服务接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("VoiceCal Team")
                                .email("support@voicecal.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .schemaRequirement(SECURITY_SCHEME_NAME, new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("在输入框中输入 JWT Token（不含 Bearer 前缀）"));
    }

}
