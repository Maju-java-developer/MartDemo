package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLedgerModel {

    private int ledgerID;
    private int productID;
    private String refType;      // 'PURCHASE', 'SALE', etc.
    private int refID;           // SaleID or PurchaseID
    private Integer refDetailID; // SaleDetailID or PurchaseDetailID
    private double qtyIn;
    private double qtyOut;
    private double rate;         // Price used for this transaction
    private LocalDateTime createdDate;
}