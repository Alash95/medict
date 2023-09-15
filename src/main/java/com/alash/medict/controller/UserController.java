package com.alash.medict.controller;

import com.alash.medict.dto.request.LoginRequestDto;
import com.alash.medict.dto.request.ResetPasswordDto;
import com.alash.medict.dto.request.UserRequestDto;
import com.alash.medict.dto.response.CustomResponse;
import com.alash.medict.service.IUserService;
import com.alash.medict.service.impl.UserAuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.List;

@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
@Tag(name = "user")
public class UserController {
    private final IUserService userService;
    private final UserAuthenticationService authenticationService;


    // user related endpoints
    @PostMapping("register")
    public ResponseEntity<CustomResponse> register(@RequestBody UserRequestDto requestDto){
        return userService.registerUser(requestDto);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<CustomResponse> verifyEmail(@RequestParam("token") String token){
        return userService.verifyEmail(token);
    }

    @GetMapping("/resend-token")
    public ResponseEntity<?> resendVerificationToken(@RequestParam("token") String oldToken) throws MessagingException, UnsupportedEncodingException {
      return userService.resendVerificationTokenEmail(oldToken);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<CustomResponse> authenticateUser(@RequestBody @Validated LoginRequestDto loginRequest) throws Exception {
        return authenticationService.createAuthenticationTokenAndAuthenticateUser(loginRequest);
    }

//    @PostMapping("/reset-password")
//    public ResponseEntity<CustomResponse> resetPassword(@RequestParam(name = "email") String email) throws MessagingException, UnsupportedEncodingException {
//        return userService.resetPassword(email);
//    }
    @PostMapping("/reset-password")
    public ResponseEntity<CustomResponse> resetPassword(Authentication authentication) throws MessagingException, UnsupportedEncodingException {
        return userService.resetPassword(authentication.getName());
    }

    @PostMapping("/confirm-password-reset")
    public ResponseEntity<CustomResponse> confirmResetPassword(@RequestParam(name = "token") Integer token, @RequestBody ResetPasswordDto request){
        return userService.confirmResetPassword(token,request);
    }

}
