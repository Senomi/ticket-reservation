package com.example.ticket_reservation.config;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.ticket_reservation.model.AppUser;
import com.example.ticket_reservation.model.Event;
import com.example.ticket_reservation.model.Role;
import com.example.ticket_reservation.repository.EventRepository;
import com.example.ticket_reservation.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            AppUser admin = AppUser.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator")
                    .role(Role.ADMIN)
                    .build();

            AppUser organizer = AppUser.builder()
                    .username("organizer")
                    .password(passwordEncoder.encode("organizer123"))
                    .fullName("Default Organizer")
                    .role(Role.ORGANIZER)
                    .build();

            AppUser customer = AppUser.builder()
                    .username("customer")
                    .password(passwordEncoder.encode("customer123"))
                    .fullName("Default Customer")
                    .role(Role.CUSTOMER)
                    .build();

            userRepository.saveAll(List.of(admin, organizer, customer));

            if (eventRepository.count() == 0) {
                Event sampleEvent = Event.builder()
                        .name("Annual Tech Symposium 2026")
                        .description("Seeded sample event")
                        .venue("Kelaniya Auditorium")
                        .eventDate(LocalDateTime.of(2026, 12, 20, 10, 0))
                        .totalSeats(100)
                        .availableSeats(100)
                        .ticketPrice(1500.0)
                        .organizerId(organizer.getId())
                        .organizerUsername(organizer.getUsername())
                        .build();
                eventRepository.save(sampleEvent);
            }
        }
    }
}