package com.alash.medict.repository;

import com.alash.medict.model.Role;
import com.alash.medict.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    List<Role> findByUsers(User user);
}
