package org.hasanceliktr.saklik.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hasanceliktr.saklik.dto.AuthResponseDto;
import org.hasanceliktr.saklik.dto.LoginRequestDto;
import org.hasanceliktr.saklik.dto.MessageResponseDto;
import org.hasanceliktr.saklik.dto.RegisterRequestDto;
import org.hasanceliktr.saklik.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDto registerRequestDto) {
        authService.registerUser(registerRequestDto);
        logger.info("Kullanıcı kaydı başarılı: {}", registerRequestDto.getUsername());
        return ResponseEntity.ok(new MessageResponseDto("Kullanıcı başarıyla kaydedildi!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        AuthResponseDto authResponse = authService.loginUser(loginRequestDto);
        logger.info("Kullanıcı girişi başarılı: {}", loginRequestDto.getUsername());
        return ResponseEntity.ok(authResponse);
    }
}