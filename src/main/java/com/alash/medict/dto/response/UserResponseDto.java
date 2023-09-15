package com.alash.medict.dto.response;

import com.alash.medict.model.Role;
import com.alash.medict.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;
    import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private Set<Role> role;
    private boolean isEnabled;
}
