package com.irctc.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Ticket {

    private String ticketId;
    private String userId;
    private String source;
    private String destination;
    private String dateOfTravel;
    private Train train;

    // Booked seat position
    private int seatRow;
    private int seatNumber;

    // Ticket fare in Indian Rupees
    private double fare;

    // Parameterized constructor
    public Ticket(
            String ticketId,
            String userId,
            String source,
            String destination,
            String dateOfTravel,
            Train train) {

        this.ticketId = ticketId;
        this.userId = userId;
        this.source = source;
        this.destination = destination;
        this.dateOfTravel = dateOfTravel;
        this.train = train;
    }

    // Default constructor required by Jackson
    public Ticket() {
    }

    @JsonIgnore
    public String getTicketInfo() {
        return String.format(
                "Ticket ID: %s | From: %s -> %s | Date: %s",
                ticketId,
                source,
                destination,
                dateOfTravel
        );
    }

    // Ticket ID
    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    // User ID
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Source station
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    // Destination station
    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    // Travel date
    public String getDateOfTravel() {
        return dateOfTravel;
    }

    public void setDateOfTravel(String dateOfTravel) {
        this.dateOfTravel = dateOfTravel;
    }

    // Train
    public Train getTrain() {
        return train;
    }

    public void setTrain(Train train) {
        this.train = train;
    }

    // Seat row
    public int getSeatRow() {
        return seatRow;
    }

    public void setSeatRow(int seatRow) {
        this.seatRow = seatRow;
    }

    // Seat number
    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    // Ticket fare
    public double getFare() {
        return fare;
    }

    public void setFare(double fare) {
        if (!Double.isFinite(fare) || fare < 0) {
            throw new IllegalArgumentException(
                    "Fare must be a finite, non-negative amount."
            );
        }

        this.fare = fare;
    }
}