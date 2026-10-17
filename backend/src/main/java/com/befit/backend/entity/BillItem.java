package com.befit.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** One billable line on an invoice (service / product, quantity, unit price). */
@Entity
@Table(name = "bill_items", indexes = { @Index(name = "idx_bill_items_bill", columnList = "bill_id") })
public class BillItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "bill_id", nullable = false) private Bill bill;
    @Column(nullable = false, length = 255) private String description;
    @Column(nullable = false) private int quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2) private BigDecimal lineTotal;
    @Column(name = "sort_order") private int position;

    public BillItem() {}
    public BillItem(Bill bill, String description, int quantity, BigDecimal unitPrice, BigDecimal lineTotal, int position) {
        this.bill = bill; this.description = description; this.quantity = quantity;
        this.unitPrice = unitPrice; this.lineTotal = lineTotal; this.position = position;
    }

    public Long getId() { return id; }
    public Bill getBill() { return bill; } public void setBill(Bill bill) { this.bill = bill; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public int getQuantity() { return quantity; } public void setQuantity(int v) { this.quantity = v; }
    public BigDecimal getUnitPrice() { return unitPrice; } public void setUnitPrice(BigDecimal v) { this.unitPrice = v; }
    public BigDecimal getLineTotal() { return lineTotal; } public void setLineTotal(BigDecimal v) { this.lineTotal = v; }
    public int getPosition() { return position; } public void setPosition(int v) { this.position = v; }
}
