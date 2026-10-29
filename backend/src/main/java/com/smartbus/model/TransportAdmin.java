package com.smartbus.model;

import jakarta.persistence.*;

/** Operations staff who monitor the fleet and act on demand forecasts. */
@Entity
@Table(name = "transport_admins")
public class TransportAdmin extends User {

    @Column(name = "employee_code", unique = true)
    private String employeeCode;

    private String depot;

    protected TransportAdmin() {}

    public TransportAdmin(String name, String email, String passwordHash, String employeeCode, String depot) {
        super(name, email, passwordHash);
        this.employeeCode = employeeCode;
        this.depot = depot;
    }

    @Override
    public String getRole() { return "ADMIN"; }

    public String getEmployeeCode() { return employeeCode; }
    public String getDepot() { return depot; }
}
