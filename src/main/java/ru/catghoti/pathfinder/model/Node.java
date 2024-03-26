package ru.catghoti.pathfinder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Node {
    @ToString.Exclude
    private Node parent;
    private Edge[] include;
    private Edge[] exclude;
    private boolean take;
    private long minWeight;
    private Matrix matrix;
    private int left;
    private Node yes;
    private Node no;
    private int order;

    public Node(Node parent, Edge[] include, Edge[] exclude, boolean take, long minWeight, Matrix matrix, int left) {
        this.parent = parent;
        this.include = include;
        this.exclude = exclude;
        this.take = take;
        this.minWeight = minWeight;
        this.matrix = matrix;
        this.left = left;
    }
}
