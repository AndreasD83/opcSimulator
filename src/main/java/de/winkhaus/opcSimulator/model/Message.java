package de.winkhaus.opcSimulator.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Message {
    private Integer number;
    private String text;

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    public Message(Integer number){
        this.number = number;
    }

    public Message(){
        this.number = null;
        this.text = null;
    }
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
