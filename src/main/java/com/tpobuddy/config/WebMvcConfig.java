package com.tpobuddy.config;

import com.tpobuddy.security.SessionAuthInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SessionAuthInterceptor sessionAuthInterceptor;

    public WebMvcConfig(SessionAuthInterceptor sessionAuthInterceptor) {
        this.sessionAuthInterceptor = sessionAuthInterceptor;
    }

    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(
                        "classpath:/META-INF/resources/assets/",
                        "classpath:/static/assets/",
                        "file:src/main/webapp/assets/",
                        "/assets/"
                )
                .setCachePeriod(3600);

        registry.addResourceHandler("/react-dist/**")
                .addResourceLocations(
                        "classpath:/static/react-dist/",
                        "file:src/main/resources/static/react-dist/"
                )
                .setCachePeriod(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/assets/**", "/react-dist/**", "/favicon.ico", "/", "/index.jsp",
                        "/login.jsp", "/register.jsp", "/forgot-password.jsp",
                        "/react-portal", "/react-portal.jsp", "/portal", "/react-app",
                        "/api/auth/**", "/logout", "/actuator/**");
    }
}
