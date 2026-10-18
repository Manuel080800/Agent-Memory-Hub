package io.agentmemoryhub.api.config;

import io.agentmemoryhub.api.interceptor.ApiAuthorizationInterceptor;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "io.agentmemoryhub.api.controller")
@RequiredArgsConstructor
public class ApiServletConfiguration implements WebMvcConfigurer {
    private final ApiAuthorizationInterceptor apiAuthorizationInterceptor;

    @Bean
    public LocaleResolver localeResolver() {
        var localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.forLanguageTag("es")));
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        return localeResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry interceptorRegistry) {
        interceptorRegistry.addInterceptor(apiAuthorizationInterceptor).addPathPatterns("/api/v1/**").excludePathPatterns("/api/v1/auth/login");
    }
}
