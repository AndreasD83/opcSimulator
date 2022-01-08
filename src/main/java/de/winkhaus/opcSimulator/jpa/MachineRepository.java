package de.winkhaus.opcSimulator.jpa;

import de.winkhaus.opcSimulator.model.Machine;
import de.winkhaus.opcSimulator.model.Status;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;

@Transactional
public interface MachineRepository extends CrudRepository<Machine, Integer> {
        Machine findByMachineId(String id);
}
