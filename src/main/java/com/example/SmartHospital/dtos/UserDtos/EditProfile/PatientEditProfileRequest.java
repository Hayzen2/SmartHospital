package com.example.SmartHospital.dtos.UserDtos.EditProfile;
import java.time.LocalDate;

import com.example.SmartHospital.enums.GenderType;
import com.example.SmartHospital.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientEditProfileRequest {
    private String email;
    private String fullName;
    private String phoneNumber;
    private String identityNumber;
    private GenderType gender;
    private LocalDate dateOfBirth;
    private String address;
    private UserStatus status;
    private String insuranceNumber;
    private String insuranceProvider;
}
