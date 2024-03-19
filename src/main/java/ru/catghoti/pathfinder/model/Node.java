package ru.catghoti.pathfinder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
public class Node {
    @ToString.Exclude
    private Node parent;
    private String from;
    private String to;
    private boolean take;
    private double minWeight;
    private Matrix matrix;
    @ToString.Exclude
    private Node yes;
    @ToString.Exclude
    private Node no;

    public Node(Node parent, String from, String to, boolean take, double minWeight, Matrix matrix) {
        this.parent = parent;
        this.from = from;
        this.to = to;
        this.take = take;
        this.minWeight = minWeight;
        this.matrix = matrix;
    }
}
