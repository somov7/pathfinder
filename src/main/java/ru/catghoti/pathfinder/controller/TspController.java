package ru.catghoti.pathfinder.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.catghoti.pathfinder.dto.FindPathRequest;
import ru.catghoti.pathfinder.model.Matrix;
import ru.catghoti.pathfinder.service.TspFinder;

import java.util.List;

@RestController
@RequestMapping(value = "/find-path", consumes = "application/json", produces = "application/json")
@RequiredArgsConstructor
@Slf4j
public class TspController {

    private final TspFinder tspFinder;

    @PostMapping
    public List<String> findPath(@RequestBody FindPathRequest request) {
        log.info("Request: {}", request);
        Matrix matrix = new Matrix(request.getWeights(), request.getLabels().toArray(new String[0]), request.getLabels().toArray(new String[0]));
        return tspFinder.solve(matrix, request.isCycle());
    }
}
