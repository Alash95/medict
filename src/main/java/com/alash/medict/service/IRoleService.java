package com.alash.medict.service;


import com.alash.medict.dto.request.UserRoleRequestDto;
import com.alash.medict.dto.response.CustomResponse;
import com.alash.medict.model.Role;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface IRoleService {
    List<Role> getAllRoles();

    ResponseEntity<CustomResponse> createRole(UserRoleRequestDto request);

    ResponseEntity<CustomResponse> deleteRole(Long roleId);

    ResponseEntity<CustomResponse> findByName(String name);

    Role findById(Long roleId);

    ResponseEntity<CustomResponse> removeUserFromRole(Long userId, Long roleId);

    ResponseEntity<CustomResponse> assignUserToRole(Long userId, Long roleId);

    ResponseEntity<CustomResponse> removeAllUserFromRole(Long roleId);
}
