package io.agentmemoryhub.core.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentmemoryhub.core.common.ProjectRole;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
public class PostgreSqlDaoIntegrationTest {
    private static final String USER_ID = "user-1";
    private static final String PROJECT_ID = "project-1";
    private static final String PROFILE_ID = "profile-1";

    @Container
    private static final PostgreSQLContainer POSTGRESQL_CONTAINER = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine")).withDatabaseName("agent_memory_hub_test").withUsername("agent_memory_hub").withPassword("agent_memory_hub");

    private DriverManagerDataSource dataSource;
    private NamedParameterJdbcTemplate jdbcNativeTemplate;
    private AuthenticationDAOImpl authenticationDAO;
    private IdempotencyKeyDAOImpl idempotencyKeyDAO;
    private ProjectMemberDAOImpl projectMemberDAO;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new DriverManagerDataSource(POSTGRESQL_CONTAINER.getJdbcUrl(), POSTGRESQL_CONTAINER.getUsername(), POSTGRESQL_CONTAINER.getPassword());
        jdbcNativeTemplate = new NamedParameterJdbcTemplate(dataSource);
        authenticationDAO = new AuthenticationDAOImpl(jdbcNativeTemplate);
        idempotencyKeyDAO = new IdempotencyKeyDAOImpl(jdbcNativeTemplate);
        projectMemberDAO = new ProjectMemberDAOImpl(jdbcNativeTemplate);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA public CASCADE");
            statement.execute("CREATE SCHEMA public");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/001_schema.sql"));
        }

        var userSqlCommand = "INSERT INTO users (id, full_name, email, password_hash, platform_role) VALUES (:id, :fullName, :email, :passwordHash, 'USER')";
        var testUserParams = new MapSqlParameterSource(Map.of("id", USER_ID, "fullName", "Test User", "email", "test@example.com", "passwordHash", "hash"));
        jdbcNativeTemplate.update(userSqlCommand, testUserParams);
        var secondUserParams = new MapSqlParameterSource(Map.of("id", "user-2", "fullName", "Second User", "email", "second@example.com", "passwordHash", "hash"));
        jdbcNativeTemplate.update(userSqlCommand, secondUserParams);
        var projectSqlCommand = "INSERT INTO projects (id, name, owner_user_id) VALUES (:id, :name, :ownerUserId)";
        var projectParams = new MapSqlParameterSource(Map.of("id", PROJECT_ID, "name", "Test Project", "ownerUserId", USER_ID));
        jdbcNativeTemplate.update(projectSqlCommand, projectParams);
        var profileSqlCommand = "INSERT INTO profiles (id, project_id, name) VALUES (:id, :projectId, :name)";
        var profileParams = new MapSqlParameterSource(Map.of("id", PROFILE_ID, "projectId", PROJECT_ID, "name", "Default"));
        jdbcNativeTemplate.update(profileSqlCommand, profileParams);
        var activeProfileParams = new MapSqlParameterSource(Map.of("profileId", PROFILE_ID, "projectId", PROJECT_ID));
        jdbcNativeTemplate.update("UPDATE projects SET active_profile_id = :profileId WHERE id = :projectId", activeProfileParams);
    }

    @Test
    void readsCredentialsCreatesSessionsAndRejectsExpiredSessions() {
        // Verifies authentication SQL maps case-insensitive credentials and enforces session expiry.
        var credential = authenticationDAO.findPasswordCredentialByEmail("TEST@example.com").orElseThrow();
        var expiry = Instant.now().plus(1, ChronoUnit.HOURS);
        authenticationDAO.createSessionByTokenHashAndUserIdAndExpiry("valid-token-hash", USER_ID, expiry);

        assertEquals(USER_ID, credential.userId());
        assertEquals("hash", credential.passwordHash());
        assertEquals(USER_ID, authenticationDAO.findUserIdByTokenHash("valid-token-hash").orElseThrow());

        var expiredSessionParams = new MapSqlParameterSource(Map.of("tokenHash", "expired-token-hash", "userId", USER_ID, "expiresAt", Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES))));
        jdbcNativeTemplate.update("INSERT INTO auth_sessions (token_hash, user_id, expires_at) VALUES (:tokenHash, :userId, :expiresAt)", expiredSessionParams);

        assertTrue(authenticationDAO.findUserIdByTokenHash("expired-token-hash").isEmpty());
        authenticationDAO.deleteSessionByTokenHash("valid-token-hash");
        assertTrue(authenticationDAO.findUserIdByTokenHash("valid-token-hash").isEmpty());
    }

    @Test
    void reservesIdempotencyKeysOnceAndDeletesExpiredReservations() {
        // Verifies PostgreSQL conflict handling retains the first reservation and permits reuse after expiry.
        assertEquals(1, idempotencyKeyDAO.insertIdempotencyKeyIfAbsentByProjectIdAndOperationAndRequestId(PROJECT_ID, "create_task", "request-1", "hash-1", "task-1"));
        assertEquals(0, idempotencyKeyDAO.insertIdempotencyKeyIfAbsentByProjectIdAndOperationAndRequestId(PROJECT_ID, "create_task", "request-1", "hash-2", "task-2"));
        var reservation = idempotencyKeyDAO.findIdempotencyKeyByProjectIdAndOperationAndRequestId(PROJECT_ID, "create_task", "request-1").orElseThrow();

        assertEquals("hash-1", reservation.contentHash());
        assertEquals("task-1", reservation.resourceId());

        var expiredKeyParams = new MapSqlParameterSource(Map.of("projectId", PROJECT_ID, "operation", "create_task", "requestId", "request-1"));
        jdbcNativeTemplate.update("UPDATE idempotency_keys SET created_at = now() - interval '8 days' WHERE project_id = :projectId AND operation = :operation AND request_id = :requestId", expiredKeyParams);

        assertEquals(1, idempotencyKeyDAO.deleteExpiredIdempotencyKeyByProjectIdAndOperationAndRequestId(PROJECT_ID, "create_task", "request-1"));
        assertEquals(1, idempotencyKeyDAO.insertIdempotencyKeyIfAbsentByProjectIdAndOperationAndRequestId(PROJECT_ID, "create_task", "request-1", "hash-2", "task-2"));
        assertEquals("task-2", idempotencyKeyDAO.findIdempotencyKeyByProjectIdAndOperationAndRequestId(PROJECT_ID, "create_task", "request-1").orElseThrow().resourceId());
    }

    @Test
    void maintainsProjectMembershipRolesAndReverseLookups() {
        // Verifies owner insertion, role changes, user project lookups, and membership removal.
        projectMemberDAO.addOwnerMember(PROJECT_ID, USER_ID);
        projectMemberDAO.addProjectMember(PROJECT_ID, "user-2", ProjectRole.EDITOR);

        assertEquals(ProjectRole.EDITOR, projectMemberDAO.findProjectRoleByProjectIdAndUserId(PROJECT_ID, "user-2").orElseThrow());
        assertEquals(java.util.List.of(PROJECT_ID), projectMemberDAO.findProjectIdsByUserId("user-2"));
        assertEquals(2, projectMemberDAO.findProjectMembersByProjectId(PROJECT_ID).size());

        projectMemberDAO.updateProjectRoleByProjectIdAndUserId(PROJECT_ID, "user-2", ProjectRole.READER);
        assertEquals(ProjectRole.READER, projectMemberDAO.findProjectRoleByProjectIdAndUserId(PROJECT_ID, "user-2").orElseThrow());

        projectMemberDAO.removeProjectMemberByProjectIdAndUserId(PROJECT_ID, "user-2");
        assertFalse(projectMemberDAO.findProjectRoleByProjectIdAndUserId(PROJECT_ID, "user-2").isPresent());
        assertEquals(ProjectRole.OWNER, projectMemberDAO.findProjectRoleByProjectIdAndUserId(PROJECT_ID, USER_ID).orElseThrow());
    }
}
