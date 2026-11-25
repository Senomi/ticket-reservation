package com.example.ticket_reservation.dto;

import com.example.ticket_reservation.model.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Username is required") String username,
    @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password,
    @NotBlank(message = "Full name is required") String fullName,
    Role role
) {}