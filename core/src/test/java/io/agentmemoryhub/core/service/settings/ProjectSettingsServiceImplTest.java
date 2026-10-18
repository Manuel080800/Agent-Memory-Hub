package io.agentmemoryhub.core.service.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.agentmemoryhub.core.common.TaskCreationMode;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.ProjectSettings;
import io.agentmemoryhub.core.model.record.ProjectSettingsInputRecord;
import io.agentmemoryhub.core.repository.ProjectRepository;
import io.agentmemoryhub.core.repository.ProjectSettingsRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProjectSettingsServiceImplTest {
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectSettingsRepository projectSettingsRepository;

    @InjectMocks
    private ProjectSettingsServiceImpl projectSettingsService;

    @Test
    void updatesSettingsAndRejectsBudgetsOutsideTheSupportedRange() {
        // Verifies valid setting changes are persisted while out-of-range budgets fail validation.
        when(projectRepository.existsById("project-1")).thenReturn(true);
        when(projectSettingsRepository.findById("project-1")).thenReturn(Optional.empty());
        when(projectSettingsRepository.save(any(ProjectSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var settingsRecord = projectSettingsService.updateProjectSettingsByProjectId("project-1", new ProjectSettingsInputRecord(null, null, false, TaskCreationMode.AUTOMATIC, true, null, 8000, 60, null), null);
        var exception = assertThrows(HubException.class, () -> projectSettingsService.updateProjectSettingsByProjectId("project-1", new ProjectSettingsInputRecord(null, null, null, null, null, null, 99, null, null), null));

        assertEquals(TaskCreationMode.AUTOMATIC, settingsRecord.taskCreationMode());
        assertEquals(8000, settingsRecord.contextBudgetTokens());
        assertEquals("INVALID_ARGUMENT", exception.getCode());
    }

    @Test
    void resetsSettingsToTheirEntityDefaults() {
        // Verifies reset restores default retrieval, selection, budget, and quota values.
        when(projectRepository.existsById("project-1")).thenReturn(true);
        var settings = new ProjectSettings();
        settings.setProjectId("project-1");
        settings.setContextBudgetTokens(12000);
        settings.setPermanentQuotaPercent(20);
        when(projectSettingsRepository.findById("project-1")).thenReturn(Optional.of(settings));
        when(projectSettingsRepository.save(settings)).thenReturn(settings);

        var resetRecord = projectSettingsService.resetProjectSettingsByProjectId("project-1");

        assertTrue(resetRecord.contextRetrievalEnabled());
        assertEquals(6000, resetRecord.contextBudgetTokens());
        assertEquals(70, resetRecord.permanentQuotaPercent());
    }
}
