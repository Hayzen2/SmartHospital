package com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerificationRequest {
    @NotBlank
    private String otp;
    @NotBlank
    private String token;
}