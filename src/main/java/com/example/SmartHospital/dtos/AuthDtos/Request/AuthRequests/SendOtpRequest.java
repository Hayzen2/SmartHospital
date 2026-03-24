package com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendOtpRequest {
    @NotBlank
    private String email;
}