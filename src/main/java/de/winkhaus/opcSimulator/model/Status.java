package de.winkhaus.opcSimulator.model;


import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class Status {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    private boolean active;


    public Status() {
        this.active = true;
    }

    public Status(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Status{" +
                ", active=" + active +
                '}';
    }
}
