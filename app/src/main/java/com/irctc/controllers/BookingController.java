package com.irctc.controllers;

import com.irctc.entities.Train;
import com.irctc.entities.User;
import com.irctc.entities.Ticket;
import com.irctc.services.AuthSessionService;
import com.irctc.services.TrainService;
import com.irctc.services.UserBookingService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "https://railwaygo.vercel.app"
})
public class BookingController {

    private final AuthSessionService authSessionService;

    public BookingController(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    // --------------------------------------------------
    // 1. BOOK A SEAT
    // --------------------------------------------------

    @PostMapping
    public Map<String, Object> bookSeat(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @RequestBody BookingRequest request) {

        String authenticatedUserId =
                getAuthenticatedUserId(authorizationHeader);

        if (request == null
                || request.getTrainId() == null
                || request.getTrainId().isBlank()
                || request.getRow() < 0
                || request.getSeat() < 0
                || request.getSource() == null
                || request.getSource().isBlank()
                || request.getDestination() == null
                || request.getDestination().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Train ID, row, seat, source and destination are required."
            );
        }

        String travelDate = request.getTravelDate();

        if (travelDate == null || travelDate.isBlank()) {
            travelDate = LocalDate.now().toString();
        }

        LocalDate parsedDate;

        try {
            parsedDate = LocalDate.parse(travelDate);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Travel date must use YYYY-MM-DD format."
            );
        }

        if (parsedDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Travel date cannot be in the past."
            );
        }

        try {
            UserBookingService bookingService =
                    new UserBookingService();

            User user = bookingService.findUserById(
                    authenticatedUserId
            );

            if (user == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user no longer exists."
                );
            }

            TrainService trainService = new TrainService();

            Train train = trainService.getAllTrains()
                    .stream()
                    .filter(t ->
                            t.getTrainId() != null
                                    && t.getTrainId().equalsIgnoreCase(
                                    request.getTrainId().trim()))
                    .findFirst()
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Train not found."
                            ));

            double fare;

            try {
                fare = bookingService.calculateFare(
                        train,
                        request.getSource(),
                        request.getDestination()
                );
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        ex.getMessage()
                );
            }

            bookingService.setCurrentUser(user);

            boolean booked = bookingService.bookTrainSeat(
                    train,
                    request.getRow(),
                    request.getSeat(),
                    request.getSource(),
                    request.getDestination(),
                    parsedDate.toString()
            );

            if (!booked) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Seat is unavailable or booking failed."
                );
            }

            // The fare returned here is calculated by the backend.
            return Map.of(
                    "status", "success",
                    "message", "Seat booked successfully.",
                    "trainId", train.getTrainId(),
                    "source", request.getSource().trim(),
                    "destination", request.getDestination().trim(),
                    "travelDate", parsedDate.toString(),
                    "row", request.getRow(),
                    "seat", request.getSeat(),
                    "fare", fare,
                    "currency", "INR"
            );

        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to process booking.",
                    ex
            );
        }
    }

    // --------------------------------------------------
    // 2. GET AUTHENTICATED USER'S BOOKING HISTORY
    // --------------------------------------------------

    @GetMapping("/user")
    public List<Ticket> getUserBookings(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader) {

        String authenticatedUserId =
                getAuthenticatedUserId(authorizationHeader);

        try {
            UserBookingService bookingService =
                    new UserBookingService();

            User user = bookingService.findUserById(
                    authenticatedUserId
            );

            if (user == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user no longer exists."
                );
            }

            List<Ticket> tickets = user.getTicketBooked();

            return tickets != null ? tickets : List.of();

        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve booking history.",
                    ex
            );
        }
    }

    // --------------------------------------------------
    // 3. CANCEL AUTHENTICATED USER'S TICKET
    // --------------------------------------------------

    @DeleteMapping("/{ticketId}")
    public Map<String, String> cancelTicket(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @PathVariable String ticketId) {

        String authenticatedUserId =
                getAuthenticatedUserId(authorizationHeader);

        if (ticketId == null || ticketId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket ID is required."
            );
        }

        try {
            UserBookingService bookingService =
                    new UserBookingService();

            User user = bookingService.findUserById(
                    authenticatedUserId
            );

            if (user == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user no longer exists."
                );
            }

            boolean ownsTicket = user.getTicketBooked() != null
                    && user.getTicketBooked().stream()
                    .anyMatch(ticket ->
                            ticketId.equals(ticket.getTicketId()));

            if (!ownsTicket) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found."
                );
            }

            bookingService.setCurrentUser(user);

            boolean cancelled =
                    bookingService.cancelBooking(ticketId);

            if (!cancelled) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ticket cancellation failed."
                );
            }

            return Map.of(
                    "status", "success",
                    "message", "Ticket cancelled successfully.",
                    "ticketId", ticketId
            );

        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to cancel ticket.",
                    ex
            );
        }
    }

    // --------------------------------------------------
    // AUTHENTICATION HELPER
    // --------------------------------------------------

    private String getAuthenticatedUserId(
            String authorizationHeader) {

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication token is required."
            );
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication token is required."
            );
        }

        String userId = authSessionService.validateSession(token);

        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired authentication token."
            );
        }

        return userId;
    }

    // --------------------------------------------------
    // BOOKING REQUEST
    // --------------------------------------------------

    public static class BookingRequest {

        private String trainId;
        private int row;
        private int seat;
        private String source;
        private String destination;
        private String travelDate;

        public BookingRequest() {
        }

        public String getTrainId() {
            return trainId;
        }

        public void setTrainId(String trainId) {
            this.trainId = trainId;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getSeat() {
            return seat;
        }

        public void setSeat(int seat) {
            this.seat = seat;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getTravelDate() {
            return travelDate;
        }

        public void setTravelDate(String travelDate) {
            this.travelDate = travelDate;
        }
    }
}