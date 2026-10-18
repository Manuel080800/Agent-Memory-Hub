package io.agentmemoryhub.api.controller;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.UserInputRecord;
import io.agentmemoryhub.core.model.record.UserPageRecord;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @GetMapping
    public UserPageRecord listUsers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return userService.listUsers(page, pageSize);
    }

    @PostMapping
    public UserRecord createUser(@RequestBody UserInputRecord userInputRecord) {
        return userService.createUserByInput(userInputRecord);
    }

    @GetMapping("/{userId}")
    public UserRecord getUser(@PathVariable String userId) {
        return userService.getUserById(userId);
    }

    @PutMapping("/{userId}")
    public UserRecord updateUser(@PathVariable String userId, @RequestBody UserInputRecord userInputRecord, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        return userService.updateUserById(userId, userInputRecord, expectedVersion);
    }

    @DeleteMapping("/{userId}")
    public void deleteUser(HttpServletRequest httpServletRequest, @PathVariable String userId, @RequestParam(defaultValue = "false") boolean confirm) {
        var authenticatedUser = (UserRecord) httpServletRequest.getAttribute("authenticatedUser");

        if (authenticatedUser.id().equals(userId)) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        userService.deleteUserById(userId, confirm);
    }
}
