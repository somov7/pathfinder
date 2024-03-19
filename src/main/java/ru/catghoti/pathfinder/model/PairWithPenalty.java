package ru.catghoti.pathfinder.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PairWithPenalty {
    private String row;
    private String column;
    private double penalty;
}
