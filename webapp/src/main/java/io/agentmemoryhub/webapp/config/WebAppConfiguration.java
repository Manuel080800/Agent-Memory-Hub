package io.agentmemoryhub.webapp.config;

import io.agentmemoryhub.webapp.handler.security.WebAppAuthorizationInterceptor;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
        "io.agentmemoryhub.webapp.controller",
        "io.agentmemoryhub.webapp.handler"
})
@RequiredArgsConstructor
public class WebAppConfiguration implements WebMvcConfigurer {
    private final WebAppAuthorizationInterceptor webAppAuthorizationInterceptor;

    @Bean
    public LocaleResolver localeResolver() {
        var localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.forLanguageTag("es")));
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        return localeResolver;
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry viewResolverRegistry) {
        viewResolverRegistry.jsp("/WEB-INF/views/", ".jsp");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry resourceHandlerRegistry) {
        resourceHandlerRegistry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry interceptorRegistry) {
        interceptorRegistry.addInterceptor(webAppAuthorizationInterceptor).addPathPatterns("/projects", "/projects/**", "/users", "/users/**");
    }
}
