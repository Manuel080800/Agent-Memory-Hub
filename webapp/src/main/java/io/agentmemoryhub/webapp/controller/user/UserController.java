package io.agentmemoryhub.webapp.controller.user;

import io.agentmemoryhub.core.exception.HubException;
import io.agentmemoryhub.core.model.record.UserInputRecord;
import io.agentmemoryhub.core.model.record.UserPageRecord;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.service.user.UserService;
import io.agentmemoryhub.webapp.handler.session.WebAppSessionHandler;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final WebAppSessionHandler webAppSessionHandler;

    @GetMapping
    public ResponseEntity<UserPageRecord> listUsers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int pageSize) {
        return ResponseEntity.ok(userService.listUsers(page, pageSize));
    }

    @PostMapping
    public ResponseEntity<UserRecord> createUser(@RequestBody UserInputRecord userInputRecord) {
        return ResponseEntity.ok(userService.createUserByInput(userInputRecord));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserRecord> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserRecord> updateUser(@PathVariable String userId, @RequestBody UserInputRecord userInputRecord, @RequestHeader(name = "If-Match", required = false) Long expectedVersion) {
        return ResponseEntity.ok(userService.updateUserById(userId, userInputRecord, expectedVersion));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(HttpSession httpSession, @PathVariable String userId) {
        var authenticatedUser = webAppSessionHandler.getUser(httpSession);

        if (authenticatedUser.id().equals(userId)) {
            throw new HubException("OPERATION_NOT_ALLOWED", "error.operationNotAllowed", 409);
        }

        userService.deleteUserById(userId, true);
        return ResponseEntity.noContent().build();
    }
}
