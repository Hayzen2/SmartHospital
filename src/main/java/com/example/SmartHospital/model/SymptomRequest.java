package com.example.SmartHospital.model;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "symptom_request")
public class SymptomRequest {
    @Id
    private String requestId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @ElementCollection
    @CollectionTable(
        name = "symptom_request_symptom",
        joinColumns = @JoinColumn(name = "request_id")
    )
    private List<Symptom> symptoms;

    @Column(nullable = true)
    private String duration;
    @Column(nullable = true)
    private String otherIssues;
}
