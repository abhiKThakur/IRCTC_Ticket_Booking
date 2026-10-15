package com.irctc.controllers;

import com.irctc.Util.UserServiceUtil;
import com.irctc.entities.User;
import com.irctc.services.AuthSessionService;
import com.irctc.services.UserBookingService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "https://railwaygo.vercel.app"
})
public class AuthController {

    private final AuthSessionService authSessionService;

    public AuthController(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    // -----------------------------------------
    // SIGN UP
    // -----------------------------------------

    @PostMapping("/signup")
    public Map<String, String> signup(
            @RequestBody SignupRequest request) {

        if (request == null
                || request.getName() == null
                || request.getName().isBlank()
                || request.getEmail() == null
                || request.getEmail().isBlank()
                || request.getPassword() == null
                || request.getPassword().length() < 8) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Name, email and a password of at least 8 characters are required"
            );
        }

        try {
            UserBookingService service = new UserBookingService();

            if (service.findUserByEmail(request.getEmail()) != null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "An account with this email already exists"
                );
            }

            User user = new User();

            user.setName(request.getName().trim());
            user.setEmail(request.getEmail().trim().toLowerCase());
            user.setPassword(request.getPassword());
            user.setHashPassword(
                    UserServiceUtil.hashPassword(request.getPassword())
            );
            user.setUserId(UUID.randomUUID().toString());

            boolean registered = service.signUp(user);

            if (!registered) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Registration failed"
                );
            }

            return Map.of(
                    "status", "success",
                    "message", "Account created successfully"
            );

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to register user",
                    e
            );
        }
    }

    // -----------------------------------------
    // LOGIN
    // -----------------------------------------

    @PostMapping("/login")
    public Map<String, String> login(
            @RequestBody LoginRequest request) {

        if (request == null
                || request.getEmail() == null
                || request.getEmail().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email and password are required"
            );
        }

        try {
            UserBookingService service = new UserBookingService();

            User user = service.login(
                    request.getEmail(),
                    request.getPassword()
            );

            if (user == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                );
            }

            // Create a server-side session for the authenticated user.
            String token = authSessionService.createSession(
                    user.getUserId()
            );

            return Map.of(
                    "status", "success",
                    "message", "Login successful",
                    "token", token,
                    "userId", user.getUserId(),
                    "name", user.getName(),
                    "email", user.getEmail()
            );

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to log in",
                    e
            );
        }
    }

    // -----------------------------------------
    // LOGOUT
    // -----------------------------------------

    @PostMapping("/logout")
    public Map<String, String> logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader) {

        String token = extractBearerToken(authorizationHeader);

        if (token == null
                || !authSessionService.invalidateSession(token)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired authentication token"
            );
        }

        return Map.of(
                "status", "success",
                "message", "Logged out successfully"
        );
    }

    // -----------------------------------------
    // TOKEN EXTRACTION
    // -----------------------------------------

    private String extractBearerToken(String authorizationHeader) {

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader
                .substring(7)
                .trim();

        return token.isEmpty() ? null : token;
    }

    // -----------------------------------------
    // SIGNUP REQUEST
    // -----------------------------------------

    public static class SignupRequest {

        private String name;
        private String email;
        private String password;

        public SignupRequest() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // -----------------------------------------
    // LOGIN REQUEST
    // -----------------------------------------

    public static class LoginRequest {

        private String email;
        private String password;

        public LoginRequest() {
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}