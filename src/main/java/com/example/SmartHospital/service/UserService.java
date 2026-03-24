package com.example.SmartHospital.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests.RegisterRequest;
import com.example.SmartHospital.dtos.PaginatedResponse;
import com.example.SmartHospital.dtos.UserDtos.DoctorDTO;
import com.example.SmartHospital.dtos.UserDtos.EditProfile.DoctorEditProfileRequest;
import com.example.SmartHospital.dtos.UserDtos.EditProfile.PatientEditProfileRequest;
import com.example.SmartHospital.dtos.UserDtos.PatientDTO;
import com.example.SmartHospital.enums.RoleType;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.Doctor;
import com.example.SmartHospital.model.MedicalRecord;
import com.example.SmartHospital.model.Patient;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.DoctorRepository;
import com.example.SmartHospital.repository.PatientRepository;
import com.example.SmartHospital.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public void updateLastLogin(String email) {
        userRepository.updateLastLogin(email);
    }

    public User registerUser(RegisterRequest registerRequest) {

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        } 
        if (userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists");
        }
        if(userRepository.existsByIdentityNumber(registerRequest.getIdentityNumber())) {
            throw new IllegalArgumentException("Identity number already exists");
        }
        if(patientRepository.existsByInsuranceNumber(registerRequest.getInsuranceNumber())) {
            throw new IllegalArgumentException("Insurance number already exists");
        }

        //check phone number is it valid or not
        if(!registerRequest.getPhoneNumber().matches("\\d{10,15}")) {
            throw new IllegalArgumentException("Invalid phone number");
        }
        //check identity number is it valid or not
        if(!registerRequest.getIdentityNumber().matches("\\w{5,20}")) {
            throw new IllegalArgumentException("Invalid identity number");
        }
        // check insurance number is it valid or not
        if(registerRequest.getInsuranceNumber() != null && 
           !registerRequest.getInsuranceNumber().matches("\\w{5,20}")) {
            throw new IllegalArgumentException("Invalid insurance number");
        }
        //check if email is valid or not
        if(!registerRequest.getEmail().matches("^[A-Za-z0-9+_.%-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        //check if password is strong enough, 1 uppercase, 1 lowercase, 1 digit, 1 special character, at least 8 characters
        if(registerRequest.getPassword().length() < 8 ||
           !registerRequest.getPassword().matches(".*[A-Z].*") ||
           !registerRequest.getPassword().matches(".*[a-z].*") ||
           !registerRequest.getPassword().matches(".*\\d.*") ||
           !registerRequest.getPassword().matches(".*[!@#$%^&*()].*")) {
            throw new IllegalArgumentException("Password is not strong enough! 1 uppercase, 1 lowercase, 1 digit, 1 special character, at least 8 characters");
        }

        Patient patient = new Patient();
        patient.setEmail(registerRequest.getEmail());
        patient.setHashedPassword(passwordEncoder.encode(registerRequest.getPassword()));
        patient.setFullName(registerRequest.getName());
        patient.setPhoneNumber(registerRequest.getPhoneNumber());
        patient.setIdentityNumber(registerRequest.getIdentityNumber());
        patient.setGender(registerRequest.getGender());
        patient.setDateOfBirth(registerRequest.getDateOfBirth());
        patient.setAddress(registerRequest.getAddress());
        patient.setAvatarPath(registerRequest.getAvatarPath());
        patient.setStatus(UserStatus.ACTIVE);
        patient.setRole(RoleType.PATIENT);
        patient.setInsuranceNumber(registerRequest.getInsuranceNumber());
        patient.setInsuranceProvider(registerRequest.getInsuranceProvider());

        List<MedicalRecord> medicalRecords = new ArrayList<>();
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setPatient(patient);
        medicalRecords.add(medicalRecord);
        
        patient.setMedicalRecords(medicalRecords);

        return patientRepository.save(patient);
    }


    // Paginated retrieval of patients with optional search
    public PaginatedResponse<PatientDTO> getPatients(int pageNumber, int pageSize, String search) {
        // Validate pagination params
        if (pageNumber < 0) {
            pageNumber = 0;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        if (pageSize > 10) {
            pageSize = 10; // Max 10 patients per page
        }
   
        // Create Pageable object
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        // Search or get all
        Page<Patient> patientPage;
        if (search != null && !search.trim().isEmpty()) {
            patientPage = patientRepository.searchPatients(search.trim(), pageable);
        } else {
            patientPage = patientRepository.findAll(pageable);
        }

        // Convert to DTO 
        List<PatientDTO> content = patientPage.getContent().stream()
            .map(this::convertToPatientDTO)
            .collect(Collectors.toList());

        return new PaginatedResponse<>(
            content,
            patientPage.getNumber(),
            patientPage.getSize(),
            patientPage.getTotalElements(),
            patientPage.getTotalPages(),
            patientPage.isLast()
        );
    }

    private PatientDTO convertToPatientDTO(Patient patient) {
        PatientDTO dto = new PatientDTO();
        dto.setEmail(patient.getEmail());
        dto.setFullName(patient.getFullName());
        dto.setPhoneNumber(patient.getPhoneNumber());
        dto.setIdentityNumber(patient.getIdentityNumber());
        dto.setGender(patient.getGender());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setAddress(patient.getAddress());
        dto.setAvatarPath(patient.getAvatarPath());
        dto.setStatus(patient.getStatus());
        dto.setInsuranceNumber(patient.getInsuranceNumber());
        dto.setInsuranceProvider(patient.getInsuranceProvider());
        return dto;
    }
    public PatientDTO getPatientById(String id) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient == null) {
            return null;
        }
        return convertToPatientDTO(patient);
    }
    public PatientDTO editPatientProfile(PatientEditProfileRequest request, String userId, MultipartFile avatarFile) {
        Patient patient = patientRepository.findById(userId).orElse(null);
        if (patient == null) {
            return null;
        }
        patient.setFullName(request.getFullName());
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setAddress(request.getAddress());
        patient.setAvatarPath(saveAvatarFile(avatarFile));
        patient.setInsuranceNumber(request.getInsuranceNumber());
        patient.setInsuranceProvider(request.getInsuranceProvider());
        patientRepository.save(patient);
        return convertToPatientDTO(patient);
    }

    // Similar method can be created for doctors
    public PaginatedResponse<DoctorDTO> getDoctors(int pageNumber, int pageSize, String search) {
        // Validate pagination params
        if (pageNumber < 0) {
            pageNumber = 0;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        if (pageSize > 10) {
            pageSize = 10; // Max 10 patients per page
        }
   
        // Create Pageable object
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        // Search or get all
        Page<Doctor> doctorPage;
        if (search != null && !search.trim().isEmpty()) {
            doctorPage = doctorRepository.searchDoctors(search.trim(), pageable);
        } else {
            doctorPage = doctorRepository.findAll(pageable);
        }

        // Convert to DTO
        List<DoctorDTO> content = doctorPage.getContent().stream()
            .map(this::convertToDoctorDTO)
            .collect(Collectors.toList());

        return new PaginatedResponse<>(
            content,
            doctorPage.getNumber(),
            doctorPage.getSize(),
            doctorPage.getTotalElements(),
            doctorPage.getTotalPages(),
            doctorPage.isLast()
        );
    }

    private DoctorDTO convertToDoctorDTO(Doctor doctor) {
        DoctorDTO dto = new DoctorDTO();
        dto.setId(doctor.getId());
        dto.setEmail(doctor.getEmail());
        dto.setFullName(doctor.getFullName());
        dto.setPhoneNumber(doctor.getPhoneNumber());
        dto.setIdentityNumber(doctor.getIdentityNumber());
        dto.setGender(doctor.getGender());
        dto.setDateOfBirth(doctor.getDateOfBirth());
        dto.setAddress(doctor.getAddress());
        dto.setAvatarPath(doctor.getAvatarPath());
        dto.setStatus(doctor.getStatus());
        return dto;
    }

    public DoctorDTO getDoctorById(String id) {
        Doctor doctor = doctorRepository.findById(id).orElse(null);
        if (doctor == null) {
            return null;
        }
        return convertToDoctorDTO(doctor);
    }

    public DoctorDTO editDoctorProfile(DoctorEditProfileRequest request, String userId, MultipartFile avatarFile) {
        Doctor doctor = doctorRepository.findById(userId).orElse(null);
        if (doctor == null) {
            return null;
        }
        doctor.setFullName(request.getFullName());
        doctor.setPhoneNumber(request.getPhoneNumber());
        doctor.setDateOfBirth(request.getDateOfBirth());
        doctor.setAddress(request.getAddress());
        doctor.setAvatarPath(saveAvatarFile(avatarFile));
        doctor.setWorkingHours(request.getWorkingHours());
        doctor.setAvailabilityStatus(request.getAvailabilityStatus());
        doctor.setSpecialization(request.getSpecialization());
        doctorRepository.save(doctor);
        return convertToDoctorDTO(doctor);
    }

    private String saveAvatarFile(MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            return null;
        }
        // Implement file saving logic here, e.g., save to local storage or cloud storage
        // Return the path or URL of the saved avatar
        return "path/to/saved/avatar.jpg"; // Placeholder
    }
}

