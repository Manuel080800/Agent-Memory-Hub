package io.agentmemoryhub.core.service.profile;

import io.agentmemoryhub.core.model.record.ProfileInputRecord;
import io.agentmemoryhub.core.model.record.ProfilePageRecord;
import io.agentmemoryhub.core.model.record.ProfileRecord;

public interface ProfileService {
    ProfilePageRecord listProfilesByProjectId(String projectId, int page, int pageSize);

    ProfileRecord getProfileByIdAndProjectId(String profileId, String projectId);

    ProfileRecord createProfileByProjectIdAndInput(String projectId, ProfileInputRecord profileInputRecord);

    ProfileRecord updateProfileByIdAndProjectId(String profileId, String projectId, ProfileInputRecord profileInputRecord, Long expectedVersion);

    void deleteProfileByIdAndProjectId(String profileId, String projectId, boolean confirm);
}
