package io.agentmemoryhub.core.service.skill;

import io.agentmemoryhub.core.model.record.SkillInputRecord;
import io.agentmemoryhub.core.model.record.SkillPageRecord;
import io.agentmemoryhub.core.model.record.SkillRecord;

public interface SkillService {
    SkillPageRecord listSkillsByProfileId(String profileId, int page, int pageSize);

    SkillRecord getSkillByIdAndProfileId(String skillId, String profileId);

    SkillRecord createSkillByProfileIdAndInput(String profileId, SkillInputRecord skillInputRecord, String requestId);

    SkillRecord updateSkillByIdAndProfileId(String skillId, String profileId, SkillInputRecord skillInputRecord, Long expectedVersion);

    void deleteSkillByIdAndProfileId(String skillId, String profileId, boolean confirm);
}
