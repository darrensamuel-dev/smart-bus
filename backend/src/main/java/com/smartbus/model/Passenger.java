package com.smartbus.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** A rider with a smart card and a prepaid wallet. */
@Entity
@Table(name = "passengers")
public class Passenger extends User {

    @Column(name = "card_number", unique = true)
    private String cardNumber;

    @Column(name = "wallet_balance", nullable = false)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    protected Passenger() {}

    public Passenger(String name, String email, String passwordHash, String cardNumber) {
        super(name, email, passwordHash);
        this.cardNumber = cardNumber;
    }

    @Override
    public String getRole() { return "PASSENGER"; }

    public void topUp(BigDecimal amount) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("Top-up must be positive");
        walletBalance = walletBalance.add(amount);
    }

    /** Encapsulation: balance can only change through validated methods. */
    public boolean deductFare(BigDecimal fare) {
        if (walletBalance.compareTo(fare) < 0) return false;
        walletBalance = walletBalance.subtract(fare);
        return true;
    }

    public String getCardNumber() { return cardNumber; }
    public BigDecimal getWalletBalance() { return walletBalance; }
}
