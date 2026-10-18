package io.agentmemoryhub.webapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import java.util.Locale;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "io.agentmemoryhub.core.repository")
@ComponentScan(basePackages = {
        "io.agentmemoryhub.core.converter",
        "io.agentmemoryhub.core.dao",
        "io.agentmemoryhub.core.service"
})
public class WebAppRootConfiguration {
    @Bean
    public DataSource dataSource() throws NamingException {
        return (DataSource) new InitialContext().lookup("java:/jdbc/AgentMemoryHubDS");
    }

    @Bean
    public EntityManagerFactory entityManagerFactory() throws NamingException {
        return (EntityManagerFactory) new InitialContext().lookup("java:jboss/agentMemoryHub/WebAppEntityManagerFactory");
    }

    @Bean
    public NamedParameterJdbcTemplate jdbcNativeTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager() throws NamingException {
        var jndiTemplate = new JndiTemplate();
        var userTransaction = jndiTemplate.lookup("java:comp/UserTransaction", UserTransaction.class);
        var transactionManager = jndiTemplate.lookup("java:jboss/TransactionManager", TransactionManager.class);
        var jtaTransactionManager = new JtaTransactionManager(userTransaction, transactionManager);
        jtaTransactionManager.setDefaultTimeout(30);
        return jtaTransactionManager;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().addModule(new JavaTimeModule()).build();
    }

    @Bean
    public MessageSource messageSource() {
        var messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(Locale.ENGLISH);
        return messageSource;
    }
}
