package ru.catghoti.pathfinder.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.catghoti.pathfinder.dto.FindPathLocationsRequest;
import ru.catghoti.pathfinder.dto.TestDto;
import ru.catghoti.pathfinder.model.Matrix;
import ru.catghoti.pathfinder.model.Path;
import ru.catghoti.pathfinder.service.DistanceMatrixProvider;
import ru.catghoti.pathfinder.service.TspFinder;

import java.util.UUID;
import java.util.stream.LongStream;


@RestController
@RequestMapping(value = "/find-path", consumes = "application/json", produces = "application/json")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class TspController {

    private final TspFinder tspFinder;
    private final DistanceMatrixProvider distanceMatrixProvider;

    @PostMapping("/locations")
    public Path findPath(@RequestBody FindPathLocationsRequest request) {
        log.info("{}", request);
        Matrix matrix = distanceMatrixProvider.getMatrix(request.getLocations());
        return tspFinder.solve(matrix, request.isCycle());
    }

    @PostMapping("/test")
    public Path findPath(@RequestBody TestDto test) {
        Matrix matrix = new Matrix(test.getDist(), LongStream.rangeClosed(1, test.getDist().length).mapToObj(i -> new UUID(0, i)).toArray(UUID[]::new));
        return tspFinder.solve(matrix, test.isCycle());
    }
}
