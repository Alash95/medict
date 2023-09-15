package com.alash.medict.service;

import com.alash.medict.dto.request.UserRoleRequestDto;
import com.alash.medict.dto.response.CustomResponse;
import com.alash.medict.dto.response.UserResponseDto;
import com.alash.medict.exception.RoleAlreadyExistException;
import com.alash.medict.exception.UserAlreadyExistsException;
import com.alash.medict.model.Role;

import com.alash.medict.model.User;
import com.alash.medict.repository.IUserRepository;
import com.alash.medict.repository.RoleRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class RoleService implements IRoleService{
    private final RoleRepository roleRepository;
    private final IUserRepository userRepository;
    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public ResponseEntity<CustomResponse> createRole(UserRoleRequestDto request) {
        Optional<Role> roleOpt = roleRepository.findByName(request.getName());
        if(roleOpt.isPresent()){
            throw new RoleAlreadyExistException(roleOpt.get().getName()+ " role already exist!");
        }
        Role role = Role.builder()
                .name("ROLE_"+request.getName()).build();
        roleRepository.save(role);
        return ResponseEntity.ok().body(new CustomResponse(HttpStatus.CREATED.name(), request, "Successfully created role"));
    }

    @Override
    public ResponseEntity<CustomResponse> deleteRole(Long roleId) {
        this.removeAllUserFromRole(roleId);
        roleRepository.deleteById(roleId);
        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK, "Deleted successfully"));
    }

    @Override
    public ResponseEntity<CustomResponse> findByName(String name) {
        Optional<Role> roleOpt =roleRepository.findByName(name);
        if(roleOpt.isEmpty()){
            return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "No role found for this name"));
        }

        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK.name(), roleOpt.get(), "Successful"));
    }

    @Override
    public Role findById(Long roleId) {
        return roleRepository.findById(roleId).get();
    }

    @Override
    public ResponseEntity<CustomResponse> removeUserFromRole(Long userId, Long roleId) {
        Optional<User> user = userRepository.findById(userId);
        Optional<Role> role = roleRepository.findById(roleId);
        if(role.isPresent() && role.get().getUsers().contains(user.get())){
            role.get().removeUserFromRole(user.get());
            roleRepository.save(role.get());
            return ResponseEntity.ok(new CustomResponse(HttpStatus.OK.name(), role.get(), "Successfully removed user from role"));
        }
        return ResponseEntity.badRequest().body(new CustomResponse(HttpStatus.BAD_REQUEST, "Failed to remove user from role"));
    }

    @Override
    public ResponseEntity<CustomResponse> assignUserToRole(Long userId, Long roleId) {
        Optional<User> user = userRepository.findById(userId);
        Optional<Role> role = roleRepository.findById(roleId);
        if(user.isPresent() && user.get().getRoles().contains(role.get())){
            throw new UserAlreadyExistsException(user.get().getUsername() + " is already assigned to the "+ role.get().getName() + " role");
        }
        role.ifPresent(assignRole -> assignRole.assignUserToRole(user.get()));
        roleRepository.save(role.get());
        UserResponseDto responseDto = UserResponseDto.builder()
                .id(user.get().getId())
                .email(user.get().getEmail())
                .username(user.get().getUsername())
                .isEnabled(user.get().isEnabled())
                .role(Collections.singleton(role.get()))
                .build();
        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK.name(), responseDto, "Successfully assigned role to user"));
    }

    @Override
    public ResponseEntity<CustomResponse> removeAllUserFromRole(Long roleId) {
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        roleOpt.ifPresent(Role ::removeAllUsersFromRole);
        roleRepository.save(roleOpt.get());
        return ResponseEntity.ok(new CustomResponse(HttpStatus.OK.name(), roleOpt.get(), "Successfully removed all roles from user"));
    }
}
