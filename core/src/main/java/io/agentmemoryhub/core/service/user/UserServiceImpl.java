package io.agentmemoryhub.core.service.user;

import io.agentmemoryhub.core.common.PlatformRole;
import io.agentmemoryhub.core.dao.ProjectMemberDAO;
import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.domain.User;
import io.agentmemoryhub.core.model.record.UserInputRecord;
import io.agentmemoryhub.core.model.record.UserPageRecord;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.repository.UserRepository;
import io.agentmemoryhub.core.service.auth.PasswordHashService;
import io.agentmemoryhub.core.utils.ValidateUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ProjectMemberDAO projectMemberDAO;
    private final PasswordHashService passwordHashService;

    @Override
    @Transactional(readOnly = true)
    public UserPageRecord listUsers(int page, int pageSize) {
        var normalizedPage = Math.max(page, 0);
        var normalizedPageSize = Math.clamp(pageSize, 1, 100);
        var userEntityList = userRepository.findAll().stream().sorted(Comparator.comparing(User::getCreatedAt).reversed().thenComparing(User::getEmail)).toList();
        var firstIndex = (int) Math.min((long) normalizedPage * normalizedPageSize, userEntityList.size());
        var lastIndex = Math.min(firstIndex + normalizedPageSize, userEntityList.size());
        var userRecordList = userEntityList.subList(firstIndex, lastIndex).stream().map(this::toUserRecord).toList();
        return new UserPageRecord(userRecordList, normalizedPage, normalizedPageSize, userEntityList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public UserRecord getUserById(String userId) {
        return toUserRecord(findUserById(userId));
    }

    @Override
    @Transactional
    public UserRecord createUserByInput(UserInputRecord userInputRecord) {
        validateCreateUserInput(userInputRecord);
        var normalizedEmail = userInputRecord.email().strip().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new HubException("CONFLICT", "error.conflict", 409, "email");
        }

        var user = new User();
        user.setFullName(userInputRecord.fullName().strip());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHashService.hashPassword(userInputRecord.password()));
        user.setPlatformRole(Objects.requireNonNullElse(userInputRecord.platformRole(), PlatformRole.USER));
        user.setEnabled(Objects.isNull(userInputRecord.enabled()) || userInputRecord.enabled());
        return toUserRecord(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserRecord updateUserById(String userId, UserInputRecord userInputRecord, Long expectedVersion) {
        validateUpdateUserInput(userInputRecord);
        var user = findUserById(userId);
        var expectedVersionDoesNotMatch = Objects.nonNull(expectedVersion) && expectedVersion.longValue() != user.getVersion();

        if (expectedVersionDoesNotMatch) {
            throw new HubException("CONFLICT", "error.conflict", 409, userId);
        }

        if (Objects.nonNull(userInputRecord.fullName())) {
            user.setFullName(userInputRecord.fullName().strip());
        }

        if (Objects.nonNull(userInputRecord.email())) {
            var normalizedEmail = userInputRecord.email().strip().toLowerCase(Locale.ROOT);
            var emailHasChanged = !normalizedEmail.equalsIgnoreCase(user.getEmail());

            if (emailHasChanged && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                throw new HubException("CONFLICT", "error.conflict", 409, "email");
            }

            user.setEmail(normalizedEmail);
        }

        if (Objects.nonNull(userInputRecord.password())) {
            user.setPasswordHash(passwordHashService.hashPassword(userInputRecord.password()));
        }

        if (Objects.nonNull(userInputRecord.platformRole())) {
            user.setPlatformRole(userInputRecord.platformRole());
        }

        if (Objects.nonNull(userInputRecord.enabled())) {
            user.setEnabled(userInputRecord.enabled());
        }

        return toUserRecord(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUserById(String userId, boolean confirm) {
        if (!confirm) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        if (!projectMemberDAO.findProjectIdsByUserId(userId).isEmpty()) {
            throw new HubException("CONFLICT", "error.conflict", 409, "projectMemberships");
        }

        try {
            userRepository.delete(findUserById(userId));
        } catch (DataIntegrityViolationException exception) {
            throw new HubException("CONFLICT", "error.conflict", 409, List.of(), exception, "userReferences");
        }
    }

    private User findUserById(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new HubException("NOT_FOUND", "error.notFound", 404, userId));
    }

    private void validateCreateUserInput(UserInputRecord userInputRecord) {
        if (hasInvalidRequiredUserInput(userInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "user");
        }

        if (userInputRecord.password().length() < 12) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "password");
        }
    }

    private void validateUpdateUserInput(UserInputRecord userInputRecord) {
        if (hasNoUserChanges(userInputRecord)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "changes");
        }

        var fullName = userInputRecord.fullName();

        if (Objects.nonNull(fullName) && ValidateUtils.isInvalidString(fullName)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "fullName");
        }

        var email = userInputRecord.email();

        if (Objects.nonNull(email) && ValidateUtils.isInvalidString(email)) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "email");
        }

        if (Objects.nonNull(userInputRecord.password()) && userInputRecord.password().length() < 12) {
            throw new HubException("INVALID_ARGUMENT", "error.invalidArgument", 400, "password");
        }
    }

    private boolean hasInvalidRequiredUserInput(UserInputRecord userInputRecord) {
        if (Objects.isNull(userInputRecord)) {
            return true;
        }

        var fullNameIsInvalid = ValidateUtils.isInvalidString(userInputRecord.fullName());
        var emailIsInvalid = ValidateUtils.isInvalidString(userInputRecord.email());
        var passwordIsMissing = Objects.isNull(userInputRecord.password());
        return fullNameIsInvalid || emailIsInvalid || passwordIsMissing;
    }

    private boolean hasNoUserChanges(UserInputRecord userInputRecord) {
        if (Objects.isNull(userInputRecord)) {
            return true;
        }

        var fullNameIsMissing = Objects.isNull(userInputRecord.fullName());
        var emailIsMissing = Objects.isNull(userInputRecord.email());
        var passwordIsMissing = Objects.isNull(userInputRecord.password());
        var platformRoleIsMissing = Objects.isNull(userInputRecord.platformRole());
        var enabledIsMissing = Objects.isNull(userInputRecord.enabled());
        return fullNameIsMissing && emailIsMissing && passwordIsMissing && platformRoleIsMissing && enabledIsMissing;
    }

    private UserRecord toUserRecord(User user) {
        return new UserRecord(user.getId(), user.getFullName(), user.getEmail(), user.getPlatformRole(), user.isEnabled(), user.getCreatedAt(), user.getVersion());
    }
}
