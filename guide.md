# Guide for using the client

<img width="662" alt="DMS_Integration documentation" src="https://github.com/DK-Company-A-S/dms_integration/assets/80399524/fcbf0034-6098-4cd4-8468-46c38bbc4fa9">


## Application.properties
In the application.properties, a field should be added called configPath. This will contain the path to a folder containing a .config file for each wished instance of the client.  
If nothing is declared in the inner application.properties, another application.properties can be created to overwrite it in the root folder.  
Using a packaged .jar file, the application.properties can be added ot the same directory as the .jar file, and it will be automatically applied.  
For this program it is necessary to add the application.properties file as the configPath field is required to run properly.  

The application.properties should contain these fields:  
- `as4Endpoint` (the DMS endpoint the user wishes to hit fx. (https://secureftpgatewaytest.skat.dk:6384))  
- `configPath` (path to the congfigs folder)  

## Adding the config files
in the configs folder that should be found on the path referenced in the application.properties under the field configPath, .config files should be added.  
The .config file will contain the following information:  

- `certificatePrefix` (unique name used to differentiate between clients internally in the program)  
- `file` (path to the directory containing the certificate file)  
- `password` (the password to access the certificate)  
- `type` (the type of certificate used (fx PKCS12))  
- `alias` (the alias used to initially create the certificate)  
- `privatePassword` (if certificate type supports multiple passwords, this contains the private password. If there is only password, this is usually the same as password)  
- `gatewayPassword` (the gateway password, which can be retrieved from DMS by registraring the certificate and acessing their toolkit)  
- `notificationQueueURL` (the URL to the notification queue inside the DMS system. the endpoint looks like this: urn:fdc:dk.skat.mft.DMS/response/CVR_{CVR_NUMBER})  
- `directoryPath` (path to the directory where the user wants the directories (in, out, success, error) to be created)  

The config files will be read dynamically, so a client in setup for eat config file inside the configs folder. If a new config is added for a new CVR number, setting these fields will automatically setup a new client for the new CVR number the next time the program is restarted.  

## in/out/success/error directories
When the program starts it will create 4 directories.  

```
├── base directory (defined by directoryPath in .config file)
│   ├── error
│   ├── out
│   ├── in
│   ├── success
```

## Files to be picked up
Files that should be sent to DMS must follow a naming convention to be picked up by the tool.

```
<ProcedureType>_<DmsService>_<any string>.xml
```

- `ProcedureType`: the type of the declaration, e.g. _B1_, _H7_, etc.
- `DmsService`: values can be either _dms.export_ or _dms.import_
- `any string`: the last part of the filename can contain information such as a timestamp, etc.

`ProcedureType`, `DmsService` and the arbitrary string must be separated by underscores (`_`).

Files must have the `.xml` extension.
