package ru.catghoti.pathfinder.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class PairWithPenalty {
    private UUID row;
    private UUID column;
    private long penalty;
}
