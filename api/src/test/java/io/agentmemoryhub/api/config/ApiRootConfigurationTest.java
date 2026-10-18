package io.agentmemoryhub.api.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.EnableTransactionManagement;

class ApiRootConfigurationTest {
    @Test
    void enablesTransactionsForCoreServicesUsedByApiOperations() {
        // Verifies API requests activate Core transaction boundaries for multi-step operations.
        var transactionManagement = ApiRootConfiguration.class.getAnnotation(EnableTransactionManagement.class);

        assertNotNull(transactionManagement);
    }
}
