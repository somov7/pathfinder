package ru.catghoti.pathfinder.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Matrix {
    private double dist[][];
    private String[] rowLabels;
    private String[] columnLabels;

    public int size() {
        return dist.length;
    }
}
