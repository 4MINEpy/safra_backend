package com.safra.safra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEmailVerificationDTO {
    private Long userId;
    private String studentEmail;
}
