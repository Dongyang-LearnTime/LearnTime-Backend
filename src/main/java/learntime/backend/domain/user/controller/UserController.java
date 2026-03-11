package learntime.backend.domain.user.controller;

import jakarta.validation.Valid;
import learntime.backend.domain.user.dto.request.SignUpRequestDTO;
import learntime.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<String> signupUser(@Valid @RequestBody SignUpRequestDTO request) {
        userService.createUser(request.getUserName(), request.getEmail(), request.getPassword());
        log.info("{} 회원가입 성공!", request.getUserName());
        return ResponseEntity.ok().build();
    }
}
