package ru.catghoti.pathfinder.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Path {
    private List<UUID> path;
    private long totalDistance;
    private long totalTime;
}
