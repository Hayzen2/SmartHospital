package com.example.SmartHospital.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "admin")
@EqualsAndHashCode(callSuper = false)
public class Admin extends User {
}
