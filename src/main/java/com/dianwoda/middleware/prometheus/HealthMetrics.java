package com.dianwoda.middleware.prometheus;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

public class HealthMetrics implements MeterBinder {


    private CompositeHealthIndicator healthIndicator;

    private List<HealthIndicator> healthIndicators;

    public HealthMetrics(CompositeHealthIndicator healthIndicator, List<HealthIndicator> healthIndicators) {
        this.healthIndicator = healthIndicator;
        this.healthIndicators = healthIndicators;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        register(registry, healthIndicator, "global");
        healthIndicators.forEach(x -> {
            if (x instanceof CompositeHealthIndicator) {
                CompositeHealthIndicator cli = (CompositeHealthIndicator) x;

                try {
                    Field field = cli.getClass().getDeclaredField("indicators");
                    field.setAccessible(true);
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

                    Map<String, HealthIndicator> climaps = (Map<String, HealthIndicator>) field.get(cli);
                    climaps.forEach((k, v) -> register(registry, v, k));
                } catch (Exception e) {
                }

            } else {
                register(registry, healthIndicator, extratKey(x.getClass().getSimpleName()));
            }
        });

    }

    private String extratKey(String name) {
        int index = name.toLowerCase().indexOf("healthindicator");
        if (index > 0) {
            return name.substring(0, index);
        }
        return name;
    }

    private void register(MeterRegistry registry, HealthIndicator healthIndicator, String key) {
        Tags upTags = Tags.of("status", "up", "type", key);
        String code = healthIndicator.health().getStatus().getCode();

        Gauge.builder("serv.health", healthIndicator, h -> "UP".equals(code) ? 1 : 0)
                .tags(upTags)
                .description("the" + key + "health info")
                .register(registry);


        Tags downTags = Tags.of("status", "down", "type", key);
        Gauge.builder("serv.health", healthIndicator, h -> "DOWN".equals(code) ? 1 : 0)
                .tags(downTags)
                .description("the" + key + "health info")
                .register(registry);
    }

}
