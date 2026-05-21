package com.lawapp.backend.dto;

import com.lawapp.backend.model.Role;
import lombok.Data;

@Data
public class SignupRequest {
    private String fullName;
    private String email;
    private String password;
    private Role role;
    private String phoneNumber;
}
