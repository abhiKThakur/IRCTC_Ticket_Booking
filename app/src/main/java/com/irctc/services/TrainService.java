package com.irctc.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.irctc.entities.Train;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrainService {

    private final List<Train> trainList;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TRAIN_DB_PATH =
            "src/main/java/com/irctc/localDB/trains.json";

    public TrainService() throws IOException {

        File trainsFile = new File(TRAIN_DB_PATH);

        // Print the exact file being used.
        System.out.println(
                "Train database path: "
                        + trainsFile.getAbsolutePath()
        );

        if (!trainsFile.exists()) {
            throw new IOException(
                    "Train database file not found: "
                            + trainsFile.getAbsolutePath()
            );
        }

        // Load train data.
        trainList = objectMapper.readValue(
                trainsFile,
                new TypeReference<List<Train>>() {}
        );

        System.out.println(
                "Train database loaded successfully."
        );
    }

    public List<Train> getAllTrains() {
        return trainList;
    }

    public List<Train> searchTrains(
            String source,
            String destination
    ) {

        if (source == null || destination == null) {
            return List.of();
        }

        return trainList.stream()
                .filter(train ->
                        validTrain(train, source, destination))
                .collect(Collectors.toList());
    }

    public void addTrain(Train newTrain) {

        if (newTrain == null || newTrain.getTrainId() == null) {
            throw new IllegalArgumentException(
                    "Train and trainId must not be null."
            );
        }

        Optional<Train> existingTrain = trainList.stream()
                .filter(train ->
                        train.getTrainId().equalsIgnoreCase(
                                newTrain.getTrainId()
                        ))
                .findFirst();

        if (existingTrain.isPresent()) {
            updateTrain(newTrain);
        } else {
            trainList.add(newTrain);
            saveTrainListToFile();
        }
    }

    public void updateTrain(Train updatedTrain) {

        if (updatedTrain == null
                || updatedTrain.getTrainId() == null) {

            throw new IllegalArgumentException(
                    "Train and trainId must not be null."
            );
        }

        OptionalInt index = IntStream.range(
                        0, trainList.size()
                )
                .filter(i ->
                        trainList.get(i).getTrainId()
                                .equalsIgnoreCase(
                                        updatedTrain.getTrainId()
                                ))
                .findFirst();

        if (index.isPresent()) {

            trainList.set(
                    index.getAsInt(),
                    updatedTrain
            );

            saveTrainListToFile();

        } else {
            addTrain(updatedTrain);
        }
    }

    private void saveTrainListToFile() {

        File trainsFile = new File(TRAIN_DB_PATH);

        try {

            objectMapper.enable(
                    SerializationFeature.INDENT_OUTPUT
            );

            objectMapper.writeValue(
                    trainsFile,
                    trainList
            );

            System.out.println(
                    "Train database saved successfully: "
                            + trainsFile.getAbsolutePath()
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to save train database: "
                            + trainsFile.getAbsolutePath(),
                    e
            );
        }
    }

    private boolean validTrain(
            Train train,
            String source,
            String destination
    ) {

        if (train.getStations() == null) {
            return false;
        }

        int sourceIndex = train.getStations().indexOf(
                source.trim().toLowerCase()
        );

        int destinationIndex = train.getStations().indexOf(
                destination.trim().toLowerCase()
        );

        return sourceIndex != -1
                && destinationIndex != -1
                && sourceIndex < destinationIndex;
    }
}