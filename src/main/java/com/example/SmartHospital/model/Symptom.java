package com.example.SmartHospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class Symptom {
    @Column(nullable = false)
    private String symptomName;

    @Column(nullable = false)
    private boolean present;

    @Column
    private String timing;
}
