package com.example.SmartHospital.dtos.UserDtos;

import com.example.SmartHospital.enums.GenderType;
import com.example.SmartHospital.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor // Default constructor
@AllArgsConstructor // All arguments constructor
public class PatientDTO {
    private String id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String identityNumber;
    private GenderType gender;
    private LocalDate dateOfBirth;
    private String address;
    private String avatarPath;
    private UserStatus status;
    private String insuranceNumber;
    private String insuranceProvider;
}
