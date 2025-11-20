DELIMITER $$

DROP PROCEDURE IF EXISTS SP_IUD_Sale$$

CREATE PROCEDURE SP_IUD_Sale(
    INOUT p_SaleID INT,               -- 1: NULL for Save, existing ID for Update/Delete
    IN p_CustomerID INT,              -- 2
    IN p_SaleDate DATETIME,           -- 3
    INOUT p_InvoiceNo VARCHAR(50),    -- 4: NULL for Save, existing InvoiceNo for Update/Delete
    IN p_ActualAmount DECIMAL(18,2),  -- 5
    IN p_GSTPercentage DECIMAL(18,2), -- 6
    IN p_GSTAmount DECIMAL(18,2),     -- 7
    IN p_DiscountType VARCHAR(50),    -- 8
    IN p_Discount DECIMAL(18,2),      -- 9
    IN p_TotalAmount DECIMAL(18,2),   -- 10: Net value after discount/tax
    IN p_ReceivedAmount DECIMAL(18,2),-- 11
    IN p_Remarks VARCHAR(255),        -- 12
    IN p_DateTime DATETIME,           -- 13: Created/Updated Date
    IN p_UserID INT,                  -- 14
    IN p_Status VARCHAR(50),          -- 15: 'Save', 'Update', 'Delete'
    IN p_DetailsJson JSON,            -- 16: Sale details as JSON array
    OUT p_CheckReturn INT             -- 17: Return ID/Status code
)
BEGIN
    DECLARE v_NetChange DECIMAL(18,2);

    -- Variables for JSON loop
    DECLARE i INT DEFAULT 0;
    DECLARE v_json_length INT;
    DECLARE v_product_id INT;
    DECLARE v_quantity DECIMAL(18, 2);
    DECLARE v_rate DECIMAL(18, 2);
    DECLARE v_product_discount DECIMAL(18, 2);
    DECLARE v_total DECIMAL(18, 2);
    DECLARE v_detail_id INT;

    -- Variables for Reversal
    DECLARE v_OldTotalAmount DECIMAL(18,2);
    DECLARE v_OldReceivedAmount DECIMAL(18,2);
    DECLARE v_OldCustomerID INT;
    DECLARE v_OldNetChange DECIMAL(18,2);
    DECLARE v_CheckId INT;

    SET p_CheckReturn = 0;

    -- Check if SaleID exists for Update/Delete
    IF (p_Status = 'Update' OR p_Status = 'Delete') THEN
        SELECT COUNT(*) INTO v_CheckId FROM TBLSale WHERE SaleID = p_SaleID;
        IF v_CheckId = 0 THEN
            SET p_CheckReturn = -4; -- Not Found
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: Sale record not found for Update/Delete.';
        END IF;
    END IF;

    START TRANSACTION;

    -- ===========================================
    -- 🔄 REVERSAL LOGIC (Used by Update and Delete)
    -- ===========================================
    IF (p_Status = 'Update' OR p_Status = 'Delete') THEN

        -- Get old amounts and Customer ID
        SELECT TotalAmount, ReceivedAmount, CustomerID
        INTO v_OldTotalAmount, v_OldReceivedAmount, v_OldCustomerID
        FROM TBLSale WHERE SaleID = p_SaleID;

        -- 1. Reverse Customer Balance Change
        -- Sale increases OutstandingBalance (Receivable).
        -- Net Change = Total - Received.
        -- To reverse, we SUBTRACT the Old Net Change.
        SET v_OldNetChange = v_OldTotalAmount - v_OldReceivedAmount;
        UPDATE TBLCustomers
        SET OpeningBalance = OpeningBalance - v_OldNetChange
        WHERE CustomerID = v_OldCustomerID;

        -- 2. Reverse Stock Ledger Entries
        -- Sale creates 'SALE' entries (QtyOut). We delete them to reverse.
        DELETE FROM TBLStockLedger
        WHERE RefID = p_SaleID AND RefType = 'SALE';

        -- 3. Delete old Sale Details
        DELETE FROM TBLSaleDetail
        WHERE SaleID = p_SaleID;

    END IF;

    -- ===========================================
    -- ✅ SAVE (INSERT)
    -- ===========================================
    IF p_Status = 'Save' THEN

        -- 1. Invoice Generation
        SET p_InvoiceNo = fnGetNextInvoiceNo();

        -- 2. Insert Sale Header
        INSERT INTO TBLSale (
            CustomerID, SaleDate, InvoiceNo, ActualAmount,
            GSTPercentage, GSTAmount, DiscountType, Discount,
            TotalAmount, ReceivedAmount, Remarks, CreatedDate, CreatedBy
        )
        VALUES (
            p_CustomerID, p_SaleDate, p_InvoiceNo, p_ActualAmount,
            p_GSTPercentage, p_GSTAmount, p_DiscountType, p_Discount,
            p_TotalAmount, p_ReceivedAmount, p_Remarks, p_DateTime, p_UserID
        );

        SET p_SaleID = LAST_INSERT_ID();

        -- 3. Update Customer Balance (New Change)
        -- Sale increases OutstandingBalance.
        SET v_NetChange = p_TotalAmount - p_ReceivedAmount;
        UPDATE TBLCustomers SET OpeningBalance = OpeningBalance + v_NetChange WHERE CustomerID = p_CustomerID;

        -- 4. Insert Sale Details and Stock Ledger
        IF p_DetailsJson IS NOT NULL THEN
            SET v_json_length = JSON_LENGTH(p_DetailsJson);
            IF v_json_length = 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: Sale details list cannot be empty.'; END IF;

            WHILE i < v_json_length DO
                SET v_product_id = JSON_UNQUOTE(JSON_EXTRACT(p_DetailsJson, CONCAT('$[', i, '].productID')));
                SET v_quantity = JSON_UNQUOTE(JSON_EXTRACT(p_DetailsJson, CONCAT('$[', i, '].quantity')));
                SET v_rate = JSON_UNQUOTE(JSON_EXTRACT(p_DetailsJson, CONCAT('$[', i, '].rate')));
                SET v_product_discount = JSON_UNQUOTE(JSON_EXTRACT(p_DetailsJson, CONCAT('$[', i, '].productDiscount')));
                SET v_total = JSON_UNQUOTE(JSON_EXTRACT(p_DetailsJson, CONCAT('$[', i, '].total')));

                INSERT INTO TBLSaleDetail (SaleID, ProductID, Quantity, Rate, ProductDiscount, Total)
                VALUES (p_SaleID, v_product_id, v_quantity, v_rate, v_product_discount, v_total);

                SET v_detail_id = LAST_INSERT_ID();

                -- Insert Stock Ledger (QtyOut)
                INSERT INTO TBLStockLedger (ProductID, RefType, RefID, RefDetailID, QtyOut, Rate)
                VALUES (v_product_id, 'SALE', p_SaleID, v_detail_id, v_quantity, v_rate);

                SET i = i + 1;
            END WHILE;
        END IF;

        SET p_CheckReturn = p_SaleID;

    -- ===========================================
    -- 🔄 UPDATE
    -- ===========================================
    ELSEIF p_Status = 'Update' THEN

        -- 1. Update Sale Header
        UPDATE TBLSale
        SET
            CustomerID = p_CustomerID,
            SaleDate = p_SaleDate,
            ActualAmount = p_ActualAmount,
            GSTPercentage = p_GSTPercentage,
            GSTAmount = p_GSTAmount,
            DiscountType = p_DiscountType,
            Discount = p_Discount,
            TotalAmount = p_TotalAmount,
            ReceivedAmount = p_ReceivedAmount,
            Remarks = p_Remarks,
            UpdatedDate = p_DateTime,
            UpdatedBy = p_UserID
        WHERE SaleID = p_SaleID;

        -- 2. Update Customer Balance (Apply NEW Net Change)
        SET v_NetChange = p_TotalAmount - p_ReceivedAmount;
        UPDATE TBLCustomers SET OpeningBalance = OpeningBalance + v_NetChange WHERE CustomerID = p_CustomerID;

        -- 3. Insert NEW Sale Details and Stock Ledger
        SET i = 0;
        IF p_DetailsJson IS NOT NULL THEN
            SET v_json_length = JSON_LENGTH(p_DetailsJson);
            IF v_json_length = 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: Sale details list cannot be empty during Update.'; END IF;

            WHILE i < v_json_length DO
                SET v_product_id = JSON_UNQUOTE(JSON_EXTRACT(p_DetailsJson, CONCAT('$[', i, '].productID')));
                SET v_quantity = JSON_UNQUOTE(JSON_EXTRACT(p_DetailsJson, CONCAT('$[', i, '].quantity')));
                SET v_rate = JSON_UNQUOTE(JSON_EXTRACT(p_DetailsJson, CONCAT('$[', i, '].rate')));
                SET v_product_discount = JSON_UNQUOTE(JSON_EXTRACT(p_DetailsJson, CONCAT('$[', i, '].productDiscount')));
                SET v_total = JSON_UNQUOTE(JSON_EXTRACT(p_DetailsJson, CONCAT('$[', i, '].total')));

                INSERT INTO TBLSaleDetail (SaleID, ProductID, Quantity, Rate, ProductDiscount, Total)
                VALUES (p_SaleID, v_product_id, v_quantity, v_rate, v_product_discount, v_total);

                SET v_detail_id = LAST_INSERT_ID();

                INSERT INTO TBLStockLedger (ProductID, RefType, RefID, RefDetailID, QtyOut, Rate)
                VALUES (v_product_id, 'SALE', p_SaleID, v_detail_id, v_quantity, v_rate);

                SET i = i + 1;
            END WHILE;
        END IF;

        SET p_CheckReturn = -1; -- Updated Successfully

    -- ===========================================
    -- ❌ DELETE
    -- ===========================================
    ELSEIF p_Status = 'Delete' THEN

        -- 1. Delete Sale Header
        DELETE FROM TBLSale WHERE SaleID = p_SaleID;

        SET p_CheckReturn = -2; -- Deleted Successfully

    END IF;

    -- Final Check and Transaction Management
    IF p_CheckReturn > 0 OR p_CheckReturn IN (-1, -2) THEN
        COMMIT;
    ELSE
        ROLLBACK;
    END IF;

END$$

DELIMITER ;
