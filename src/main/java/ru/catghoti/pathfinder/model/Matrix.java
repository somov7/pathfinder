package ru.catghoti.pathfinder.model;

import lombok.Data;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.IntStream;

@Data
public class Matrix {
    private int size;
    private long[] dist;
    private UUID[] rowLabels;
    private UUID[] columnLabels;

    public Matrix(long[][] dist, UUID[] labels) {
        if (labels.length != dist.length) {
            throw new IllegalArgumentException();
        }
        this.rowLabels = labels;
        this.columnLabels = labels;
        this.dist = Arrays.stream(dist).flatMapToLong(Arrays::stream).toArray();
        this.size = labels.length;
    }

    public Matrix(long[] dist, UUID[] rowLabels, UUID[] columnLabels) {
        if (dist.length != rowLabels.length * rowLabels.length || rowLabels.length != columnLabels.length) {
            throw new IllegalArgumentException();
        }
        this.rowLabels = rowLabels;
        this.columnLabels = columnLabels;
        this.dist = dist;
        this.size = rowLabels.length;
    }

    public long getDist(int row, int column) {
        return dist[row * size + column];
    }

    public void setDist(int row, int column, long value) {
        dist[row * size + column] = value;
    }

    public void addToDist(int row, int column, long value) {
        if (dist[row * size + column] == Long.MAX_VALUE) {
            return;
        }
        dist[row * size + column] += value;
    }

    public int getRowIndex(UUID label) {
        return IntStream.range(0, rowLabels.length).filter(i -> rowLabels[i].equals(label)).findFirst().orElse(-1);
    }

    public int getColumnIndex(UUID label) {
        return IntStream.range(0, columnLabels.length).filter(i -> columnLabels[i].equals(label)).findFirst().orElse(-1);
    }

    public int size() {
        return size;
    }
}
