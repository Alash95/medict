package com.alash.medict.service.impl;

import com.alash.medict.dto.request.LoginRequestDto;
import com.alash.medict.dto.response.CustomResponse;
import com.alash.medict.dto.response.LoginResponse;
import com.alash.medict.exception.ApplicationAuthenticationException;
import com.alash.medict.model.User;
import com.alash.medict.repository.IUserRepository;
import com.alash.medict.security.CustomUserDetails;
import com.alash.medict.security.CustomerUserDetailsService;
import com.alash.medict.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthenticationService {
    private final JwtTokenUtil jwtTokenUtil;
    private final CustomerUserDetailsService userDetailsService;
    private final IUserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    public ResponseEntity<CustomResponse> createAuthenticationTokenAndAuthenticateUser(LoginRequestDto loginRequest) throws Exception {
        authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
        final CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(loginRequest.getUsername());
        User user = userRepository.findUserByEmail(loginRequest.getUsername()).get();

        final String token = jwtTokenUtil.generateToken(userDetails);

        LoginResponse response = LoginResponse.builder()
                .id(user.getId())
                .access_token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .isEnabled(user.isEnabled())
                .build();
        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK.name(), response, "Login successfully"));
    }

    // authenticate user

    private void authenticateUser(String username, String password) throws Exception {
        try{
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

        }catch (DisabledException e){
            throw new Exception("USER_DISABLED", e);
        }catch (BadCredentialsException e){
            throw new ApplicationAuthenticationException("Invalid username or password combination", e);
        }

    }
}
