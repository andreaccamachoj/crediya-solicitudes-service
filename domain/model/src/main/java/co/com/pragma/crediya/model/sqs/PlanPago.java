package co.com.pragma.crediya.model.sqs;

import lombok.Data;

@Data
public class PlanPago {
    private int mes;
    private double cuota;
    private double interes;
    private double abonoCapital;
    private double saldoFinal;
}
