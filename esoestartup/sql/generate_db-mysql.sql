CREATE TABLE ENTITY_DESCRIPTORS (ENTITYID VARCHAR(100) NOT NULL, ORGANIZATIONNAME VARCHAR(1024) NOT NULL, ORGANIZATIONDISPLAYNAME VARCHAR(1024) NOT NULL, ORGANIZATIONURL VARCHAR(1024) NOT NULL, ACTIVEFLAG CHAR(1) NOT NULL, DESCRIPTORID VARCHAR(512), PRIMARY KEY (ENTITYID));

CREATE TABLE SERVICE_DESCRIPTIONS (ENTITYID VARCHAR(100) NOT NULL, SERVICENAME VARCHAR(1024) NOT NULL, SERVICEURL VARCHAR(2048) NOT NULL, SERVICEDESC VARCHAR(2048), AUTHZFAILUREMSG VARCHAR(2048), PRIMARY KEY (ENTITYID));

CREATE TABLE DESCRIPTORS (ENTITYID VARCHAR(100) NOT NULL, DESCRIPTORID VARCHAR(100) NOT NULL, DESCRIPTORXML LONGTEXT NOT NULL, DESCRIPTORTYPEID INT(2) NOT NULL, PRIMARY KEY(DESCRIPTORID));

CREATE TABLE SERVICE_ENDPOINTS (DESCRIPTORID VARCHAR(100) NOT NULL, ENDPOINTID VARCHAR(12) NOT NULL, NODEURL VARCHAR(512) NOT NULL, ASSERTIONCONSUMER_ENDPOINT VARCHAR(512) NOT NULL, SINGLELOGOUT_ENDPOINT VARCHAR(512) NOT NULL, CACHECLEAR_ENDPOINT VARCHAR(512) NOT NULL, PRIMARY KEY (ENDPOINTID, DESCRIPTORID));

CREATE TABLE DESCRIPTOR_TYPES (DESCRIPTORTYPEID INT(2) NOT NULL, DESCRIPTORTYPEDESCRIPTION VARCHAR(128) NOT NULL, PRIMARY KEY(DESCRIPTORTYPEID));
INSERT INTO DESCRIPTOR_TYPES VALUES (1, 'IDPSSODescriptor');
INSERT INTO DESCRIPTOR_TYPES VALUES (2, 'SPSSODescriptor');
INSERT INTO DESCRIPTOR_TYPES VALUES (3, 'LXACMLPDPDescriptor');
INSERT INTO DESCRIPTOR_TYPES VALUES (4, 'AuthNAuthorityDescriptor');
INSERT INTO DESCRIPTOR_TYPES VALUES (5, 'AttributeAuthorityDescriptor');

CREATE TABLE ENTITY_CONTACTS (CONTACTTYPE VARCHAR(255) NOT NULL CHECK(CONTACTTYPE IN ('technical', 'support', 'administrative', 'billing', 'other')), ENTITYID VARCHAR(100) NOT NULL, CONTACTID VARCHAR(12) NOT NULL, COMPANY VARCHAR(255), GIVENNAME VARCHAR(255) NOT NULL, SURNAME VARCHAR(255) NOT NULL, EMAILADDRESS VARCHAR(255) NOT NULL, TELEPHONENUMBER VARCHAR(255) NOT NULL, CONSTRAINT CONTACTTYPE PRIMARY KEY (CONTACTID, ENTITYID));

CREATE TABLE CLIENT_SESSIONS (SESSIONID VARCHAR(255) NOT NULL, DESCRIPTORID VARCHAR(512) NOT NULL, ENDPOINT VARCHAR(255) NOT NULL, USERIDENTIFIER VARCHAR(255) NOT NULL, DATESTART DATE NOT NULL, AUTHNMETHOD VARCHAR(255) NOT NULL, IPV4ADDRESS VARCHAR(32) NOT NULL, IPV6ADDRESS VARCHAR(128), DATESTOP DATE, PRIMARY KEY (SESSIONID));

CREATE TABLE SERVICE_POLICIES (DESCRIPTORID VARCHAR(100) NOT NULL, LXACMLPOLICY LONGTEXT NOT NULL, DATE_LAST_UPDATED DATE NOT NULL, PRIMARY KEY (DESCRIPTORID));

CREATE TABLE SERVICE_POLICIES_HISTORICAL (DESCRIPTORID VARCHAR(100) NOT NULL, LXACMLPOLICY LONGTEXT NOT NULL, DATE_INSERTED DATE NOT NULL);

CREATE TABLE SERVICE_POLICIES_SHUNT (DESCRIPTORID VARCHAR(100) NOT NULL, LXACMLPOLICY LONGTEXT NOT NULL, DATE_INSERTED DATE NOT NULL, PRIMARY KEY (DESCRIPTORID));

CREATE TABLE PKI_STORES (DESCRIPTORID VARCHAR(100) NOT NULL, EXPIRYDATE DATE NOT NULL, KEYPAIRNAME VARCHAR(255) NOT NULL UNIQUE, KEYPAIR_PASSPHRASE VARCHAR(255) NOT NULL UNIQUE, KEYSTORE BLOB, KEYSTORE_PASSPHRASE VARCHAR(255), PRIMARY KEY (DESCRIPTORID));

CREATE TABLE SPEP_REGISTRATIONS (DESCRIPTORID VARCHAR(100) NOT NULL, NODEID VARCHAR(100) NOT NULL, IPADDRESS VARCHAR(1024) NOT NULL, COMPILEDATE VARCHAR(30) NOT NULL, COMPILESYSTEM VARCHAR(512) NOT NULL, VERSION VARCHAR(100) NOT NULL, ENVIRONMENT VARCHAR(512) NOT NULL, DATE_ADDED DATE NOT NULL, DATE_UPDATED DATE NOT NULL, PRIMARY KEY (DESCRIPTORID, NODEID));

CREATE TABLE SPEP_REGISTRATIONS_HISTORY (DESCRIPTORID VARCHAR(100) NOT NULL, NODEID VARCHAR(100) NOT NULL, IPADDRESS VARCHAR(100) NOT NULL, COMPILEDATE VARCHAR(30) NOT NULL, COMPILESYSTEM VARCHAR(512) NOT NULL, VERSION VARCHAR(100) NOT NULL, ENVIRONMENT VARCHAR(512) NOT NULL, DATE_ADDED DATE NOT NULL);

ALTER TABLE ENTITY_CONTACTS ADD CONSTRAINT FKENTITY_CON930535 FOREIGN KEY (ENTITYID) REFERENCES ENTITY_DESCRIPTORS;
ALTER TABLE SERVICE_DESCRIPTIONS ADD CONSTRAINT FKSERVICE_DE602827 FOREIGN KEY (ENTITYID) REFERENCES ENTITY_DESCRIPTORS;
ALTER TABLE SERVICE_ENDPOINTS  ADD CONSTRAINT FKSERVICE_EN713736 FOREIGN KEY (DESCRIPTORID) REFERENCES DESCRIPTORS;
ALTER TABLE DESCRIPTORS ADD CONSTRAINT FKENTDESC FOREIGN KEY (ENTITYID) REFERENCES ENTITY_DESCRIPTORS;
ALTER TABLE PKI_STORES ADD CONSTRAINT FKPKISTOR332354 FOREIGN KEY (DESCRIPTORID) REFERENCES DESCRIPTORS;
ALTER TABLE SERVICE_POLICIES ADD CONSTRAINT FKSERVICE_PO306234 FOREIGN KEY (DESCRIPTORID) REFERENCES DESCRIPTORS;
ALTER TABLE CLIENT_SESSIONS ADD CONSTRAINT FKCLIENT_SES105275 FOREIGN KEY (DESCRIPTORID(100)) REFERENCES DESCRIPTORS;
ALTER TABLE SPEP_REGISTRATIONS ADD CONSTRAINT FKSPEP_REGIS65644 FOREIGN KEY (DESCRIPTORID) REFERENCES DESCRIPTORS;
ALTER TABLE SPEP_REGISTRATIONS_HISTORY ADD CONSTRAINT FKSPEP_REGIS647647 FOREIGN KEY (DESCRIPTORID) REFERENCES DESCRIPTORS;
ALTER TABLE DESCRIPTORS ADD CONSTRAINT FKDESCTYPE FOREIGN KEY (DESCRIPTORTYPEID) REFERENCES DESCRIPTOR_TYPES;
