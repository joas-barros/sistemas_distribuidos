package edu.unidade2.servicediscovery.src.entities;

import java.io.Serializable;

public class Calculation implements Serializable {
	private static final long serialVersionUID = 1L;

    private Operation operation;
    private Double arg1;
    private Double arg2;
    private int clientId;

    public Calculation(Operation operation, Double arg1, Double arg2, int clientId) {
        this.operation = operation;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.clientId = clientId;
    }

    public Operation getOperation() {
        return operation;
    }

    public Double getArg1() {
        return arg1;
    }

    public Double getArg2() {
        return arg2;
    }

    public int getClientId() {
        return clientId;
    }
}
