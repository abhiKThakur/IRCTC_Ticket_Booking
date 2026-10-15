# RailwayGo frontend

React + Vite frontend foundation for the existing Java ticket-booking project.

## Run locally

Requirements: Node.js 20.19+ or 22.12+.

```bash
cd frontend
npm install
npm run dev
```

Open the local URL printed by Vite.

## Current frontend scope

- Responsive landing page and navigation
- Origin, destination, and journey-date search form
- Station swap control
- Sample train results, sorting, and booking preview modal
- Login and signup interface previews
- Responsive layouts for mobile, tablet, and desktop

## Important integration note

This is the frontend-first stage. Train search currently uses sample data in `src/App.jsx`. Login, signup, and booking actions are preview-only and do not write to the existing Java application. The current Java project is a command-line application; REST endpoints and API integration are a later step. Do not treat sample availability or fares as live railway information.

## Next steps

1. Review and refine the frontend screens.
2. Expose existing Java services through a web API without discarding working business logic.
3. Replace sample data with API responses and wire authentication/booking flows.
4. Test the end-to-end application before deployment.
