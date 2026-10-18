package io.agentmemoryhub.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@EnableJpaRepositories(basePackages = "io.agentmemoryhub.core.repository")
public class ApiPersistenceConfiguration {
    @Bean
    public DataSource dataSource() throws NamingException {
        return (DataSource) new InitialContext().lookup("java:/jdbc/AgentMemoryHubDS");
    }

    @Bean
    public EntityManagerFactory entityManagerFactory() throws NamingException {
        return (EntityManagerFactory) new InitialContext().lookup("java:jboss/agentMemoryHub/EntityManagerFactory");
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
}
