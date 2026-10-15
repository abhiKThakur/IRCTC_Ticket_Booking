package com.irctc;

import com.irctc.Util.UserServiceUtil;
import com.irctc.entities.Train;
import com.irctc.entities.User;
import com.irctc.services.UserBookingService;

import java.io.IOException;
import java.util.*;

public class Main {

    static void printHeader(String title) {
        System.out.println("\n╔══════════════════════════════════╗");
        System.out.printf( "║  %-32s║%n", title);
        System.out.println("╚══════════════════════════════════╝");
    }

    static void printMenu() {
        System.out.println("\n╔══════════════════════════════════╗");
        System.out.println("║       IRCTC BOOKING SYSTEM       ║");
        System.out.println("╠══════════════════════════════════╣");
        System.out.println("║  1.  Sign Up                     ║");
        System.out.println("║  2.  Login                       ║");
        System.out.println("║  3.  Fetch My Bookings           ║");
        System.out.println("║  4.  Search Trains               ║");
        System.out.println("║  5.  Book a Seat                 ║");
        System.out.println("║  6.  Cancel Booking              ║");
        System.out.println("║  7.  Exit                        ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.print("👉 Enter your choice: ");
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        int option = 0;
        UserBookingService userBookingService;

        try {
            userBookingService = new UserBookingService();
        } catch (IOException ex) {
            System.out.println("❌ Error loading data. Please try again.");
            return;
        }

        Train trainSelectedForBooking = new Train();

        while (option != 7) {
            printMenu();
            option = scanner.nextInt();

            switch (option) {
                case 1:
                    printHeader("SIGN UP");
                    System.out.print("👤 Enter username : ");
                    String nameToSignUp = scanner.next();
                    System.out.print("🔒 Enter password : ");
                    String passwordToSignUp = scanner.next();
                    User userToSignup = new User(nameToSignUp, passwordToSignUp,
                            UserServiceUtil.hashPassword(passwordToSignUp),
                            new ArrayList<>(), UUID.randomUUID().toString());
                    userBookingService.signUp(userToSignup);
                    System.out.println("✅ Sign up successful! Please login.");
                    break;

                case 2:
                    printHeader("LOGIN");
                    System.out.print("👤 Enter username : ");
                    String nameToLogin = scanner.next();
                    System.out.print("🔒 Enter password : ");
                    String passwordToLogin = scanner.next();
                    User userToLogin = new User(nameToLogin, passwordToLogin,
                            UserServiceUtil.hashPassword(passwordToLogin),
                            new ArrayList<>(), UUID.randomUUID().toString());
                    try {
                        userBookingService = new UserBookingService(userToLogin);
                        // ✅ Load the actual user from file
                        User loggedInUser = userBookingService.loginUser();
                        if (loggedInUser != null) {
                            userBookingService.setCurrentUser(loggedInUser);
                            System.out.println("✅ Login successful! Welcome " + nameToLogin + "!");
                        } else {
                            System.out.println("❌ Invalid username or password!");
                        }
                    } catch (IOException ex) {
                        System.out.println("❌ Login failed. Try again.");
                        return;
                    }
                    break;

                case 3:
                    printHeader("MY BOOKINGS");
                    userBookingService.fetchBooking();
                    break;

                case 4:
                    printHeader("SEARCH TRAINS");
                    System.out.print("🚉 Enter source station      : ");
                    String source = scanner.next();
                    System.out.print("🏁 Enter destination station : ");
                    String dest = scanner.next();
                    List<Train> trains = userBookingService.getTrains(source, dest);

                    if (trains.isEmpty()) {
                        System.out.println("❌ No trains found for this route.");
                        break;
                    }

                    System.out.println("\n╔══════════════════════════════════╗");
                    System.out.println("║         AVAILABLE TRAINS         ║");
                    System.out.println("╠══════════════════════════════════╣");
                    int index = 1;
                    for (Train t : trains) {
                        System.out.println("║  " + index + ". Train ID : " + t.getTrainId());
                        for (Map.Entry<String, String> entry : t.getStationTimes().entrySet()) {
                            System.out.printf("║     %-12s : %s%n", entry.getKey(), entry.getValue());
                        }
                        System.out.println("║");
                        index++;
                    }
                    System.out.println("╚══════════════════════════════════╝");
                    System.out.print("👉 Select train number: ");
                    trainSelectedForBooking = trains.get(scanner.nextInt() - 1);
                    System.out.println("✅ Train selected!");
                    break;

                case 5:
                    printHeader("BOOK A SEAT");
                    List<List<Integer>> seats = userBookingService.fetchSeats(trainSelectedForBooking);
                    System.out.println("0 = Available  |  1 = Booked\n");
                    int rowNum = 0;
                    for (List<Integer> row : seats) {
                        System.out.print("Row " + rowNum + " : ");
                        for (Integer val : row) {
                            System.out.print("[" + (val == 0 ? "✅" : "❌") + "] ");
                        }
                        System.out.println();
                        rowNum++;
                    }
                    System.out.print("\n👉 Enter row number    : ");
                    int row = scanner.nextInt();
                    System.out.print("👉 Enter seat number   : ");
                    int col = scanner.nextInt();
                    boolean booked = userBookingService.bookTrainSeat(trainSelectedForBooking, row, col);
                    if (booked) {
                        System.out.println("✅ Seat booked! Enjoy your journey! 🚂");
                    } else {
                        System.out.println("❌ Could not book this seat. Try another.");
                    }
                    break;

                case 6:
                    printHeader("CANCEL BOOKING");
                    System.out.println("📋 Your current bookings:\n");
                    userBookingService.fetchBooking();
                    System.out.print("\n👉 Enter Ticket ID to cancel: ");
                    String ticketId = scanner.next();
                    boolean cancelled = userBookingService.cancelBooking(ticketId);
                    if (cancelled) {
                        System.out.println("✅ Booking cancelled successfully!");
                    } else {
                        System.out.println("❌ No booking found with that ID!");
                    }
                    break;

                case 7:
                    System.out.println("\n👋 Thank you for using IRCTC. Goodbye!");
                    break;

                default:
                    System.out.println("❌ Invalid option! Please enter 1-7.");
                    break;
            }
        }
    }
}