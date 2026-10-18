package io.agentmemoryhub.core.service.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.core.dao.IdempotencyKeyDAO;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.IdempotencyKeyRecord;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyKeyServiceImplTest {
    @Mock
    private IdempotencyKeyDAO idempotencyKeyDAO;

    @InjectMocks
    private IdempotencyKeyServiceImpl idempotencyKeyService;

    @Test
    void returnsThePreviouslyReservedResourceForTheSameRequestContent() {
        // Verifies retries reuse the resource id and reserve a SHA-256 hash of the request body.
        when(idempotencyKeyDAO.findIdempotencyKeyByProjectIdAndOperationAndRequestId("project-1", "create_task", "request-1")).thenReturn(Optional.of(new IdempotencyKeyRecord("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", "task-1")));

        var resourceId = idempotencyKeyService.reserveResourceIdByRequest("project-1", "create_task", "request-1", "abc", "new-task");

        assertEquals("task-1", resourceId);
        verify(idempotencyKeyDAO).insertIdempotencyKeyIfAbsentByProjectIdAndOperationAndRequestId("project-1", "create_task", "request-1", "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", "new-task");
    }

    @Test
    void rejectsRequestIdReuseWithDifferentContent() {
        // Verifies an idempotency key cannot be reused for a different request body.
        when(idempotencyKeyDAO.findIdempotencyKeyByProjectIdAndOperationAndRequestId("project-1", "create_task", "request-1")).thenReturn(Optional.of(new IdempotencyKeyRecord("another-hash", "task-1")));

        var exception = assertThrows(HubException.class, () -> idempotencyKeyService.reserveResourceIdByRequest("project-1", "create_task", "request-1", "abc", "new-task"));

        assertEquals("CONFLICT", exception.getCode());
        assertEquals(409, exception.getHttpStatus());
    }

    @Test
    void reportsUnavailableWhenTheReservationCannotBeReadBack() {
        // Verifies a missing reservation is surfaced as an unavailable storage operation.
        when(idempotencyKeyDAO.findIdempotencyKeyByProjectIdAndOperationAndRequestId("project-1", "create_task", "request-1")).thenReturn(Optional.empty());

        var exception = assertThrows(HubException.class, () -> idempotencyKeyService.reserveResourceIdByRequest("project-1", "create_task", "request-1", "abc", "new-task"));

        assertEquals("CONNECTION_UNAVAILABLE", exception.getCode());
        assertEquals(503, exception.getHttpStatus());
    }
}
