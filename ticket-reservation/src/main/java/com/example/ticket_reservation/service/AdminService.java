package com.example.ticket_reservation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.ticket_reservation.dto.StatsResponse;
import com.example.ticket_reservation.model.Reservation;
import com.example.ticket_reservation.model.ReservationStatus;
import com.example.ticket_reservation.repository.EventRepository;
import com.example.ticket_reservation.repository.ReservationRepository;
import com.example.ticket_reservation.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;

    public StatsResponse getStats() {
        List<Reservation> confirmed = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .toList();

        long ticketsSold = confirmed.stream().mapToLong(Reservation::getSeats).sum();
        double revenue = confirmed.stream().mapToDouble(Reservation::getTotalAmount).sum();

        return new StatsResponse(
                userRepository.count(),
                eventRepository.count(),
                confirmed.size(),
                ticketsSold,
                revenue
        );
    }
}