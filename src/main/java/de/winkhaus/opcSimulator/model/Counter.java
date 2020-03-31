package de.winkhaus.opcSimulator.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.math.BigInteger;

@Entity
public class Counter {
    private Double pieces;

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    public Counter() {
        this.pieces = new Double("0");
    }

    public Double getPieces() {
        return pieces;
    }

    public void setPieces(Double pieces) {
        this.pieces = pieces;
    }
}
