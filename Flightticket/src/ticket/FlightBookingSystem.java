
package ticket;

import java.sql.*;
import java.util.Scanner;

public class FlightBookingSystem {
    private static final String DB_URL = "jdbc:mysql://localhost/flight_booking";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "santhosh";
    
    private static final int SEATS_PER_FLIGHT = 60;
    
    private Connection connection;
    private User currentUser;
    
    public FlightBookingSystem() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void login(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM users WHERE username = ? AND password = ?"
            );
            statement.setString(1, username);
            statement.setString(2, password);
            
            ResultSet result = statement.executeQuery();
            
            if (result.next()) {
                int userId = result.getInt("id");
                boolean isAdmin = result.getBoolean("is_admin");
                currentUser = new User(userId, username, isAdmin);
                System.out.println("Login successful!");
                if (isAdmin) {
                    adminMenu(scanner);
                } else {
                    userMenu(scanner);
                }
            } else {
                System.out.println("Invalid username or password.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void signup(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        try {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)"
            );
            statement.setString(1, username);
            statement.setString(2, password);
            
            int rowsInserted = statement.executeUpdate();
            
            if (rowsInserted > 0) {
                System.out.println("Signup successful! Please login to continue.");
            } else {
                System.out.println("Signup failed. Please try again.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void userMenu(Scanner scanner) {
        while (true) {
            System.out.println("User Menu");
            System.out.println("---------");
            System.out.println("1. Search flights");
            System.out.println("2. Book a flight");
            System.out.println("3. My Bookings");
            System.out.println("4. Logout");
            System.out.print("Enter your choice: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); 
            
            switch (choice) {
                case 1:
                    searchFlights(scanner);
                    break;
                case 2:
                    bookFlight(scanner);
                    break;
                case 3:
                    viewBookings();
                    break;
                case 4:
                    currentUser = null;
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
            
            System.out.println();
        }
    }
    
    private void searchFlights(Scanner scanner) {
        System.out.print("Enter the date (YYYY-MM-DD): ");
        String date = scanner.nextLine();
        
        System.out.print("Enter the time (HH:MM): ");
        String time = scanner.nextLine();
        
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM flights WHERE DATE(departure_datetime) = ? AND TIME(departure_datetime) >= ?"
            );
            statement.setString(1, date);
            statement.setString(2, time);
            
            ResultSet result = statement.executeQuery();
            
            System.out.println("Available flights:");
            
            while (result.next()) {
                int flightId = result.getInt("id");
                String flightNumber = result.getString("flight_number");
                String departureDatetime = result.getString("departure_datetime");
                int seatsAvailable = result.getInt("seats_available");
                
                System.out.println("Flight ID: " + flightId);
                System.out.println("Flight Number: " + flightNumber);
                System.out.println("Departure Datetime: " + departureDatetime);
                System.out.println("Seats Available: " + seatsAvailable);
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void bookFlight(Scanner scanner) {
        System.out.print("Enter the Flight ID: ");
        int flightId = scanner.nextInt();
        
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM flights WHERE id = ?"
            );
            statement.setInt(1, flightId);
            
            ResultSet result = statement.executeQuery();
            
            if (result.next()) {
                int seatsAvailable = result.getInt("seats_available");
                
                if (seatsAvailable > 0) {
                    PreparedStatement bookStatement = connection.prepareStatement(
                        "INSERT INTO bookings (user_id, flight_id, booking_datetime) VALUES (?, ?, NOW())"
                    );
                    bookStatement.setInt(1, currentUser.getId());
                    bookStatement.setInt(2, flightId);
                    
                    int rowsInserted = bookStatement.executeUpdate();
                    
                    if (rowsInserted > 0) {
                        PreparedStatement updateStatement = connection.prepareStatement(
                            "UPDATE flights SET seats_available = seats_available - 1 WHERE id = ?"
                        );
                        updateStatement.setInt(1, flightId);
                        updateStatement.executeUpdate();
                        
                        System.out.println("Flight booked successfully!");
                    } else {
                        System.out.println("Failed to book the flight. Please try again.");
                    }
                } else {
                    System.out.println("No seats available on the selected flight.");
                }
            } else {
                System.out.println("Invalid Flight ID.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void viewBookings() {
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bookings WHERE user_id = ?"
            );
            statement.setInt(1, currentUser.getId());
            
            ResultSet result = statement.executeQuery();
            
            System.out.println("Your bookings:");
            
            while (result.next()) {
                int bookingId = result.getInt("id");
                int flightId = result.getInt("flight_id");
                String bookingDatetime = result.getString("booking_datetime");
                
                System.out.println("Booking ID: " + bookingId);
                System.out.println("Flight ID: " + flightId);
                System.out.println("Booking Datetime: " + bookingDatetime);
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void adminMenu(Scanner scanner) {
        while (true) {
            System.out.println("Admin Menu");
            System.out.println("----------");
            System.out.println("1. Add Flight");
            System.out.println("2. Remove Flight");
            System.out.println("3. View Bookings");
            System.out.println("4. Logout");
            System.out.print("Enter your choice: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); 
            
            switch (choice) {
                case 1:
                    addFlight(scanner);
                    break;
                case 2:
                    removeFlight(scanner);
                    break;
                case 3:
                    viewBookingsAdmin(scanner);
                    break;
                case 4:
                    currentUser = null;
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
            
            System.out.println();
        }
    }
    
    private void addFlight(Scanner scanner) {
        System.out.print("Enter the Flight Number: ");
        String flightNumber = scanner.nextLine();
        
        System.out.print("Enter the Departure Datetime (YYYY-MM-DD HH:MM): ");
        String departureDatetime = scanner.nextLine();
        
        try {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO flights (flight_number, departure_datetime) VALUES (?, ?)"
            );
            statement.setString(1, flightNumber);
            statement.setString(2, departureDatetime);
            
            int rowsInserted = statement.executeUpdate();
            
            if (rowsInserted > 0) {
                System.out.println("Flight added successfully!");
            } else {
                System.out.println("Failed to add the flight. Please try again.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void removeFlight(Scanner scanner) {
        System.out.print("Enter the Flight ID: ");
        int flightId = scanner.nextInt();
        
        try {
            PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM flights WHERE id = ?"
            );
            statement.setInt(1, flightId);
            
            int rowsDeleted = statement.executeUpdate();
            
            if (rowsDeleted > 0) {
                System.out.println("Flight removed successfully!");
            } else {
                System.out.println("Failed to remove the flight. Please try again.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void viewBookingsAdmin(Scanner scanner) {
        System.out.print("Enter the Flight Number: ");
        String flightNumber = scanner.nextLine();
        
        System.out.print("Enter the Departure Datetime (YYYY-MM-DD HH:MM): ");
        String departureDatetime = scanner.nextLine();
        
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT bookings.id, bookings.user_id, bookings.flight_id, bookings.booking_datetime " +
                "FROM bookings " +
                "JOIN flights ON bookings.flight_id = flights.id " +
                "WHERE flights.flight_number = ? AND flights.departure_datetime = ?"
            );
            statement.setString(1, flightNumber);
            statement.setString(2, departureDatetime);
            
            ResultSet result = statement.executeQuery();
            
            System.out.println("Bookings for Flight " + flightNumber + " on " + departureDatetime);
            
            while (result.next()) {
                int bookingId = result.getInt("id");
                int userId = result.getInt("user_id");
                int flightId = result.getInt("flight_id");
                String bookingDatetime = result.getString("booking_datetime");
                
                System.out.println("Booking ID: " + bookingId);
                System.out.println("User ID: " + userId);
                System.out.println("Flight ID: " + flightId);
                System.out.println("Booking Datetime: " + bookingDatetime);
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
