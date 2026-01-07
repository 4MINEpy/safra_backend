package com.safra.safra.dto;

import com.safra.safra.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateDTO {
    private String name;
    private String phoneNumber;
    private Gender gender;
    private LocalDate birthDate;
    private String studentEmail;
}
