package de.winkhaus.opcSimulator.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "machine")
public class ConfigProperties {
    @Value("#{'${machines}'.split(',')}")
    private List<String> machines;

    public List<String> getMachines() {
        return machines;
    }

    public void setMachines(List<String> machines) {
        this.machines = machines;
    }
}
