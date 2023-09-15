package com.alash.medict.config;

import com.alash.medict.model.Role;
import com.alash.medict.model.User;
import com.alash.medict.repository.IUserRepository;
import com.alash.medict.repository.RoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;



@Configuration

public class BeanConfig {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

//    @Bean
//    public CommandLineRunner createDefaultUser(PlatformTransactionManager transactionManager) {
//        return args -> {
//            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
//
//            transactionTemplate.execute(status -> {
//                if (userRepository.findUserByEmail("oyinlolaalasho95@gmail.com").isEmpty()) {
//                    Role role = roleRepository.findByName("ROLE_ADMIN").orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found"));
//
//                    User newUser = User.builder()
//                            .email("oyinlolaalasho95@gmail.com")
//                            .username("Alash95")
//                            .enabled(true)
//                            .roles(Collections.singleton(role))
//                            .password(passwordEncoder().encode("admin"))
//                            .build();
//
//                    entityManager.persist(role); // Save the UserRole entity
//                    userRepository.save(newUser); // Save the UserEntity entity
//                }
//                return null;
//            });
//        };
//    }
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate getRestTemplate() throws Exception {
        return new RestTemplate();
    }

}
