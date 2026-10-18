package io.agentmemoryhub.api.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {
        "io.agentmemoryhub.api.handler",
        "io.agentmemoryhub.api.interceptor",
        "io.agentmemoryhub.api.service",
        "io.agentmemoryhub.core.converter",
        "io.agentmemoryhub.core.dao",
        "io.agentmemoryhub.core.service"
})
@Import({ApiPersistenceConfiguration.class, ApiMessageConfiguration.class})
public class ApiRootConfiguration {
}
