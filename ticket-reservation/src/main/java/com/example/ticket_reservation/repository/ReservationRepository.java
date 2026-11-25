package com.example.ticket_reservation.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.ticket_reservation.model.Reservation;
import com.example.ticket_reservation.model.ReservationStatus;

public interface ReservationRepository extends MongoRepository<Reservation, String> {
    List<Reservation> findByCustomerId(String customerId);
    List<Reservation> findByEventId(String eventId);
    List<Reservation> findByEventIdIn(List<String> eventIds);
    boolean existsByEventIdAndStatus(String eventId, ReservationStatus status);
}