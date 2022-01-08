package de.winkhaus.opcSimulator.thread;

import de.winkhaus.opcSimulator.jpa.MachineRepository;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IncrementCounterThread extends Thread {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private MachineRepository repository;

    private Integer interval = 10000;
    private int counterIncrease = 10;
    private int rejectAmountIncrease = 1;

    @SneakyThrows
    public void run(){
        logger.info(String.format("start counter: %d ms", interval));

        while(true){
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
            repository.findAll().forEach(machine -> {
                if(machine.getStatus().isActive()){
                    Double oldValue = machine.getCounter().getPieces();
                    Double newValue = oldValue + Double.valueOf(counterIncrease);
                    machine.getCounter().setPieces(newValue);
                    logger.debug(String.format("increment counter %s, old: %f, new: %f", machine.getMachineId(), oldValue, newValue));

                    oldValue = machine.getRejectAmountCounter().getPieces();
                    newValue = oldValue + Double.valueOf(rejectAmountIncrease);
                    machine.getRejectAmountCounter().setPieces(newValue);
                    logger.debug(String.format("increment rejectAmount %s, old: %f, new: %f", machine.getMachineId(), oldValue, newValue));


                    repository.save(machine);
                } else{
                    logger.debug(String.format("NOT increment %s, old: %f", machine.getMachineId(), machine.getCounter().getPieces()));
                }
            } );

        }
    }
}