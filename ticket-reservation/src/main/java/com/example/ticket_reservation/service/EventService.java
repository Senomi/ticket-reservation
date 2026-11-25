package com.example.ticket_reservation.service;

import java.util.List;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.ticket_reservation.dto.EventRequest;
import com.example.ticket_reservation.dto.EventResponse;
import com.example.ticket_reservation.exception.ApiException;
import com.example.ticket_reservation.model.AppUser;
import com.example.ticket_reservation.model.Event;
import com.example.ticket_reservation.model.ReservationStatus;
import com.example.ticket_reservation.model.Role;
import com.example.ticket_reservation.repository.EventRepository;
import com.example.ticket_reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final AuthService authService;
    private final MongoTemplate mongoTemplate;

    public List<EventResponse> getAllEvents(String search) {
        List<Event> events = (search != null && !search.isBlank())
                ? eventRepository.findByNameContainingIgnoreCase(search)
                : eventRepository.findAll();
        return events.stream().map(this::toResponse).toList();
    }

    public EventResponse getEventById(String id) {
        return toResponse(getEventEntity(id));
    }

    public Event getEventEntity(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    public EventResponse createEvent(EventRequest request, String username) {
        AppUser organizer = authService.getUserEntity(username);
        Event event = Event.builder()
                .name(request.name())
                .description(request.description())
                .venue(request.venue())
                .eventDate(request.eventDate())
                .totalSeats(request.totalSeats())
                .availableSeats(request.totalSeats())
                .ticketPrice(request.ticketPrice())
                .organizerId(organizer.getId())
                .organizerUsername(organizer.getUsername())
                .build();
        return toResponse(eventRepository.save(event));
    }

    public EventResponse updateEvent(String id, EventRequest request, String username) {
        Event event = getEventEntity(id);
        AppUser currentUser = authService.getUserEntity(username);

        if (currentUser.getRole() != Role.ADMIN && !event.getOrganizerId().equals(currentUser.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Organizers can only edit their own events");
        }

        int bookedSeats = event.getTotalSeats() - event.getAvailableSeats();
        if (request.totalSeats() < bookedSeats) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Total seats cannot be less than already booked seats (" + bookedSeats + ")");
        }

        event.setName(request.name());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setEventDate(request.eventDate());
        event.setTotalSeats(request.totalSeats());
        event.setAvailableSeats(request.totalSeats() - bookedSeats);
        event.setTicketPrice(request.ticketPrice());

        return toResponse(eventRepository.save(event));
    }

    public void deleteEvent(String id) {
        Event event = getEventEntity(id);
        if (reservationRepository.existsByEventIdAndStatus(id, ReservationStatus.CONFIRMED)) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete event with active confirmed reservations");
        }
        eventRepository.delete(event);
    }

    // Atomic decrement to prevent double-booking
    public Event reserveSeatsAtomically(String eventId, int seats) {
        Query query = new Query(Criteria.where("_id").is(eventId).and("availableSeats").gte(seats));
        Update update = new Update().inc("availableSeats", -seats);
        Event updated = mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), Event.class
        );
        if (updated == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Not enough available seats or event does not exist");
        }
        return updated;
    }

    // Atomic increment on cancellation
    public void releaseSeatsAtomically(String eventId, int seats) {
        Query query = new Query(Criteria.where("_id").is(eventId));
        Update update = new Update().inc("availableSeats", seats);
        mongoTemplate.updateFirst(query, update, Event.class);
    }

    private EventResponse toResponse(Event e) {
        return new EventResponse(
                e.getId(), e.getName(), e.getDescription(), e.getVenue(), e.getEventDate(),
                e.getTotalSeats(), e.getAvailableSeats(), e.getTicketPrice(),
                e.getOrganizerId(), e.getOrganizerUsername()
        );
    }
}