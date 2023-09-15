package com.alash.medict.event;

import com.alash.medict.model.User;
import com.alash.medict.service.impl.EmailService;
import com.alash.medict.service.IUserService;
import com.alash.medict.service.impl.UserService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationCompletionEventListener implements ApplicationListener<RegistrationCompletePublisher>{
    private final IUserService userService;


    private User theUser;
    private final EmailService emailService;


    @Override
    public void onApplicationEvent(RegistrationCompletePublisher event) {
        //1. Get the newly registered user
        theUser = event.getUser();
        //2. Create verification token for user
        String verificationToken = UUID.randomUUID().toString();
        //3. Save the verification token to db
        userService.saveVerificationToken(theUser, verificationToken);
        //4. Build the verification url
        String url = event.getApplicationUrl()+"/api/v1/user/verify-email?token="+verificationToken;


        //5. Send the url to user mail
        try {
            emailService.sendVerificationEmail(url, theUser);
            emailService.sendResetPasswordEmail(UserService.theToken(), theUser);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        log.info("click this link to verify your email {}", url);

    }
}
