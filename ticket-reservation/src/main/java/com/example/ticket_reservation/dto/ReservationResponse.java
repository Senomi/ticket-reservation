package com.example.ticket_reservation.dto;

import java.time.LocalDateTime;

import com.example.ticket_reservation.model.ReservationStatus;

public record ReservationResponse(
    String id,
    String reservationCode,
    String eventId,
    String eventName,
    String customerId,
    String customerUsername,
    int seats,
    double totalAmount,
    ReservationStatus status,
    LocalDateTime createdAt
) {}