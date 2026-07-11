package com.vcarrin87.dynamodb_example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebUiConfig implements WebMvcConfigurer {

    /**
     * Maps /graphiql to the custom static GraphiQL page.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/graphiql").setViewName("forward:/graphiql/index.html");
    }
}
