package ru.catghoti.pathfinder.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class FindPathLocationsRequest {
    private List<Location> locations;
    private boolean cycle;

    @Data
    public static class Location {
        private double longitude;
        private double latitude;
        private String title;
        private UUID id;
    }
}
