# DMS Integration

## Development environment
Prerequisites:
- Java 17 JDK
- Maven
- VOCES certificate

## Secrets
Secrets are stored in the directory `resources/security` which is not under source control.
This means that when the repository is pulled to a new machine these files need to be added manually.

Specifically the following files are stored in `resources/security`:
### as4-crypto.properties
```properties
# Company Keystore
org.apache.wss4j.crypto.merlin.keystore.file=<path to certificate file>
org.apache.wss4j.crypto.merlin.keystore.password=<certificate password>
org.apache.wss4j.crypto.merlin.keystore.type=PKCS12
org.apache.wss4j.crypto.merlin.keystore.alias=DK COMPANY A/S - DK Company A/S
org.apache.wss4j.crypto.merlin.keystore.private.password=<private key password>
```
### certificate.properties
### The certificate file