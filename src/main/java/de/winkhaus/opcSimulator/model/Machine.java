package de.winkhaus.opcSimulator.model;

import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.*;

@Entity
public class Machine {

    protected Machine(){}
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    private String machineId;

    private static Integer basePort = 8080;
    private static Integer basePortHttp = 8443;
    private Integer port;
    private Integer httpsPort;

    @OneToOne(cascade = CascadeType.ALL)
    private Status status;

    @OneToOne(cascade = CascadeType.ALL)
    private Counter counter;

    @OneToOne(cascade = CascadeType.ALL)
    private Message message;


    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Counter getCounter() {
        return counter;
    }

    public void setCounter(Counter counter) {
        this.counter = counter;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
    }

    public Machine(String machineId) {
        this.machineId = machineId;
        this.status = new Status();
        this.message = new Message();
        this.counter = new Counter();
        this.port = basePort++;
        this.httpsPort = basePortHttp++;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
