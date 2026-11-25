package com.example.ticket_reservation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticket_reservation.dto.ReservationRequest;
import com.example.ticket_reservation.dto.ReservationResponse;
import com.example.ticket_reservation.service.ReservationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(request, auth.getName()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ReservationResponse>> getMy(Authentication auth) {
        return ResponseEntity.ok(reservationService.getMyReservations(auth.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<List<ReservationResponse>> getAll(Authentication auth) {
        return ResponseEntity.ok(reservationService.getReservationsForRole(auth.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(reservationService.cancelReservation(id, auth.getName()));
    }
}