;;; First load the JDBC driver and open the target-database.

; for hsql (OJB default)
; d org.hsqldb.jdbcDriver;
; o jdbc:hsqldb:../OJB sa;

; for instantDB
d org.enhydra.instantdb.jdbc.idbDriver;
o jdbc:idb:tracker.prp;

;for DB2
;d COM.ibm.db2.jdbc.app.DB2Driver;
;o jdbc:db2:dbwte001 db2admin db2;

;for Oracle
;d oracle.jdbc.driver.OracleDriver;
;o jdbc:oracle:oci8:@(description=(address=(host=127.0.0.1)(protocol=tcp)(port=1521))(connect_data=(sid=orcl))) scott tiger;

;for PostgreSQL
;d org.postgresql.Driver;
;o jdbc:postgresql:ojbdemodb username passwd;

;for mySQL
;d org.gjt.mm.mysql.Driver;
;o jdbc:mysql://localhost:3306/test username passwd


;;;; THE DSET IMPLEMENTATION
;;;;
; create PEERS table
e DROP TABLE PEERS;
e CREATE TABLE PEERS (
	peer_id     	BINARY(20) PRIMARY KEY NOT NULL,
	peer_ip		BINARY(15),
	port		INT,
	info_hash	BINARY(20),
	last_access	LONG,
	downloaded	LONG,
	uploaded	LONG,
	peer_cache	BINARY(4096)
);

c close;









