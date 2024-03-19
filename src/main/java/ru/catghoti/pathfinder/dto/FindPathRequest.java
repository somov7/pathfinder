package ru.catghoti.pathfinder.dto;

import lombok.Data;

import java.util.List;

@Data
public class FindPathRequest {
    private List<String> labels;
    private double[][] weights;
    private boolean cycle;
}
