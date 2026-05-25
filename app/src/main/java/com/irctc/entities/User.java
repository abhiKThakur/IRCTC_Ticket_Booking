package com.irctc.entities;
import  java.util.*;

public class User {
    private String name;
    private String password;
    private String hashPassword;
    private List<Ticket> ticketBooked;
    private String userId;

    public User(String name, String password, String hashPassword, List<Ticket> ticketBooked, String userId) {
        this.name = name;
        this.password = password;
        this.hashPassword = hashPassword;
        this.ticketBooked = ticketBooked;
        this.userId = userId;
    }

    public User() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHashPassword() {
        return hashPassword;
    }

    public void setHashPassword(String hashPassword) {
        this.hashPassword = hashPassword;
    }

    public List<Ticket> getTicketBooked() {
        return ticketBooked;
    }

    public void setTicketBooked(List<Ticket> ticketBooked) {
        this.ticketBooked = ticketBooked;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public  void printTicket(){
        for (int i = 0; i<ticketBooked.size();i++){
            System.out.println(ticketBooked.get(i).getTicketInfo());
        }
    }
}
