package de.winkhaus.opcSimulator.controller;

import de.winkhaus.opcSimulator.jpa.MachineRepository;
import de.winkhaus.opcSimulator.model.Counter;
import de.winkhaus.opcSimulator.model.Machine;
import de.winkhaus.opcSimulator.model.Message;
import de.winkhaus.opcSimulator.model.Status;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Api(value = "/", description = "Maschinenansteuerung", tags = "Maschinen")
@RestController
public class OpcController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private MachineRepository repository;

    @ApiOperation(value = "Liste aller Maschinen",
            notes = "Liste aller konfigurierten Maschinen zurückgeben"
    )
    @CrossOrigin(origins = "*")
    @GetMapping("/machines")
    List<Machine> all() {
        return (List<Machine>) repository.findAll();
    }

    @ApiOperation(value = "Maschine",
            notes = "MAschine zu ID zurückgeben"
    )
    @GetMapping("/machine/{id}")
    public Machine machine(@ApiParam(value = "ID der Maschine", required = true) @PathVariable("id") String id) {
        return machineById(id);
    }

    @ApiOperation(value = "Maschine stoppen",
            notes = "Maschine mit ID stoppen"
    )
    @PostMapping("/stop/{id}")
    public Status stop(@ApiParam(value = "ID der Maschine", required = true) @PathVariable("id") String id) {
        if(logger.isInfoEnabled()){
            logger.info("stop {}", id);
        }
        Machine machine = machineById(id);
        machine.setStatus(new Status(false));
        repository.save(machine);
        return machine.getStatus();
    }

    @ApiOperation(value = "Maschine stoppen und Fehler setzen",
            notes = "Maschine mit ID stoppen und Fehler mit Fehlernummer setzen"
    )
    @PostMapping("/stop/{id}/{messageId}")
    public Status stop(@ApiParam(value = "ID der Maschine", required = true) @PathVariable("id") String id, @ApiParam(value = "ID der Fehlermeldung", required = true) @PathVariable("messageId") String messageId) {
        if(logger.isInfoEnabled()){
            logger.info("error {},{}", id, messageId);
        }
        Machine machine = machineById(id);
        machine.setStatus(new Status(false));
        machine.setMessage(new Message(new Integer(messageId)));
        repository.save(machine);
        return machine.getStatus();
    }

    @ApiOperation(value = "Maschine starten",
            notes = "Maschine mit ID starten"
    )
    @PostMapping("/start/{id}")
    public Status start(@ApiParam(value = "ID der Maschine", required = true) @PathVariable("id") String id) {
        if(logger.isInfoEnabled()){
            logger.info("start {}", id);
        }
        Machine machine = machineById(id);
        machine.setStatus(new Status(true));
        machine.setCounter(new Counter());
        repository.save(machine);
        return machine.getStatus();
    }

    @ApiOperation(value = "Maschinenstatus",
            notes = "Maschinenstatus: gestartet oder gestoppt"
    )
    @GetMapping("/status/{id}")
    public Status status(@ApiParam(value = "ID der Maschine", required = true) @PathVariable("id") String id) {
        if(logger.isDebugEnabled()){
            logger.debug("status {}", id);
        }
        return machineById(id).getStatus();
    }

    @ApiOperation(value = "Stückzähler",
            notes = "Stückzähler: Anzahl Stück"
    )
    @GetMapping("/counter/{id}")
    public Counter counter(@ApiParam(value = "ID der Maschine", required = true) @PathVariable("id") String id) {
        if(logger.isDebugEnabled()){
            logger.debug("counter {}", id);
        }
        return machineById(id).getCounter();
    }


    private Machine machineById(String id) throws ResponseStatusException{
        Machine result = repository.findByMachineId(id);
        if(null == result){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "machine Not Found");
        }
        return result;
    }

}
