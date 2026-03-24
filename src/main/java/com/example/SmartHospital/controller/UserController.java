package com.example.SmartHospital.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.example.SmartHospital.dtos.UserDtos.EditProfile.DoctorEditProfileRequest;
import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.PaginatedResponse;
import com.example.SmartHospital.dtos.UserDtos.DoctorDTO;
import com.example.SmartHospital.dtos.UserDtos.EditProfile.PatientEditProfileRequest;
import com.example.SmartHospital.dtos.UserDtos.PatientDTO;
import com.example.SmartHospital.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getPatients")
    @Operation(summary = "Get paginated list of patients with optional search") // Swagger documentation
    public ResponseEntity<ApiResponse<PaginatedResponse<PatientDTO>>> getPatients(
        @Parameter(description = "Page number (0-indexed)") 
        @RequestParam(defaultValue = "0") int pageNumber,
        
        @Parameter(description = "Page size (max 100)") 
        @RequestParam(defaultValue = "10") int pageSize,
        
        @Parameter(description = "Search by name, email, phone, or identity number") 
        @RequestParam(required = false) String search) {
        
        try{
            PaginatedResponse<PatientDTO> response = userService.getPatients(pageNumber, pageSize, search);
            return ResponseEntity.ok(new ApiResponse<>(200, "Successfully retrieved patients", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to get patients", null));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getDoctors")
    @Operation(summary = "Get paginated list of doctors with optional search")
    public ResponseEntity<ApiResponse<PaginatedResponse<DoctorDTO>>> getDoctors(
            @Parameter(description = "Page number (0-indexed)") 
            @RequestParam(defaultValue = "0") int pageNumber,
            
            @Parameter(description = "Page size (max 100)") 
            @RequestParam(defaultValue = "10") int pageSize,
            
            @Parameter(description = "Search by name, email, phone, or identity number") 
            @RequestParam(required = false) String search) {
        
        try{
            PaginatedResponse<DoctorDTO> response = userService.getDoctors(pageNumber, pageSize, search);
            return ResponseEntity.ok(new ApiResponse<>(200, "Successfully retrieved doctors", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to get doctors", null));
        }

    }

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/patient/user-profile/edit")
    public ResponseEntity<ApiResponse<PatientDTO>> editPatientProfile(@RequestBody PatientEditProfileRequest request, @AuthenticationPrincipal String userId, @RequestParam(required = false)  MultipartFile avatarFile) {
        try {
            PatientDTO response = userService.editPatientProfile(request, userId, avatarFile);
            return ResponseEntity.ok(new ApiResponse<>(200, "Successfully edited user profile", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to edit patient profile", null));
        }
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/doctor/user-profile/edit")
    public ResponseEntity<ApiResponse<DoctorDTO>> editDoctorProfile(@RequestBody DoctorEditProfileRequest request, @AuthenticationPrincipal String userId, @RequestParam(required = false)  MultipartFile avatarFile) {
        try {
            DoctorDTO response = userService.editDoctorProfile(request, userId, avatarFile);
            return ResponseEntity.ok(new ApiResponse<>(200, "Successfully edited user profile", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to edit doctor profile", null));
        }
    }

    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @GetMapping("/user-profile/view")
    public ResponseEntity<ApiResponse<Object>> viewUserProfile(@AuthenticationPrincipal String userId,
                                                                Authentication authentication
    ) {
        try {
            boolean isPatient = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_PATIENT"));
            if (isPatient) {
                PatientDTO response = userService.getPatientById(userId);
                return ResponseEntity.ok(new ApiResponse<>(200, "Successfully retrieved patient profile", response));
            } else {
                DoctorDTO response = userService.getDoctorById(userId);
                return ResponseEntity.ok(new ApiResponse<>(200, "Successfully retrieved doctor profile", response));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to retrieve user profile", null));
        }
    }
}