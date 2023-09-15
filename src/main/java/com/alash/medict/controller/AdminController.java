package com.alash.medict.controller;

import com.alash.medict.dto.request.UserRoleRequestDto;
import com.alash.medict.dto.response.CustomResponse;
import com.alash.medict.dto.response.UserResponseDto;
import com.alash.medict.model.Role;
import com.alash.medict.service.IRoleService;
import com.alash.medict.service.IUserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "admin")
public class AdminController {
    private final IRoleService roleService;
    private final IUserService userService;

    @Operation(
            summary = "fetch all users",
            description = "This endpoint fetches all users from database",
            responses = {
                    @ApiResponse(responseCode = "200",
                    description = "Success",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDto.class))
                    ),

                    @ApiResponse(responseCode = "204",
                            description = "NO content",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomResponse.class))
                    ),
                    @ApiResponse(responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomResponse.class))
                    )
            }
    )
    @GetMapping("/users")
    public ResponseEntity<CustomResponse> getUser(){
        return userService.getAllUsers();
    }

    @Hidden
    @GetMapping("/all-roles")
    public ResponseEntity<List<Role>> getAllRoles(){
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @Operation(
            summary = "Create roles",
            description = "This endpoint allow authenticated admin to crate roles for users",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Role.class))
                    ),
                    @ApiResponse(responseCode = "204",
                            description = "NO content",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomResponse.class))
                    ),
                    @ApiResponse(responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomResponse.class))
                    )
            }
    )
    @PostMapping("/create-role")
    public ResponseEntity<CustomResponse> createRole(@RequestBody UserRoleRequestDto request){
        return roleService.createRole(request);
    }

    @PostMapping("/remove-all-users-from-role/{id}")
    public ResponseEntity<CustomResponse> removeUserAllUsersFromRole(@PathVariable("id") Long roleId){
        return roleService.removeAllUserFromRole(roleId);
    }

    @PostMapping("/remove-user-from-role")
    public ResponseEntity<CustomResponse> removeSingleUserFromRole(@RequestParam(name = "userId") Long userId, @RequestParam(name = "roleId")Long roleId){
        return roleService.removeUserFromRole(userId, roleId);
    }

    @PostMapping("/assign-user-to-role")
    public ResponseEntity<CustomResponse> assignUserToRole(@RequestParam(name = "userId") Long userId, @RequestParam(name = "roleId")Long roleId){
        return roleService.assignUserToRole(userId, roleId);
    }

    @DeleteMapping("/delete-role/{id}")
    public ResponseEntity<CustomResponse> deleteRole (@PathVariable("id") Long roleId){
        return roleService.deleteRole(roleId);
    }
}
