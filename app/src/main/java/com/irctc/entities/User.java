package com.irctc.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class User {

    private String name;
    private String email;

    @JsonIgnore
    private String password;

    private String hashPassword;

    private List<Ticket> ticketBooked = new ArrayList<>();

    private String userId;

    public User() {
    }

    public User(
            String name,
            String password,
            String hashPassword,
            List<Ticket> ticketBooked,
            String userId
    ) {
        this.name = name;
        this.password = password;
        this.hashPassword = hashPassword;
        this.ticketBooked = ticketBooked != null
                ? ticketBooked
                : new ArrayList<>();
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @JsonProperty
    public void setPassword(String password) {
        this.password = password;
    }

    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    public String getHashPassword() {
        return hashPassword;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public void setHashPassword(String hashPassword) {
        this.hashPassword = hashPassword;
    }

    public List<Ticket> getTicketBooked() {
        return ticketBooked;
    }

    public void setTicketBooked(List<Ticket> ticketBooked) {
        this.ticketBooked = ticketBooked != null
                ? ticketBooked
                : new ArrayList<>();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void printTicket() {
        if (ticketBooked == null || ticketBooked.isEmpty()) {
            System.out.println("No bookings found!");
            return;
        }

        for (Ticket ticket : ticketBooked) {
            System.out.println(ticket.getTicketInfo());
        }
    }
}