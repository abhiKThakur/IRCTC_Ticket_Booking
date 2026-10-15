package com.irctc.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irctc.Util.UserServiceUtil;
import com.irctc.entities.Ticket;
import com.irctc.entities.Train;
import com.irctc.entities.User;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserBookingService {

    private User user;
    private List<User> userList;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String USER_PATH =
            "src/main/java/com/irctc/localDB/users.json";

    // Sample project fare, not an official railway fare.
    private static final double FARE_PER_SEGMENT = 150.0;

    public UserBookingService(User user1) throws IOException {
        this.user = user1;
        this.userList = loadUsers();
    }

    public UserBookingService() throws IOException {
        this.userList = loadUsers();
    }

    // --------------------------------------------------
    // LOAD USERS
    // --------------------------------------------------

    public synchronized List<User> loadUsers() throws IOException {

        File usersFile = new File(USER_PATH);

        if (!usersFile.exists() || usersFile.length() == 0) {
            return new ArrayList<>();
        }

        return objectMapper.readValue(
                usersFile,
                new TypeReference<List<User>>() {}
        );
    }

    // --------------------------------------------------
    // REGISTER USER
    // --------------------------------------------------

    public synchronized boolean signUp(User newUser) {

        if (newUser == null
                || newUser.getName() == null
                || newUser.getName().isBlank()
                || newUser.getEmail() == null
                || newUser.getEmail().isBlank()
                || newUser.getPassword() == null
                || newUser.getPassword().length() < 8) {

            return false;
        }

        String email = newUser.getEmail().trim().toLowerCase();

        try {
            userList = loadUsers();

            boolean emailExists = userList.stream()
                    .anyMatch(existingUser ->
                            existingUser.getEmail() != null
                                    && existingUser.getEmail()
                                    .trim()
                                    .equalsIgnoreCase(email));

            if (emailExists) {
                return false;
            }

            String hashedPassword =
                    UserServiceUtil.hashPassword(newUser.getPassword());

            newUser.setEmail(email);
            newUser.setHashPassword(hashedPassword);

            if (newUser.getUserId() == null
                    || newUser.getUserId().isBlank()) {

                newUser.setUserId(UUID.randomUUID().toString());
            }

            if (newUser.getTicketBooked() == null) {
                newUser.setTicketBooked(new ArrayList<>());
            }

            // Never persist a plain-text password.
            newUser.setPassword(null);

            userList.add(newUser);

            saveUserListToFile();

            return true;

        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // --------------------------------------------------
    // LOGIN USER
    // --------------------------------------------------

    public synchronized User login(String email, String password) {

        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {

            return null;
        }

        try {
            userList = loadUsers();

            Optional<User> foundUser = userList.stream()
                    .filter(existingUser ->
                            existingUser.getEmail() != null
                                    && existingUser.getEmail()
                                    .equalsIgnoreCase(email.trim()))
                    .filter(existingUser ->
                            existingUser.getHashPassword() != null
                                    && UserServiceUtil.checkPassword(
                                    password,
                                    existingUser.getHashPassword()
                            ))
                    .findFirst();

            if (foundUser.isEmpty()) {
                return null;
            }

            this.user = foundUser.get();

            return this.user;

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // Preserve the existing CLI login method.
    public User loginUser() {

        if (user == null) {
            return null;
        }

        return login(user.getEmail(), user.getPassword());
    }

    // --------------------------------------------------
    // SAVE USERS
    // --------------------------------------------------

    private synchronized void saveUserListToFile() throws IOException {

        File usersFile = new File(USER_PATH);

        File parentDirectory = usersFile.getParentFile();

        if (parentDirectory != null && !parentDirectory.exists()) {
            if (!parentDirectory.mkdirs()) {
                throw new IOException(
                        "Could not create user data directory."
                );
            }
        }

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(usersFile, userList);
    }

    // --------------------------------------------------
    // BOOKING HISTORY
    // --------------------------------------------------

    public void fetchBooking() {

        if (user != null) {
            user.printTicket();
        }
    }

    public synchronized List<Ticket> getBookingHistory(String userId) {

        User requestedUser = findUserById(userId);

        if (requestedUser == null
                || requestedUser.getTicketBooked() == null) {

            return new ArrayList<>();
        }

        return new ArrayList<>(requestedUser.getTicketBooked());
    }

    // --------------------------------------------------
    // CANCEL BOOKING
    // --------------------------------------------------

    public synchronized Boolean cancelBooking(String ticketId) {

        if (ticketId == null || ticketId.isBlank() || user == null) {
            return false;
        }

        List<Ticket> tickets = user.getTicketBooked();

        if (tickets == null) {
            return false;
        }

        Ticket ticketToCancel = tickets.stream()
                .filter(ticket ->
                        ticketId.equals(ticket.getTicketId()))
                .findFirst()
                .orElse(null);

        if (ticketToCancel == null || ticketToCancel.getTrain() == null) {
            return false;
        }

        try {
            TrainService trainService = new TrainService();

            Train bookedTrain = trainService.getAllTrains()
                    .stream()
                    .filter(train ->
                            train.getTrainId() != null
                                    && train.getTrainId().equalsIgnoreCase(
                                    ticketToCancel.getTrain().getTrainId()))
                    .findFirst()
                    .orElse(null);

            if (bookedTrain == null || bookedTrain.getSeats() == null) {
                return false;
            }

            int row = ticketToCancel.getSeatRow();
            int seat = ticketToCancel.getSeatNumber();

            List<List<Integer>> seats = bookedTrain.getSeats();

            if (row < 0 || row >= seats.size()
                    || seat < 0 || seat >= seats.get(row).size()) {
                return false;
            }

            if (seats.get(row).get(seat) != 1) {
                return false;
            }

            // Release the occupied seat.
            seats.get(row).set(seat, 0);
            bookedTrain.setSeats(seats);

            int userIndex = findUserIndex(user.getUserId());

            if (userIndex == -1) {
                return false;
            }

            tickets.remove(ticketToCancel);

            userList.set(userIndex, user);

            trainService.addTrain(bookedTrain);
            saveUserListToFile();

            return true;

        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // --------------------------------------------------
    // SEARCH TRAINS
    // --------------------------------------------------

    public List<Train> getTrains(String source, String destination) {

        try {
            TrainService trainService = new TrainService();

            return trainService.searchTrains(source, destination);

        } catch (IOException ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<List<Integer>> fetchSeats(Train train) {

        if (train == null || train.getSeats() == null) {
            return new ArrayList<>();
        }

        return train.getSeats();
    }

    // --------------------------------------------------
    // FARE CALCULATION
    // --------------------------------------------------

    /**
     * Calculates a sample project fare using the number of
     * station-to-station segments in the selected journey.
     *
     * Example:
     * Bangalore -> Jaipur -> Delhi
     *
     * Bangalore to Jaipur = 1 segment = Rs. 150
     * Bangalore to Delhi  = 2 segments = Rs. 300
     *
     * These are illustrative project fares, not official
     * Indian Railways fares.
     */
    public double calculateFare(
            Train train,
            String source,
            String destination
    ) {

        if (train == null
                || train.getStations() == null
                || source == null
                || source.isBlank()
                || destination == null
                || destination.isBlank()) {

            throw new IllegalArgumentException(
                    "Train, source and destination are required."
            );
        }

        List<String> stations = train.getStations();

        int sourceIndex = -1;
        int destinationIndex = -1;

        for (int i = 0; i < stations.size(); i++) {

            String station = stations.get(i);

            if (station == null) {
                continue;
            }

            if (station.trim().equalsIgnoreCase(source.trim())) {
                sourceIndex = i;
            }

            if (station.trim().equalsIgnoreCase(destination.trim())) {
                destinationIndex = i;
            }
        }

        if (sourceIndex == -1 || destinationIndex == -1) {
            throw new IllegalArgumentException(
                    "Source or destination is not on this train's route."
            );
        }

        if (sourceIndex >= destinationIndex) {
            throw new IllegalArgumentException(
                    "Destination must appear after source on the train's route."
            );
        }

        int segments = destinationIndex - sourceIndex;

        return segments * FARE_PER_SEGMENT;
    }

    // --------------------------------------------------
    // BOOK TRAIN SEAT - BACKWARD COMPATIBILITY
    // --------------------------------------------------

    /**
     * Preserves existing callers.
     *
     * Until the controller passes the selected journey,
     * this method books the complete train route.
     */
    public synchronized Boolean bookTrainSeat(
            Train train,
            int row,
            int seat
    ) {

        if (train == null
                || train.getStations() == null
                || train.getStations().size() < 2) {

            return false;
        }

        List<String> stations = train.getStations();

        String source = stations.get(0);
        String destination = stations.get(stations.size() - 1);

        return bookTrainSeat(
                train,
                row,
                seat,
                source,
                destination,
                LocalDate.now().toString()
        );
    }

    // --------------------------------------------------
    // BOOK TRAIN SEAT - SELECTED JOURNEY
    // --------------------------------------------------

    public synchronized Boolean bookTrainSeat(
            Train train,
            int row,
            int seat,
            String source,
            String destination,
            String travelDate
    ) {

        if (user == null
                || train == null
                || train.getTrainId() == null
                || train.getSeats() == null
                || source == null
                || source.isBlank()
                || destination == null
                || destination.isBlank()
                || travelDate == null
                || travelDate.isBlank()) {

            return false;
        }

        // Validate the date format.
        try {
            LocalDate.parse(travelDate);
        } catch (RuntimeException ex) {
            return false;
        }

        // Do not allow a past travel date.
        LocalDate journeyDate = LocalDate.parse(travelDate);

        if (journeyDate.isBefore(LocalDate.now())) {
            return false;
        }

        // Validate the journey and calculate its fare before
        // changing the seat matrix.
        final double fare;

        try {
            fare = calculateFare(train, source, destination);
        } catch (IllegalArgumentException ex) {
            return false;
        }

        if (row < 0 || row >= train.getSeats().size()
                || seat < 0 || seat >= train.getSeats().get(row).size()) {

            return false;
        }

        if (user.getTicketBooked() == null) {
            user.setTicketBooked(new ArrayList<>());
        }

        try {
            TrainService trainService = new TrainService();

            // Retrieve the latest train data.
            Train currentTrain = trainService.getAllTrains()
                    .stream()
                    .filter(existingTrain ->
                            existingTrain.getTrainId() != null
                                    && existingTrain.getTrainId()
                                    .equalsIgnoreCase(train.getTrainId()))
                    .findFirst()
                    .orElse(null);

            if (currentTrain == null
                    || currentTrain.getSeats() == null
                    || currentTrain.getStations() == null) {

                return false;
            }

            List<List<Integer>> currentSeats = currentTrain.getSeats();

            if (row < 0 || row >= currentSeats.size()
                    || seat < 0 || seat >= currentSeats.get(row).size()) {

                return false;
            }

            // Make sure the selected journey is valid on the
            // latest train data as well.
            try {
                calculateFare(currentTrain, source, destination);
            } catch (IllegalArgumentException ex) {
                return false;
            }

            // Check current seat availability.
            if (currentSeats.get(row).get(seat) != 0) {
                return false;
            }

            // Mark the seat as occupied.
            currentSeats.get(row).set(seat, 1);
            currentTrain.setSeats(currentSeats);

            // Create a ticket for the selected journey.
            Ticket ticket = new Ticket(
                    UUID.randomUUID().toString(),
                    user.getUserId(),
                    source.trim(),
                    destination.trim(),
                    journeyDate.toString(),
                    currentTrain
            );

            ticket.setSeatRow(row);
            ticket.setSeatNumber(seat);
            ticket.setFare(fare);

            user.getTicketBooked().add(ticket);

            int userIndex = findUserIndex(user.getUserId());

            if (userIndex == -1) {
                user.getTicketBooked().remove(ticket);
                return false;
            }

            userList.set(userIndex, user);

            trainService.addTrain(currentTrain);
            saveUserListToFile();

            return true;

        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // --------------------------------------------------
    // CURRENT USER
    // --------------------------------------------------

    public void setCurrentUser(User user) {
        this.user = user;
    }

    public User findUserById(String userId) {

        if (userId == null || userId.isBlank()) {
            return null;
        }

        return userList.stream()
                .filter(existingUser ->
                        userId.equals(existingUser.getUserId()))
                .findFirst()
                .orElse(null);
    }

    public User findUserByEmail(String email) {

        if (email == null || email.isBlank()) {
            return null;
        }

        return userList.stream()
                .filter(existingUser ->
                        existingUser.getEmail() != null
                                && existingUser.getEmail()
                                .equalsIgnoreCase(email.trim()))
                .findFirst()
                .orElse(null);
    }

    private int findUserIndex(String userId) {

        if (userId == null) {
            return -1;
        }

        for (int i = 0; i < userList.size(); i++) {

            User existingUser = userList.get(i);

            if (userId.equals(existingUser.getUserId())) {
                return i;
            }
        }

        return -1;
    }
}