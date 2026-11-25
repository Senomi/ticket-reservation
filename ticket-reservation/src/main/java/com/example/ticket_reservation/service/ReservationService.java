package com.example.ticket_reservation.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.ticket_reservation.dto.ReservationRequest;
import com.example.ticket_reservation.dto.ReservationResponse;
import com.example.ticket_reservation.exception.ApiException;
import com.example.ticket_reservation.model.AppUser;
import com.example.ticket_reservation.model.Event;
import com.example.ticket_reservation.model.Reservation;
import com.example.ticket_reservation.model.ReservationStatus;
import com.example.ticket_reservation.model.Role;
import com.example.ticket_reservation.repository.EventRepository;
import com.example.ticket_reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final AuthService authService;

    public ReservationResponse createReservation(ReservationRequest request, String username) {
        AppUser customer = authService.getUserEntity(username);
        Event event = eventService.reserveSeatsAtomically(request.eventId(), request.seats());

        String code = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Reservation reservation = Reservation.builder()
                .reservationCode(code)
                .eventId(event.getId())
                .eventName(event.getName())
                .customerId(customer.getId())
                .customerUsername(customer.getUsername())
                .seats(request.seats())
                .totalAmount(event.getTicketPrice() * request.seats())
                .status(ReservationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    public List<ReservationResponse> getMyReservations(String username) {
        AppUser customer = authService.getUserEntity(username);
        return reservationRepository.findByCustomerId(customer.getId()).stream()
                .map(this::toResponse).toList();
    }

    public List<ReservationResponse> getReservationsForRole(String username) {
        AppUser user = authService.getUserEntity(username);
        if (user.getRole() == Role.ADMIN) {
            return reservationRepository.findAll().stream().map(this::toResponse).toList();
        }
        // ORGANIZER: return reservations only for events owned by this organizer
        List<String> myEventIds = eventRepository.findByOrganizerId(user.getId()).stream()
                .map(Event::getId).toList();
        return reservationRepository.findByEventIdIn(myEventIds).stream()
                .map(this::toResponse).toList();
    }

    public ReservationResponse cancelReservation(String id, String username) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reservation not found"));

        AppUser currentUser = authService.getUserEntity(username);

        if (currentUser.getRole() != Role.ADMIN && !reservation.getCustomerId().equals(currentUser.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Customers can only cancel their own reservations");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        eventService.releaseSeatsAtomically(reservation.getEventId(), reservation.getSeats());
        return toResponse(reservationRepository.save(reservation));
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(), r.getReservationCode(), r.getEventId(), r.getEventName(),
                r.getCustomerId(), r.getCustomerUsername(), r.getSeats(),
                r.getTotalAmount(), r.getStatus(), r.getCreatedAt()
        );
    }
}