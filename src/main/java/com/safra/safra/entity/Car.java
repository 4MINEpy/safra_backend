package com.safra.safra.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name="cars")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Car {
    @Id
    @GeneratedValue
    private Long id;
    @Column
    private String registrationNumber;
    @Column
    private String brand;
    @Column 
    private String model;
    @Column
    private String color;

    @OneToOne
    @JoinColumn(name = "owner", nullable = false)
    @JsonBackReference
    private User owner;

}
