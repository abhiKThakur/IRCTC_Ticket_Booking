# RailwayGo — Train Ticket Booking System

RailwayGo is a train ticket booking application with a React frontend and a Java Spring Boot backend. It supports account registration and login, train search, seat selection, ticket booking, fare calculation, booking history, and ticket cancellation.

> **Project/demo note:** The application currently stores train and user/booking data in local JSON files. The fare calculation is sample pricing of **₹150 per route segment**, not an official Indian Railways fare.

## Features

- User signup and login
- Password hashing on registration
- Server-side authentication sessions using bearer tokens
- Search trains by source and destination
- View available seat data for a train
- Book a seat for a selected journey and travel date
- Calculate a sample fare from the number of route segments
- View the signed-in user's booking history
- Cancel a ticket
- Validate authentication for booking-related endpoints
- Prevent one signed-in user from viewing or cancelling another user's tickets

## Technology Stack

### Frontend
- React
- Vite
- JavaScript
- CSS

### Backend
- Java
- Spring Boot
- Gradle
- JSON file-based data storage

## Project Structure

```text
IRCTC_Ticket_Booking/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/irctc/
│       │   ├── IrctcApiApplication.java
│       │   ├── Main.java
│       │   ├── config/CorsConfig.java
│       │   ├── controllers/
│       │   │   ├── AuthController.java
│       │   │   ├── BookingController.java
│       │   │   └── TrainController.java
│       │   ├── entities/
│       │   │   ├── Ticket.java
│       │   │   ├── Train.java
│       │   │   └── User.java
│       │   ├── localDB/
│       │   │   ├── trains.json
│       │   │   └── users.json
│       │   ├── services/
│       │   │   ├── AuthSessionService.java
│       │   │   ├── TrainService.java
│       │   │   └── UserBookingService.java
│       │   └── Util/UserServiceUtil.java
│       └── resources/
├── frontend/
│   ├── src/
│   │   ├── App.jsx
│   │   ├── main.jsx
│   │   └── styles.css
│   ├── index.html
│   ├── package.json
│   └── package-lock.json
├── build.gradle
├── settings.gradle
└── README.md
```

Generated folders such as `app/build`, `frontend/dist`, `frontend/node_modules`, `.gradle`, and IDE-specific `.idea` files are omitted from this overview.

## Prerequisites

Install:
- Java (the project was tested with Java 25 in the development environment)
- Gradle, or a working Gradle Wrapper
- Node.js and npm

If the Gradle Wrapper distribution download fails in your environment, use an installed Gradle version as shown below.

## Run the Backend

Open a terminal at the project root:

```powershell
gradle :app:bootRun
```

The Spring Boot API is expected to run on:

```text
http://localhost:8080
```

To build the backend without starting it:

```powershell
gradle :app:build
```

## Run the Frontend

Open a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Vite prints the local development URL in the terminal. The backend CORS configuration permits these development origins:
- `http://localhost:5173`
- `http://localhost:5174`

If Vite starts on a different port, update the backend CORS configuration to permit that origin.

## Build the Frontend for Production

Run from the frontend directory:

```powershell
npm run build
```

The generated static site is written to `frontend/dist/`.

## API Reference

Base URL: `http://localhost:8080`

### Train endpoints

| Method | Endpoint | Purpose | Authentication |
|---|---|---|---|
| GET | `/api/trains/health` | Check whether the train API is running | Not required |
| GET | `/api/trains?source={source}&destination={destination}` | Search trains by route | Not required |
| GET | `/api/trains/all` | Return all trains | Not required |
| GET | `/api/trains/{trainId}/seats` | Get a train's seat matrix | Not required |

Example:

```text
GET http://localhost:8080/api/trains?source=bangalore&destination=delhi
```

### Authentication endpoints

| Method | Endpoint | Purpose | Authentication |
|---|---|---|---|
| POST | `/api/auth/signup` | Create an account | Not required |
| POST | `/api/auth/login` | Log in and receive a bearer token | Not required |
| POST | `/api/auth/logout` | Invalidate the current session token | Bearer token required |

Signup request:

```json
{
  "name": "Example User",
  "email": "user@example.com",
  "password": "password123"
}
```

Login request:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Signup requires a name, email, and password of at least eight characters. A duplicate email is rejected.

For authenticated requests, include the token returned by login:

```text
Authorization: Bearer YOUR_TOKEN
```

### Booking endpoints

| Method | Endpoint | Purpose | Authentication |
|---|---|---|---|
| POST | `/api/bookings` | Book a seat | Bearer token required |
| GET | `/api/bookings/user` | Get the signed-in user's booking history | Bearer token required |
| DELETE | `/api/bookings/{ticketId}` | Cancel a ticket owned by the signed-in user | Bearer token required |

Booking request:

```json
{
  "trainId": "TRAIN_ID",
  "row": 0,
  "seat": 0,
  "source": "bangalore",
  "destination": "delhi",
  "travelDate": "2027-01-15"
}
```

Use a real `trainId` from the train data. Row and seat indexes are zero-based. The travel date uses `YYYY-MM-DD` and cannot be in the past. If `travelDate` is omitted, the backend defaults to the current date.

## Fare Calculation

The backend currently uses sample pricing:

```text
Fare = number of route segments × ₹150
```

For example, a journey spanning three route segments costs ₹450 in this demo. This is a simplified project rule, **not an official Indian Railways fare calculation**.

## Data Storage

The application uses JSON files:
- `app/src/main/java/com/irctc/localDB/trains.json` — train route and seat data
- `app/src/main/java/com/irctc/localDB/users.json` — user and booking data

This is suitable for a local demonstration, but it is not a substitute for a production database. Back up these files before manual edits or resetting demo data.

## Important Limitations

- Data is stored in local JSON files rather than a database server.
- Fare calculation uses a fixed demo rate per route segment; it does not represent official railway fares.
- Seat availability is stored in train data. The current model does not maintain separate seat inventories for each travel date.
- The project is a demonstration application and should receive further security, persistence, concurrency, and deployment review before production use.
- The repository's test directory should be populated with automated tests as a future improvement.

## Manual Verification Checklist

- [ ] Sign up with a new email
- [ ] Log in with valid credentials
- [ ] Confirm invalid credentials are rejected
- [ ] Search for a train by source and destination
- [ ] Check the displayed sample fare
- [ ] Book an available seat for a valid future date
- [ ] Confirm the ticket and fare appear in booking history
- [ ] Cancel a ticket and confirm it disappears from history
- [ ] Confirm unauthenticated booking/history requests are rejected
- [ ] Confirm one user cannot access another user's bookings
- [ ] Log out and confirm the client session is cleared

## Troubleshooting

### `npm` cannot find `package.json`

Run npm commands from the `frontend` directory, not the project root:

```powershell
cd frontend
npm run build
```

### Frontend cannot connect to the backend

1. Confirm Spring Boot is running on port `8080`.
2. Check the browser console and Network panel for the failed request.
3. Confirm the frontend origin is allowed by the backend CORS configuration.
4. Confirm authenticated requests include the `Authorization: Bearer ...` header.

### Gradle Wrapper cannot download Gradle

If the wrapper distribution download times out and Gradle is installed locally, run:

```powershell
gradle :app:build
gradle :app:bootRun
```

## Future Improvements

- Add automated unit and integration tests.
- Move user, train, seat, and booking data to a database.
- Make seat inventory specific to travel date.
- Improve booking consistency and concurrent seat-booking protection.
- Configure production secrets, HTTPS, logging, and deployment settings.
- Replace demo fare calculation with a clearly specified and verified fare model if required.
