package com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;
    private String newPassword;
}
