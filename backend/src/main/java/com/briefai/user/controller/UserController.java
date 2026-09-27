package com.briefai.user.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.user.dto.CurrentUserResponse;
import com.briefai.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/loggedInUser")
    public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.getCurrentUser(currentUser.getId());
    }
}
