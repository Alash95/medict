package com.alash.medict.service;


import com.alash.medict.dto.request.ResetPasswordDto;
import com.alash.medict.dto.request.UserRequestDto;
import com.alash.medict.dto.response.CustomResponse;
import com.alash.medict.dto.response.UserResponseDto;
import com.alash.medict.event.RegistrationCompletePublisher;
import com.alash.medict.model.Role;
import com.alash.medict.model.User;
import com.alash.medict.model.VerificationToken;
import com.alash.medict.repository.IUserRepository;
import com.alash.medict.repository.IVerificationTokenRepository;
import com.alash.medict.repository.RoleRepository;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements IUserService{

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final IRoleService roleService;
    private final ApplicationEventPublisher publisher;
    private final HttpServletRequest servletRequest;
    private final RoleRepository roleRepository;
//    private final RedisTemplate redisTemplate;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]+$";
    private static final Pattern pattern = Pattern.compile(EMAIL_REGEX);

    private static final String IMAGE_FOLDER = System.getProperty("user.dir") + "/src/main/resources/user_profile/";

    private static final String PASSWORD_RESET = "code";


    @Override
    public ResponseEntity<CustomResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponseDto> userResponseList = users.stream()
                .map(user -> mapToUserResponse(user)).collect(Collectors.toList());

        CustomResponse successResponse = CustomResponse.builder()
                .status(HttpStatus.OK.name())
                .message("Successful")
                .data(userResponseList.isEmpty() ? null : userResponseList)
                .build();

        return ResponseEntity.ok(successResponse);
    }

    private UserResponseDto mapToUserResponse(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(new HashSet<>(user.getRoles()))
                .isEnabled(user.isEnabled())
                .build();
    }

    @Override
    public ResponseEntity<CustomResponse> registerUser(UserRequestDto request) {
        Optional<User> userOpt = userRepository.findUserByEmail(request.getEmail());

        if(userOpt.isPresent()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "User already exists"));
        }
        if(request == null){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Request body is required"));
        }
        if(request.getUsername() == null){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "firstName is required"));
        }

        if(request.getEmail() == null){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "email is required"));
        }
        if(!validateEmail(request.getEmail())){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "provide correct email format"));
        }
        if(request.getPassword() == null){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "password is required"));
        }

        Role role = roleRepository.findByName("ROLE_USER").get();
        User newUser = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .roles(Collections.singleton(role))
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userRepository.save(newUser);

        // Publish
        publisher.publishEvent(new RegistrationCompletePublisher(newUser, applicationUrl(servletRequest)));
        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK, "Successful! Kindly check your mail to verify your email address."));
    }

    private String applicationUrl(HttpServletRequest servletRequest) {
        return "http://"+servletRequest.getServerName()+":"+servletRequest.getServerPort()+servletRequest.getContextPath();
    }

    @Override
    public ResponseEntity<CustomResponse> resetPassword(String email) throws MessagingException, UnsupportedEncodingException {
        if(email == null){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "email is required"));
        }
        if(!validateEmail(email)){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "provide correct email format"));
        }
        Optional<User> userOpt = userRepository.findUserByEmail(email);
        if(userOpt.isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "No account found for this email"));
        }

        User user = userOpt.get();
        Integer token = theToken();
        emailService.sendResetPasswordEmail(token, user);
        redisTemplate.opsForHash().put(PASSWORD_RESET,email,token);
        redisTemplate.expire(PASSWORD_RESET, 5, TimeUnit.MINUTES);
        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK, "Kindly proceed to "+email+" to confirm your password reset"));
    }

    @Override
    public ResponseEntity<CustomResponse> confirmResetPassword(Integer token, ResetPasswordDto request) {
        Integer theToken = (Integer) redisTemplate.opsForHash().get(PASSWORD_RESET, request.getEmail());
        if(theToken != null && theToken.equals(token)){

            Long expirationTime = redisTemplate.getExpire(PASSWORD_RESET, TimeUnit.MINUTES);

            if (expirationTime != null && expirationTime <= 0) {
                return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Token has expired"));
            }

            if(request.getEmail() == null){
                return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "email is required"));
            }

            if(request.getNewPassword() == null){
                return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "new password is required"));
            }

            Optional<User> userOpt = userRepository.findUserByEmail(request.getEmail());
            if(userOpt.isEmpty()){
                return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "No account found for this email"));
            }

            User user = userOpt.get();

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));

            userRepository.save(user);

            return ResponseEntity.ok(new CustomResponse(HttpStatus.OK, "Your password has been successfully reset. Proceed to login"));
        }else{
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Invalid token"));
        }

    }

    @Override
    public ResponseEntity<CustomResponse> findUserByEmail(String email) {
        Optional<User> userOpt = userRepository.findUserByEmail(email);
        if(!userOpt.isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "No user with email address found"));
        }
        User user = userOpt.get();
        UserResponseDto response = UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(new HashSet<>(user.getRoles()))
                .isEnabled(user.isEnabled())
                .build();
        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK.name(), response, "Successful"));
    }

    @Override
    public ResponseEntity<CustomResponse> fetchUserById(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "No user found"));
        }
        User user = userOpt.get();
        UserResponseDto response = UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(new HashSet<>(user.getRoles()))
                .isEnabled(user.isEnabled())
                .build();
        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK.name(), response, "Successful"));
    }

    @Override
    public ResponseEntity<CustomResponse> updateProfile(Long userId, UserRequestDto request) {
        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "No user found"));
        }
        if(request.getEmail() != null){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "We're sorry, but you cannot change your email address"));
        }

        if(request.getPassword() != null){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "We're sorry, but you cannot change your password in this channel"));
        }
        User existingUser = userOpt.get();
        // Perform the partial update
        BeanUtils.copyProperties(request, existingUser, getNullPropertyNames(request));

        User updatedUser = userRepository.save(existingUser);

        if(updatedUser != null){
            return ResponseEntity.ok(new CustomResponse(HttpStatus.OK.name(), request, "Successfully updated profile"));
        }
        return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Something went wrong"));
    }

    @Override
    public ResponseEntity<CustomResponse> deleteProfile(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "No user found"));
        }

        User existingUser = userOpt.get();

        List<Role> roles = roleRepository.findByUsers(existingUser);

        for(Role role: roles){
            roleService.removeAllUserFromRole(role.getId());
        }

        userRepository.delete(existingUser);
        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK, "Successfully deleted profile"));
    }

    @Override
    public void saveVerificationToken(User theUser, String token) {
        var verificationToken = new VerificationToken(token, theUser);
        tokenRepository.save(verificationToken);
    }

    @Override
    public ResponseEntity<CustomResponse> verifyEmail(String token) {
        String url = applicationUrl(servletRequest)+"/api/v1/register/resend-token?token="+token;
        log.info("Resend link {} ", url);
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if(!tokenOpt.isPresent()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "No token found"));
        }
        VerificationToken theToken = tokenOpt.get();
        if(theToken.getUser().isEnabled()){
            return ResponseEntity.ok().body(new CustomResponse(HttpStatus.FOUND, "This user has already been verified, please login"));
        }
        String message = "<p> Link has expired.<a href=\"" + url + "\">Get a new verification link</a></p>";
        CustomResponse verificationResult = validateToken(token);
        if(verificationResult.getMessage().equalsIgnoreCase("Valid")){
            return ResponseEntity.ok().body(new CustomResponse(HttpStatus.OK, "Email verified successfully. Kindly proceed to login"));
        }
        CustomResponse response = new CustomResponse(HttpStatus.BAD_REQUEST, message);
        return ResponseEntity.badRequest().body(response);
    }


    public VerificationToken generateNewVerificationToken(String oldToken) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(oldToken);
        VerificationToken theToken = tokenOpt.get();
        var verificationTokenTime = new VerificationToken();
        theToken.setToken(UUID.randomUUID().toString());
        theToken.setExpirationTime(verificationTokenTime.getTokenExpirationTime());
        return  tokenRepository.save(theToken);
    }
    @Override
    public ResponseEntity<?> resendVerificationTokenEmail(String token) throws MessagingException, UnsupportedEncodingException {

        VerificationToken theToken = generateNewVerificationToken(token);
        User user = theToken.getUser();
        String url = applicationUrl(servletRequest)+"/api/v1/user/verify-email?token="+theToken.getToken();

        emailService.sendVerificationEmail(url, user);
        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK, "New verification link has been sent to your email and it will expire in 1min. Kindly check your email to activate your account"));
    }

    private CustomResponse validateToken(String token) {
            Optional<VerificationToken> tokeOpt = tokenRepository.findByToken(token);
            if(!tokeOpt.isPresent()){
                return new CustomResponse(HttpStatus.BAD_REQUEST, "Invalid verification token");
            }
            VerificationToken theToken = tokeOpt.get();

            User user = theToken.getUser();
            Calendar calendar = Calendar.getInstance();
            if((theToken.getExpirationTime().getTime()-calendar.getTime().getTime())<=0){
                //tokenRepository.delete(theToken);
                return new CustomResponse(HttpStatus.BAD_REQUEST, "Token has expired");
            }

            user.setEnabled(true);
            userRepository.save(user);

            return new CustomResponse(HttpStatus.OK, "Valid");
    }

    @Override
    public ResponseEntity<CustomResponse> changePassword(ChangePasswordDTO request) {
        if(request.getEmail() == null){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "email is required"));
        }
        if(!validateEmail(request.getEmail())){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "provide correct email format"));
        }
        if(request.getOldPassword() == null){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "oldPassword is required"));
        }
        if(request.getNewPassword() == null){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "newPassword is required"));
        }
        Optional<User> userOpt = userRepository.findUserByEmail(request.getEmail());

        if(userOpt.isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "No user is associated with this email"));
        }

        User user = userOpt.get();
        if(!passwordEncoder.matches(request.getOldPassword(), user.getPassword())){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Old password is not correct. Try again"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK, "Password has been successfully changed"));
    }

    @Override
    public ResponseEntity<CustomResponse> addAddress(Long userId, UserAddressRequest request) {
        Optional<User> userOpt = userRepository.findById(userId);
        if(!userOpt.isPresent()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "User not found"));
        }

        if(request.getPhoneNumber()==null || request.getPhoneNumber().isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Phone number is required"));
        }
        if(request.getStreet()==null || request.getStreet().isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Street is required"));
        }
        if(request.getCity()==null || request.getCity().isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "City is required"));
        }
        if(request.getState()==null || request.getState().isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "State is required"));
        }
        if(request.getCountry()==null || request.getCountry().isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Country is required"));
        }

        User user = userOpt.get();

        UserAddress address = UserAddress.builder()
                .city(request.getCity())
                .street(request.getStreet())
                .phoneNumber(request.getPhoneNumber())
                .user(user)
                .state(request.getState())
                .country(request.getCountry())
                .build();
        addressRepository.save(address);
        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK, "Successful"));
    }

    @Override
    public ResponseEntity<CustomResponse> uploadProfilePicture(Long userId, MultipartFile file) throws IOException {
        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "No user found"));
        }
        if(file.getSize() > 1048576){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Cannot upload file size that is more than 1mb"));
        }

        // Check the file extension
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        List<String> supportedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif");
        if (!supportedExtensions.contains(fileExtension)) {
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Invalid image file format! Supported formats: JPG, JPEG, PNG, GIF"));
        }
        User user = userOpt.get();

        String profileName = user.getFirstName()+"_avatar"+user.getId();
        String fileName = profileName + "." + fileExtension;

        BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
        if (bufferedImage == null) {
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Invalid image file!"));
        }
        String imagePath = IMAGE_FOLDER + fileName;
        ProfileAvatar profileAvater = ProfileAvatar.builder()
                .name(file.getOriginalFilename())
                .type(file.getContentType())
                .imagePath(imagePath)
                .user(user)
                .build();
        profileRepository.save(profileAvater);
        ImageIO.write(bufferedImage, fileExtension, new File(imagePath));
        if(profileAvater!=null){
            return ResponseEntity.ok(new CustomResponse(HttpStatus.OK, "Successfully uploaded profile picture"));
        }
        return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Error occurred while uploading profile"));
    }

    @Override
    public ResponseEntity<CustomResponse> fetchProfilePicture(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "No user found"));
        }
        User user = userOpt.get();
        ProfileAvatar avatar = profileRepository.findByUser(user).get();
        ProfileAvatarResponse response = ProfileAvatarResponse.builder()
                .imagePath(avatar.getImagePath()).build();
        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK.name(), response, "Successful"));
    }

    public static boolean validateEmail(String email) {
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    public static int theToken() {
        Random random = new Random();
        int min = 100000; // Minimum 6-digit number
        int max = 999999; // Maximum 6-digit number
        return random.nextInt(max - min + 1) + min;
    }

}
