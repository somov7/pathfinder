package ru.catghoti.pathfinder.service;

import org.springframework.stereotype.Service;
import ru.catghoti.pathfinder.model.Matrix;
import ru.catghoti.pathfinder.model.Node;
import ru.catghoti.pathfinder.model.PairWithPenalty;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@Service
public class TspFinder {

    private static final String DUMMY = "Dummy";

    public List<String> solve(Matrix matrix, boolean cycle) {
        Matrix distances = copy(matrix);
        Matrix withDummy = cycle ? copy(matrix) : addDummyVertex(matrix);
        for (int i = 0; i < withDummy.size(); i++) {
            setByLabels(withDummy, withDummy.getRowLabels()[i], withDummy.getColumnLabels()[i], Double.POSITIVE_INFINITY);
        }
        double minWeight = reduceMatrix(withDummy);
        Node root = new Node(null, null, null, false, minWeight, withDummy);
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparing(Node::getMinWeight));
        queue.add(root);
        while (!queue.isEmpty()) {
            Node cur = queue.poll();
            if (cur.getMatrix().size() == 0) {
                Optional<List<String>> path = restorePath(cur, cycle);
                if (path.isPresent()) {
                    return path.get();
                }
                continue;
            }
            appendChildren(cur);
            if (cur.getYes() != null) {
                queue.add(cur.getYes());
            }
            if (cur.getNo() != null) {
                queue.add(cur.getNo());
            }
        }
        throw new IllegalStateException();
    }

    private Optional<List<String>> restorePath(Node leaf, boolean cycle) {
        NavigableMap<String, String> next = new TreeMap<>();
        while (leaf.getParent() != null) {
            if (leaf.isTake()) {
                next.put(leaf.getFrom(), leaf.getTo());
            }
            leaf = leaf.getParent();
        }
        List<String> path = new ArrayList<>();
        String cur = next.firstKey();
        do {
            if (!Objects.equals(cur, DUMMY)) {
                path.add(cur);
            }
            cur = next.get(cur);
        } while (!cur.equals(next.firstKey()));
        int expectedPathSize = cycle ? next.size() : next.size() - 1;
        return path.size() == expectedPathSize ? Optional.of(path) : Optional.empty();
    }

    private void appendChildren(Node node) {
        if (node.getMatrix().size() == 1) {
            Node yes = new Node(
                    node,
                    node.getMatrix().getRowLabels()[0],
                    node.getMatrix().getColumnLabels()[0],
                    true,
                    node.getMinWeight(),
                    new Matrix(new double[0][0], new String[0], new String[0])
            );
            node.setYes(yes);
            return;
        }
        Matrix reduced = copy(node.getMatrix());
        if (!node.isTake()) {
            reduceMatrix(reduced);
        }
        PairWithPenalty pairWithPenalty = getMaxZeroPenalty(reduced);

        CompletableFuture<Node> yesFuture = CompletableFuture.supplyAsync(() -> {
                Matrix yesMatrix = exclude(reduced, pairWithPenalty.getRow(), pairWithPenalty.getColumn());
                double yesWeight = reduceMatrix(yesMatrix);
                Node yes = new Node(node, pairWithPenalty.getRow(), pairWithPenalty.getColumn(), true, yesWeight + node.getMinWeight(), yesMatrix);
                return yes;
        });

        Matrix noMatrix = copy(reduced);
        setByLabels(noMatrix, pairWithPenalty.getRow(), pairWithPenalty.getColumn(), Double.POSITIVE_INFINITY);
        Node no = new Node(node, pairWithPenalty.getRow(), pairWithPenalty.getColumn(), false, pairWithPenalty.getPenalty() + node.getMinWeight(), noMatrix);

        node.setYes(yesFuture.join());
        node.setNo(no);
    }

    private Matrix copy(Matrix matrix) {
        String[] rowLabels = Arrays.copyOf(matrix.getRowLabels(), matrix.size());
        String[] columnLabels = Arrays.copyOf(matrix.getColumnLabels(), matrix.size());
        double[][] dist = new double[matrix.size()][matrix.size()];
        for (int i = 0; i < dist.length; i++) {
            System.arraycopy(matrix.getDist()[i], 0, dist[i], 0, matrix.size());
        }
        return new Matrix(dist, rowLabels, columnLabels);
    }

    private Matrix addDummyVertex(Matrix matrix) {
        int size = matrix.size() + 1;
        String[] rowLabels = Arrays.copyOf(matrix.getRowLabels(), size);
        rowLabels[size - 1] = DUMMY;
        String[] columnLabels = Arrays.copyOf(matrix.getColumnLabels(), size);
        columnLabels[size - 1] = DUMMY;
        double[][] dist = new double[size][size];
        for (int i = 0; i < matrix.size(); i++) {
            System.arraycopy(matrix.getDist()[i], 0, dist[i], 0, matrix.size());
        }
        dist[size - 1][size - 1] = Double.POSITIVE_INFINITY;
        return new Matrix(dist, rowLabels, columnLabels);
    }

    private Matrix exclude(Matrix matrix, String row, String column) {
        double[][] grid = new double[matrix.size() - 1][matrix.size() - 1];
        int x = 0;
        int y = 0;
        for (int i = 0; i < matrix.size(); i++) {
            x = 0;
            if (matrix.getRowLabels()[i].equals(row)) {
                continue;
            }
            for (int j = 0; j < matrix.size(); j++) {
                if (matrix.getColumnLabels()[j].equals(column)) {
                    continue;
                }
                grid[y][x] = matrix.getDist()[i][j];
                x++;
            }
            y++;
        }
        String[] rowLabels = new String[matrix.size() - 1];
        int k = 0;
        for (int i = 0; i < matrix.size(); i++) {
            if (!matrix.getRowLabels()[i].equals(row)) {
                rowLabels[k] = matrix.getRowLabels()[i];
                k++;
            }
        }
        String[] columnLabels = new String[matrix.size() - 1];
        k = 0;
        for (int i = 0; i < matrix.size(); i++) {
            if (!matrix.getColumnLabels()[i].equals(column)) {
                columnLabels[k] = matrix.getColumnLabels()[i];
                k++;
            }
        }
        Matrix excluded = new Matrix(grid, rowLabels, columnLabels);
        setByLabels(excluded, column, row, Double.POSITIVE_INFINITY);
        return excluded;
    }

    private void setByLabels(Matrix matrix, String row, String column, double value) {
        int rowIndex = -1;
        int columnIndex = -1;
        for (int i = 0; i < matrix.size(); i++) {
            if (matrix.getRowLabels()[i].equals(row)) {
                rowIndex = i;
            }
            if (matrix.getColumnLabels()[i].equals(column)) {
                columnIndex = i;
            }
        }
        if (rowIndex != -1 && columnIndex != -1) {
            matrix.getDist()[rowIndex][columnIndex] = value;
        }
    }

    private double reduceMatrix(Matrix matrix) {
        // row reduction
        double[] rowMin = new double[matrix.size()];
        for (int i = 0; i < matrix.size(); i++) {
            rowMin[i] = Double.POSITIVE_INFINITY;
            for (int j = 0; j < matrix.size(); j++) {
                rowMin[i] = Math.min(rowMin[i], matrix.getDist()[i][j]);
            }
            for (int j = 0; j < matrix.size(); j++) {
                matrix.getDist()[i][j] -= rowMin[i];
            }
        }
        // column reduction
        double[] columnMin = new double[matrix.size()];
        for (int j = 0; j < matrix.size(); j++) {
            columnMin[j] = Double.POSITIVE_INFINITY;
            for (int i = 0; i < matrix.size(); i++) {
                columnMin[j] = Math.min(columnMin[j], matrix.getDist()[i][j]);
            }
            for (int i = 0; i < matrix.size(); i++) {
                matrix.getDist()[i][j] -= columnMin[j];
            }
        }
        return Stream.of(rowMin, columnMin)
                .flatMapToDouble(Arrays::stream)
                .sum();
    }

    public PairWithPenalty getMaxZeroPenalty(Matrix matrix) {
        List<PairWithPenalty> penalties = new ArrayList<>();
        for (int i = 0; i < matrix.size(); i++) {
            for (int j = 0; j < matrix.size(); j++) {
                if (Math.abs(matrix.getDist()[i][j]) > 1e-6) {
                    continue;
                }
                double rowMin = Double.POSITIVE_INFINITY;
                for (int y = 0; y < matrix.size(); y++) {
                    if (y != i) {
                        rowMin = Math.min(rowMin, matrix.getDist()[y][j]);
                    }
                }
                double columnMin = Double.POSITIVE_INFINITY;
                for (int x = 0; x < matrix.size(); x++) {
                    if (x != j) {
                        columnMin = Math.min(columnMin, matrix.getDist()[i][x]);
                    }
                }
                penalties.add(new PairWithPenalty(matrix.getRowLabels()[i], matrix.getColumnLabels()[j], rowMin + columnMin));
            }
        }
        return penalties.stream().max(Comparator.comparing(PairWithPenalty::getPenalty)).orElseThrow();
    }
}
