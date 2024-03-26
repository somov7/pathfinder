package ru.catghoti.pathfinder.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.catghoti.pathfinder.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@Slf4j
public class TspFinder {

    private static final UUID DUMMY = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public Path solve(Matrix matrix, boolean cycle) {
        for (int i = 0; i < matrix.size(); i++) {
                matrix.setDist(i, i, Long.MAX_VALUE);
        }
        Matrix initialMatrix = cycle ? copy(matrix) : addDummyVertex(matrix);
        Matrix withDummy = cycle ? copy(matrix) : addDummyVertex(matrix);
        long minWeight = reduceMatrix(withDummy, true);
        Node root = new Node(null, null, null, true, minWeight, withDummy, withDummy.size());
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparing(Node::getMinWeight)
                .thenComparing(Node::getOrder)
                .thenComparing(Node::isTake, Comparator.reverseOrder())
        );
        int order = 0;
        root.setOrder(order);
        queue.add(root);
        while (!queue.isEmpty()) {
            Node cur = queue.poll();
            if (cur.getLeft() == 0) {
                log.info("{}", cur);
                var path = restorePath(cur, cycle);
                validatePath(path, initialMatrix, cur.getMinWeight(), cycle);
                return new Path(path, cur.getMinWeight(), 0);
            }
            appendChildren(cur);
            order++;
            cur.getYes().setOrder(order);
            order++;
            cur.getNo().setOrder(order);
            queue.add(cur.getYes());
            queue.add(cur.getNo());
        }
        throw new IllegalArgumentException();
    }

    private void validatePath(List<UUID> path, Matrix initialMatrix, long minWeight, boolean cycle) {
        long actual = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            actual += initialMatrix.getDist(initialMatrix.getRowIndex(path.get(i)), initialMatrix.getColumnIndex(path.get(i + 1)));
        }
        if (cycle) {
            actual += initialMatrix.getDist(initialMatrix.getRowIndex(path.get(path.size() - 1)), initialMatrix.getColumnIndex(path.get(0)));
        }
        if (actual != minWeight) {
            throw new IllegalStateException("Weight of path expected to be %d but was %d".formatted(minWeight, actual));
        }
    }

    private List<UUID> restorePath(Node leaf, boolean cycle) {
        NavigableMap<UUID, UUID> next = new TreeMap<>();
        while (leaf.getParent() != null) {
            for (Edge edge : leaf.getInclude()) {
                next.put(edge.from(), edge.to());
            }
            leaf = leaf.getParent();
        }
        List<UUID> path = new ArrayList<>();
        UUID cur = cycle ? next.firstKey() : next.get(DUMMY);
        do {
            if (!Objects.equals(cur, DUMMY)) {
                path.add(cur);
            }
            cur = next.get(cur);
        } while (!cur.equals(cycle ? next.firstKey() : next.get(DUMMY)));
        return path;
    }

    private void appendChildren(Node node) {
        log.info("{}", node);
        if (!node.isTake()) {
            reduceMatrix(node.getMatrix(), false);
        }
        PairWithPenalty pairWithPenalty = getMaxZeroPenalty(node.getMatrix());
        appendYesChild(node, pairWithPenalty);
        appendNoChild(node, pairWithPenalty);
        node.setMatrix(null);
    }

    private void restoreMatrix(Node node, Matrix initialMatrix) {
        Map<UUID, UUID> from = new HashMap<>();
        Map<UUID, UUID> to = new HashMap<>();
        List<Edge> excluded = new ArrayList<>();
        Node copy = node;
        while (copy.getParent() != null) {
            for (Edge edge : copy.getInclude()) {
                from.put(edge.from(), edge.to());
                to.put(edge.to(), edge.from());
            }
            excluded.addAll(Arrays.asList(copy.getExclude()));
            copy = copy.getParent();
        }
        Map<UUID, Integer> initialRowLabelToNumber = IntStream.range(0, initialMatrix.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> initialMatrix.getRowLabels()[i],
                        i -> i
                ));
        Map<UUID, Integer> initialColumnLabelToNumber = IntStream.range(0, initialMatrix.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> initialMatrix.getColumnLabels()[i],
                        i -> i
                ));
        long minWeight = from.entrySet().stream()
                .mapToLong((e) -> initialMatrix.getDist(initialRowLabelToNumber.get(e.getKey()), initialColumnLabelToNumber.get(e.getValue())))
                .sum();
        Matrix matrix = exclude(initialMatrix, from.keySet(), to.keySet());

        Map<UUID, Integer> rowLabelToNumber = IntStream.range(0, matrix.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> matrix.getRowLabels()[i],
                        i -> i
                ));
        Map<UUID, Integer> columnLabelToNumber = IntStream.range(0, matrix.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> matrix.getColumnLabels()[i],
                        i -> i
                ));

        for (Edge edge : excluded) {
            int rowNum = rowLabelToNumber.getOrDefault(edge.from(), -1);
            int columnNum = columnLabelToNumber.getOrDefault(edge.to(), -1);
            if (rowNum != -1 && columnNum != -1) {
                matrix.setDist(rowNum, columnNum, Long.MAX_VALUE);
            }
        }
        long reduction = reduceMatrix(matrix, true);
        minWeight = add(minWeight, reduction);

        node.setMatrix(matrix);
        node.setMinWeight(minWeight);
    }

    private void appendYesChild(Node node, PairWithPenalty pairWithPenalty) {
        if (node.getMatrix().size() == 2) {
            UUID finalFrom = node.getMatrix().getRowLabels()[0] == pairWithPenalty.getRow()
                    ? node.getMatrix().getRowLabels()[1] : node.getMatrix().getRowLabels()[0];
            UUID finalTo = node.getMatrix().getColumnLabels()[0] == pairWithPenalty.getColumn()
                    ? node.getMatrix().getColumnLabels()[1] : node.getMatrix().getColumnLabels()[0];
            long minWeight = add(node.getMinWeight(), reduceMatrix(node.getMatrix(), true));
            Node yes = new Node(
                    node,
                    new Edge[]{
                            new Edge(pairWithPenalty.getRow(), pairWithPenalty.getColumn()),
                            new Edge(finalFrom, finalTo),
                    },
                    new Edge[0],
                    true,
                    minWeight,
                    new Matrix(new long[0], new UUID[0], new UUID[0]),
                    0
            );
            node.setYes(yes);
            return;
        }
        Matrix matrix = exclude(node.getMatrix(), Set.of(pairWithPenalty.getRow()), Set.of(pairWithPenalty.getColumn()));

        Node copy = node;

        Map<UUID, UUID> fromMap = new HashMap<>();
        Map<UUID, UUID> toMap = new HashMap<>();
        while (copy.getParent() != null) {
            for (Edge edge : copy.getInclude()) {
                fromMap.put(edge.from(), edge.to());
                toMap.put(edge.to(), edge.from());
            }
            copy = copy.getParent();
        }

        UUID from = pairWithPenalty.getColumn();
        while (fromMap.get(from) != null) {
            from = fromMap.get(from);
        }

        UUID to = pairWithPenalty.getRow();
        while (toMap.get(to) != null) {
            to = toMap.get(to);
        }

        setByLabels(matrix, from, to, Long.MAX_VALUE);

        long minWeight = add(reduceMatrix(matrix, true), node.getMinWeight());

        Node child = new Node(
                node,
                new Edge[]{new Edge(pairWithPenalty.getRow(), pairWithPenalty.getColumn())},
                new Edge[]{new Edge(from, to)},
                true,
                minWeight,
                matrix,
                matrix.size()
        );
        node.setYes(child);
    }

    private void appendNoChild(Node node, PairWithPenalty pairWithPenalty) {
        long minWeight = add(node.getMinWeight(), pairWithPenalty.getPenalty());
        Matrix matrix = node.getMatrix();
        setByLabels(matrix, pairWithPenalty.getRow(), pairWithPenalty.getColumn(), Long.MAX_VALUE);
        Node child = new Node(
                node,
                new Edge[0],
                new Edge[]{new Edge(pairWithPenalty.getRow(), pairWithPenalty.getColumn())},
                false,
                minWeight,
                matrix,
                node.getLeft()
        );
        node.setNo(child);
    }

    private Matrix copy(Matrix matrix) {
        UUID[] rowLabels = Arrays.copyOf(matrix.getRowLabels(), matrix.size());
        UUID[] columnLabels = Arrays.copyOf(matrix.getColumnLabels(), matrix.size());
        long[] dist = new long[matrix.size() * matrix.size()];
        System.arraycopy(matrix.getDist(), 0, dist, 0, matrix.size() * matrix.size());
        return new Matrix(dist, rowLabels, columnLabels);
    }

    private Matrix addDummyVertex(Matrix matrix) {
        int size = matrix.size() + 1;
        UUID[] rowLabels = Arrays.copyOf(matrix.getRowLabels(), size);
        rowLabels[size - 1] = DUMMY;
        UUID[] columnLabels = Arrays.copyOf(matrix.getColumnLabels(), size);
        columnLabels[size - 1] = DUMMY;
        long[] dist = new long[size * size];
        for (int i = 0; i < matrix.size(); i++) {
            System.arraycopy(matrix.getDist(), matrix.size() * i, dist, size * i, matrix.size());
        }
        dist[size * size - 1] = Long.MAX_VALUE;
        return new Matrix(dist, rowLabels, columnLabels);
    }

    private Matrix exclude(Matrix matrix, Set<UUID> rows, Set<UUID> columns) {
        long[] dist = new long[(matrix.size() - rows.size()) * (matrix.size() - columns.size())];
        int k = 0;
        for (int i = 0; i < matrix.size(); i++) {
            if (rows.contains(matrix.getRowLabels()[i])) {
                continue;
            }
            for (int j = 0; j < matrix.size(); j++) {
                if (columns.contains(matrix.getColumnLabels()[j])) {
                    continue;
                }
                dist[k] = matrix.getDist(i, j);
                k++;
            }
        }
        UUID[] rowLabels = new UUID[matrix.size() - rows.size()];
        k = 0;
        for (int i = 0; i < matrix.size(); i++) {
            if (!rows.contains(matrix.getRowLabels()[i])) {
                rowLabels[k] = matrix.getRowLabels()[i];
                k++;
            }
        }
        UUID[] columnLabels = new UUID[matrix.size() - columns.size()];
        k = 0;
        for (int i = 0; i < matrix.size(); i++) {
            if (!columns.contains(matrix.getColumnLabels()[i])) {
                columnLabels[k] = matrix.getColumnLabels()[i];
                k++;
            }
        }
        return new Matrix(dist, rowLabels, columnLabels);
    }

    private void setByLabels(Matrix matrix, UUID row, UUID column, long value) {
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
            matrix.setDist(rowIndex, columnIndex, value);
        }
    }

    private long reduceMatrix(Matrix matrix, boolean reduceExtra) {
        boolean isInfinite = false;
        // row reduction
        long[] rowMin = new long[matrix.size()];
        for (int i = 0; i < matrix.size(); i++) {
            rowMin[i] = Long.MAX_VALUE;
            for (int j = 0; j < matrix.size(); j++) {
                rowMin[i] = Math.min(rowMin[i], matrix.getDist(i, j));
            }
            for (int j = 0; j < matrix.size(); j++) {
                matrix.addToDist(i, j, -rowMin[i]);
            }
            if (rowMin[i] == Long.MAX_VALUE) {
                isInfinite = true;
            }
        }
        // column reduction
        long[] columnMin = new long[matrix.size()];
        for (int j = 0; j < matrix.size(); j++) {
            columnMin[j] = Long.MAX_VALUE;
            for (int i = 0; i < matrix.size(); i++) {
                columnMin[j] = Math.min(columnMin[j], matrix.getDist(i, j));
            }
            for (int i = 0; i < matrix.size(); i++) {
                matrix.addToDist(i, j, -columnMin[j]);
            }
            if (columnMin[j] == Long.MAX_VALUE) {
                isInfinite = true;
            }
        }
        if (isInfinite) {
            return Long.MAX_VALUE;
        }
        if (!reduceExtra) {
            return Stream.of(rowMin, columnMin)
                    .flatMapToLong(Arrays::stream)
                    .sum();
        }

        boolean reducable = true;
        long extraReduce = 0;


        while (reducable) {
            reducable = false;
            Map<Integer, List<Integer>> rowsWithSingleZeroByColumn = new HashMap<>();
            Map<Integer, Long> minPerRow = new HashMap<>();

            outerCycle:
            for (int i = 0; i < matrix.size(); i++) {
                long min = Long.MAX_VALUE;
                int zeroColumn = -1;
                boolean zero = false;
                for (int j = 0; j < matrix.size(); j++) {
                    long dist = matrix.getDist(i, j);
                    if (dist != 0) {
                        if (dist <= min) {
                            min = dist;
                        }
                    } else {
                        if (zero) {
                            continue outerCycle;
                        }
                        zero = true;
                        zeroColumn = j;
                    }
                }
                if (zero) {
                    minPerRow.put(i, min);
                    List<Integer> column = rowsWithSingleZeroByColumn.get(zeroColumn);
                    if (column == null) {
                        column = new ArrayList<>();
                        column.add(i);
                        rowsWithSingleZeroByColumn.put(zeroColumn, column);
                    } else {
                        column.add(i);
                    }
                }
            }

            var columnEntryO = rowsWithSingleZeroByColumn.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().size()));

            if (columnEntryO.isPresent()) {
                var columnEntry = columnEntryO.get();

                if (columnEntry.getValue().size() > 1) {
                    reducable = true;
                    long rowsMin = columnEntry.getValue().stream().mapToLong(minPerRow::get).min().orElseThrow();
                    extraReduce += rowsMin * (columnEntry.getValue().size() - 1);
                    columnEntry.getValue().forEach(row -> IntStream.range(0, matrix.size()).forEach(column -> matrix.addToDist(row, column, -rowsMin)));
                    IntStream.range(0, matrix.size()).forEach(row -> matrix.addToDist(row, columnEntry.getKey(), rowsMin));
                }
            }
        }

        reducable = true;

        while (reducable) {
            reducable = false;
            Map<Integer, List<Integer>> columnsWithSingleZeroByRow = new HashMap<>();
            Map<Integer, Long> minPerColumn = new HashMap<>();

            outerCycle:
            for (int j = 0; j < matrix.size(); j++) {
                long min = Long.MAX_VALUE;
                int zeroRow = -1;
                boolean zero = false;
                for (int i = 0; i < matrix.size(); i++) {
                    long dist = matrix.getDist(i, j);
                    if (dist != 0) {
                        if (dist <= min) {
                            min = dist;
                        }
                    } else {
                        if (zero) {
                            continue outerCycle;
                        }
                        zero = true;
                        zeroRow = i;
                    }
                }
                if (zero) {
                    minPerColumn.put(j, min);
                    List<Integer> row = columnsWithSingleZeroByRow.get(zeroRow);
                    if (row == null) {
                        row = new ArrayList<>();
                        row.add(j);
                        columnsWithSingleZeroByRow.put(zeroRow, row);
                    } else {
                        row.add(j);
                    }
                }
            }


            var rowEntryO = columnsWithSingleZeroByRow.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().size()));

            if (rowEntryO.isPresent()) {
                var rowEntry = rowEntryO.get();

                if (rowEntry.getValue().size() > 1) {
                    reducable = true;
                    long columnsMin = rowEntry.getValue().stream().mapToLong(minPerColumn::get).min().orElseThrow();
                    extraReduce += columnsMin * (rowEntry.getValue().size() - 1);
                    rowEntry.getValue().forEach(column -> IntStream.range(0, matrix.size()).forEach(row -> matrix.addToDist(row, column, -columnsMin)));
                    IntStream.range(0, matrix.size()).forEach(column -> matrix.addToDist(rowEntry.getKey(), column, columnsMin));
                }
            }
        }

        if (extraReduce > 0) {
            log.info("Reduced " + extraReduce);
        }

        return Stream.of(rowMin, columnMin)
                .flatMapToLong(Arrays::stream)
                .sum() + extraReduce;
    }

    public PairWithPenalty getMaxZeroPenalty(Matrix matrix) {
        List<PairWithPenalty> penalties = new ArrayList<>();
        int[][] rowMins = new int[matrix.size()][2];
        int[][] columnMins = new int[matrix.size()][2];

        for (int i = 0; i < matrix.size(); i++) {
            rowMins[i][0] = -1;
            rowMins[i][1] = -1;
            for (int j = 0; j < matrix.size(); j++) {
                if (rowMins[i][0] == -1 || matrix.getDist(i, j) < matrix.getDist(i, rowMins[i][0])) {
                    rowMins[i][1] = rowMins[i][0];
                    rowMins[i][0] = j;
                } else if (rowMins[i][1] == -1 || matrix.getDist(i, j) < matrix.getDist(i, rowMins[i][1])) {
                    rowMins[i][1] = j;
                }
            }
        }

        for (int j = 0; j < matrix.size(); j++) {
            columnMins[j][0] = -1;
            columnMins[j][1] = -1;
            for (int i = 0; i < matrix.size(); i++) {
                if (columnMins[j][0] == -1 || matrix.getDist(i, j) < matrix.getDist(columnMins[j][0], j)) {
                    columnMins[j][1] = columnMins[j][0];
                    columnMins[j][0] = i;
                } else if (columnMins[j][1] == -1 || matrix.getDist(i, j) < matrix.getDist(columnMins[j][1], j)) {
                    columnMins[j][1] = i;
                }
            }
        }

        for (int i = 0; i < matrix.size(); i++) {
            for (int j = 0; j < matrix.size(); j++) {
                if (matrix.getDist(i, j) != 0L) {
                    continue;
                }
                long rowMin = rowMins[i][0] == j ? matrix.getDist(i, rowMins[i][1]) : matrix.getDist(i, rowMins[i][0]);
                long columnMin = columnMins[j][0] == i ? matrix.getDist(columnMins[j][1], j) : matrix.getDist(columnMins[j][0], j);
                long penalty;
                if (rowMin == Long.MAX_VALUE || columnMin == Long.MAX_VALUE) {
                    penalty = Long.MAX_VALUE;
                } else {
                    penalty = rowMin + columnMin;
                }
                penalties.add(new PairWithPenalty(matrix.getRowLabels()[i], matrix.getColumnLabels()[j], penalty));
            }
        }
        return penalties.stream().max(Comparator.comparing(PairWithPenalty::getPenalty)).orElseThrow();
    }

    private long add(long a, long b) {
        return a == Long.MAX_VALUE || b == Long.MAX_VALUE ? Long.MAX_VALUE : a + b;
    }
}
