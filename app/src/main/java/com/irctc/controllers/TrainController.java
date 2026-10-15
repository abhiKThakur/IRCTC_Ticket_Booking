package com.irctc.controllers;

import com.irctc.entities.Train;
import com.irctc.services.TrainService;
import com.irctc.services.UserBookingService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/trains")
public class TrainController {

    private final TrainService trainService;

    public TrainController() {
        try {
            this.trainService = new TrainService();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to load train data from trains.json", e
            );
        }
    }

    @GetMapping("/health")
    public Map<String, String> checkHealth() {
        return Map.of(
                "status", "success",
                "message", "Train API is running"
        );
    }

    @GetMapping
    public List<Train> searchTrains(
            @RequestParam String source,
            @RequestParam String destination
    ) {
        return trainService.searchTrains(
                source.trim().toLowerCase(),
                destination.trim().toLowerCase()
        );
    }

    @GetMapping("/all")
    public List<Train> getAllTrains() {
        try {
            TrainService latestTrainService = new TrainService();
            return latestTrainService.getAllTrains();
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load the latest train data",
                    e
            );
        }
    }

    @GetMapping("/{trainId}/seats")
    public List<List<Integer>> getSeatAvailability(
            @PathVariable String trainId
    ) {
        try {
            TrainService latestTrainService = new TrainService();

            Train train = latestTrainService.getAllTrains()
                    .stream()
                    .filter(t -> t.getTrainId().equalsIgnoreCase(trainId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Train not found: " + trainId
                    ));

            return train.getSeats();

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve the latest seat availability",
                    e
            );
        }
    }
}