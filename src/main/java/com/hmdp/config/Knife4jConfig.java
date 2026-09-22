package com.hmdp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 4.x (基于 springdoc-openapi) 接口文档配置
 * 分组配置见 application.yaml 的 springdoc.group-configs
 * 访问地址: http://localhost:8080/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("黑马点评 API文档")
                        .description("黑马点评项目接口文档 - 基于Knife4j 4.x (OpenAPI3)")
                        .version("1.0")
                        .contact(new Contact().name("hmdp")));
    }
}

