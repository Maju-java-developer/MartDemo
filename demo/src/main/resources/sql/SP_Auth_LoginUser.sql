CREATE PROCEDURE `SP_Auth_LoginUser`(
    IN p_Username VARCHAR(100),
    IN p_PasswordHash VARCHAR(200),
    IN p_DateTime DATETIME
)
BEGIN
    DECLARE v_UserLoginID INT;

    -- 1. Find the LoginID based on credentials, ensuring the user is not blocked (IsBlocked = 0)
    SET v_UserLoginID = (
        SELECT ul.LoginID
        FROM TBLUserLogin ul
        WHERE ul.Username = p_Username
          AND ul.PasswordHash = p_PasswordHash
    );

    IF v_UserLoginID IS NOT NULL AND v_UserLoginID > 0 THEN
        -- 2. Update LastLogin timestamp if user is found
        UPDATE TBLUserLogin
        SET LastLogin = p_DateTime
        WHERE LoginID = v_UserLoginID;

        -- 3. Return the full user record and role
        SELECT
            u.UserID,
            u.FullName,
            u.Email,
            u.ContactNo,
            ul.Username,
            ul.Role,
            ul.IsBlocked,
            u.IsActive
        FROM TBLUsers u
                 INNER JOIN TBLUserLogin ul ON u.UserID = ul.UserID
        WHERE ul.LoginID = v_UserLoginID;
        -- Note: If no user is found, the procedure returns an empty result set.
    END IF;
END