package ru.catghoti.pathfinder.service;

import com.google.common.collect.Lists;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.catghoti.pathfinder.dto.FindPathLocationsRequest;
import ru.catghoti.pathfinder.model.Matrix;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistanceMatrixProvider {

    private static final int BATCH_SIZE = 10;

    private final GeoApiContext context;

    @SneakyThrows
    @Cacheable("distanceMatrix")
    public Matrix getMatrix(List<FindPathLocationsRequest.Location> locations) {
        long[][] distances = new long[locations.size()][locations.size()];
        long[][] times = new long[locations.size()][locations.size()];

        List<List<LatLng>> batches = Lists.partition(locations, BATCH_SIZE)
                .stream()
                .map(batch -> batch.stream().map(loc -> new LatLng(loc.getLatitude(), loc.getLongitude())).toList())
                .toList();

        CountDownLatch cdl = new CountDownLatch(batches.size() * batches.size());

        for (int i = 0; i < batches.size(); i++) {
            LatLng[] origins = batches.get(i).toArray(new LatLng[0]);
            for (int j = 0; j < batches.size(); j++) {
                LatLng[] destinations = batches.get(j).toArray(new LatLng[0]);
                int finalI = i;
                int finalJ = j;
                DistanceMatrixApi.newRequest(context)
                        .mode(TravelMode.WALKING)
                        .units(Unit.METRIC)
                        .origins(origins)
                        .destinations(destinations)
                        .setCallback(new PendingResult.Callback<>() {
                            @Override
                            public void onResult(DistanceMatrix distanceMatrix) {
                                log.info("response: {}", distanceMatrix);

                                for (int k = 0; k < distanceMatrix.originAddresses.length; k++) {
                                    for (int p = 0; p < distanceMatrix.destinationAddresses.length; p++) {
                                        distances[k + finalI * BATCH_SIZE][p + finalJ * BATCH_SIZE] =
                                                distanceMatrix.rows[k].elements[p].distance.inMeters;
                                        times[k + finalI * BATCH_SIZE][p + finalJ * BATCH_SIZE] =
                                                distanceMatrix.rows[k].elements[p].duration.inSeconds;
                                    }
                                }
                                cdl.countDown();
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                throw new RuntimeException(throwable);
                            }
                        });
            }
        }

        cdl.await();

        for (int i = 0; i < locations.size(); i++) {
            distances[i][i] = Long.MAX_VALUE;
            times[i][i] = Long.MAX_VALUE;
        }

        log.info("{}, {}", distances, locations);
        UUID[] ids = locations.stream().map(FindPathLocationsRequest.Location::getId).toArray(UUID[]::new);

        return new Matrix(distances, ids);
    }
}
