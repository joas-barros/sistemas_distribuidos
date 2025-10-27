package edu.unidade2.overleay.entities;

import java.io.Serializable;

public class Calculation implements Serializable {
	private static final long serialVersionUID = 1L;

    private Operation operation;
    private Double arg1;
    private Double arg2;

    public Calculation(Operation operation, Double arg1, Double arg2) {
        this.operation = operation;
        this.arg1 = arg1;
        this.arg2 = arg2;
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
}
