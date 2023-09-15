package com.alash.medict.service;


import com.alash.medict.dto.request.ChangePasswordDTO;
import com.alash.medict.dto.request.ResetPasswordDto;
import com.alash.medict.dto.request.UserRequestDto;
import com.alash.medict.dto.response.CustomResponse;
import com.alash.medict.model.User;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public interface IUserService {

    ResponseEntity<CustomResponse> getAllUsers();
    ResponseEntity<CustomResponse> registerUser(UserRequestDto request);

    ResponseEntity<CustomResponse> findUserByEmail(String email);

    ResponseEntity<CustomResponse> fetchUserById(Long userId);

    void saveVerificationToken(User theUser, String verificationToken);

    ResponseEntity<CustomResponse> verifyEmail(String token);

    ResponseEntity<?> resendVerificationTokenEmail(String oldToken)throws MessagingException, UnsupportedEncodingException;


    ResponseEntity<CustomResponse> deleteProfile(Long userId);

    ResponseEntity<CustomResponse> updateProfile(Long userId, UserRequestDto request);

    ResponseEntity<CustomResponse> changePassword(ChangePasswordDTO request);


    ResponseEntity<CustomResponse>  resetPassword(String email) throws MessagingException, UnsupportedEncodingException;

    ResponseEntity<CustomResponse> confirmResetPassword(Integer token, ResetPasswordDto request);
}
