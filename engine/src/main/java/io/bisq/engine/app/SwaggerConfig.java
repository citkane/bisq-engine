/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.engine.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {   
    @Bean
    public Docket api(){
        return new Docket(DocumentationType.SWAGGER_2)  
            .select()                                  
            .apis(RequestHandlerSelectors.basePackage( "io.bisq.engine.app.api" )) 
            .paths(PathSelectors.any())           
            .build()
            .apiInfo(metadata())
            .tags(
                new Tag("Offers", "Perform operations relating to offers"),
                new Tag("Preferences", "Perform operations relating to preferences"),
                new Tag("User", "Perform operations relating to the user")
            );     
    }
    Contact michael = new Contact("Bisq-engine","https://github.com/citkane/bisq-engine",null);
    private ApiInfo metadata() {
        return new ApiInfoBuilder()
        .title("Bisq-engine API")
        .description("API endpoints for the BISQ exchange: https://github.com/bisq-network/exchange")
        .version("0.6.5")
        .contact(michael)
        .build();
    }
}
