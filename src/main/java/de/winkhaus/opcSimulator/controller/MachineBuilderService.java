package de.winkhaus.opcSimulator.controller;

import de.winkhaus.opcSimulator.jpa.MachineRepository;
import de.winkhaus.opcSimulator.model.Machine;
import org.apache.catalina.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@PropertySource("application.properties")
public class MachineBuilderService {

    @Value("#{'${machines}'.split(',')}")
    private List<String> machines = Arrays.asList(new String[]{"4711", "4712", "4713", "beckhoff-1"});

    @Autowired
    public MachineBuilderService(MachineRepository repository){
        machines.forEach(machine -> repository.save(new Machine(machine) ));
    }
}
