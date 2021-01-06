CREATE TABLE `ACCOUNTS` (
  `account_id` int NOT NULL AUTO_INCREMENT,
  `account_type` int DEFAULT NULL,
  `first_name` varchar(45) DEFAULT NULL,
  `last_name` varchar(45) DEFAULT NULL,
  `mobile_phone` varchar(45) DEFAULT NULL,
  `email` varchar(45) DEFAULT NULL,
  `password` varchar(45) DEFAULT NULL,
  `username` varchar(45) NOT NULL,
  PRIMARY KEY (`account_id`,`username`),
  UNIQUE KEY `account_id_UNIQUE` (`account_id`)
);

CREATE TABLE `ACCOUNT_TYPES` (
  `account_type` int NOT NULL AUTO_INCREMENT,
  `description` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`account_type`),
  UNIQUE KEY `account_type_UNIQUE` (`account_type`)
) ;

DELIMITER $$
CREATE PROCEDURE `spGetAllAccounts`()
BEGIN
	SELECT account_id
    FROM ACCOUNTS;
END$$

CREATE PROCEDURE `spGetAccountJSON`(
	IN acctId INT
    )
BEGIN
SELECT JSON_OBJECT( 
	'schemas', JSON_ARRAY('urn:scim:schemas:core:1.0'),
    'totalResults', 1,
	'Resources', JSON_ARRAYAGG(
	JSON_OBJECT('schemas', JSON_ARRAY('urn:scim:schemas:core:1.0', 'urn:scim:schemas:extension:enterprise:1.0'),
    'id', CAST(account_id as CHAR(10)), 'userName', username, 'password', password, 'name', 
    JSON_OBJECT('formatted', CONCAT(first_name, ' ', last_name), 
	'givenName', first_name, 'familyName', last_name), 'emails', 
    JSON_ARRAY(JSON_OBJECT('value', email, 'primary', 'true', 'type', 'work')))) )
	FROM ACCOUNTS
    WHERE account_id = acctId;
END$$

CREATE PROCEDURE `spSaveAccount`(
	IN json VARCHAR(500),
    OUT result INT
    )
BEGIN
    SET result = 0;
	SET @username = JSON_UNQUOTE(JSON_EXTRACT(json, '$.Resources[0].userName'));
	SET @firstname = JSON_UNQUOTE(JSON_EXTRACT(json, '$.Resources[0].name.givenName'));
	SET @lastname = JSON_UNQUOTE(JSON_EXTRACT(json, '$.Resources[0].name.familyName'));
	SET @email = JSON_UNQUOTE(JSON_EXTRACT(json, '$.Resources[0].emails[0].value'));
	SET @mobile = JSON_UNQUOTE(JSON_EXTRACT(json, '$.Resources[0].phoneNumbers[0].value'));
	SET @pwd = JSON_UNQUOTE(JSON_EXTRACT(json, '$.Resources[0].password'));
    
    SET @accountid = (SELECT account_id FROM ACCOUNTS WHERE username = @username);
    SELECT @username, @firstname, @lastname, @email, @mobile, @pwd, @accountid;
    
    IF @accountid > 0 THEN
		UPDATE CCOUNTS
		SET username = @username,
			first_name = COALESCE(@firstname, first_name),
			last_name = COALESCE(@lastname, last_name),
			email = COALESCE(@email, email),
			mobile_phone = COALESCE(@mobile, mobile_phone),
			password = COALESCE(@pwd, password)
		WHERE account_id = @accountid;
        SET result = 1;
    ELSE
		INSERT INTO ACCOUNTS(account_type, first_name, last_name, mobile_phone, email, password, username)
        VALUES ('1', @firstname, @lastname, @mobile, @email, @pwd, @username);
        SET result = 1;
    END IF;
    COMMIT;
END$$

DELIMITER ;

