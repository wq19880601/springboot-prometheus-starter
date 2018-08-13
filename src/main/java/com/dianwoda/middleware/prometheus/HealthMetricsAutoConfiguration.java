package com.dianwoda.middleware.prometheus;

import com.dianwoda.middleware.prometheus.consul.ConsulCenterRegisterRunner;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.spring.autoconfigure.MeterRegistryCustomizer;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Optional;

@Configuration
public class HealthMetricsAutoConfiguration {


    @Bean
    @ConditionalOnClass(CompositeHealthIndicator.class)
    public HealthMetrics createDataSourceMetrics(HealthAggregator healthAggregator,
                                                 List<HealthIndicator> healthIndicators) {

        CompositeHealthIndicator healthIndicator = new CompositeHealthIndicator(healthAggregator);
        for (Integer i = 0; i < healthIndicators.size(); i++) {
            healthIndicator.addHealthIndicator(i.toString(), healthIndicators.get(i));
        }
        return new HealthMetrics(healthIndicator, healthIndicators);
    }


    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment) {
        String sysProjectName = environment.getProperty("project.name");
        String appName = Optional.ofNullable(sysProjectName).orElseGet(() -> {
            String applicationName = environment.getProperty("application.name");
            return applicationName;
        });

        if (appName == null || appName.isEmpty())
            throw new RuntimeException("must configure the application.name in your application.properties");

        return registry -> registry.config().commonTags("application", appName);
    }

    @Bean
    ConsulCenterRegisterRunner createRunner(Environment environment) {
        return new ConsulCenterRegisterRunner(environment);
    }
}
