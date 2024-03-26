package ru.catghoti.pathfinder.configuration;

import com.google.maps.GeoApiContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoApiContextConfiguration {

    @Bean
    public GeoApiContext geoApiContext(@Value("${google.maps.api.key}") String key) {
        return new GeoApiContext.Builder()
                .apiKey(key)
                .queryRateLimit(10)
                .disableRetries()
                .build();
    }
}
