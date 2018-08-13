package com.dianwoda.middleware.prometheus.consul;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.concurrent.Executors;


public class ConsulCenterRegisterRunner implements CommandLineRunner {

    private static Logger logger = LoggerFactory.getLogger(ConsulCenterRegisterRunner.class);

    private Environment env;

    public ConsulCenterRegisterRunner(Environment env) {
        this.env = env;
    }

    private static final String CONSUL_ADDR = "http://127.0.0.1:8500";

    private static final int MAX_RETRAY_TIMES = 3;

    @Override
    public void run(String... args) {
        Executors.newSingleThreadExecutor().execute(() -> {
            int i = 0;
            while (i < MAX_RETRAY_TIMES) {
                try {
                    Consul client = Consul.builder().withUrl(CONSUL_ADDR).build();
                    KeyValueClient kvClient = client.keyValueClient();

                    InetAddress localHost = InetAddress.getLocalHost();
                    String hostAddress = localHost.getHostAddress();

                    String serverPort = env.getProperty("local.server.port");
                    if (StringUtils.isEmpty(serverPort)) {
                        throw new RuntimeException("invalid port");
                    }

                    String ipAddr = String.format("%s:%s", hostAddress, serverPort);


                    String key = "/prometheus/serv/" + ipAddr;
                    kvClient.putValue(key, "");

                    i = MAX_RETRAY_TIMES;
                } catch (Exception e) {
                    logger.error("reigster occur error", e);
                    i++;
                }
            }
        });
    }
}
