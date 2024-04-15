# Guide for quickly setting up the tool

Inside the root folder there is a .zip file called dms_integration.zip. This zip contains the full setup of the tool. to get started using the tool these are the steps to go through:  

Extract zip  
add certificate
update application.properties
update config-file
optional: add more config files

# Extract zip

Go to the directory where you wish the root of the project to be located. here, extract the project and you will see a .jar file, a folder called configs and a application.properties file.

# add certificate

the next step is to add you certificate to the directory. This certificate should be registered at DMS for the enviroment you wish to use (test/prod). After registering you will get a gatewaypassword that you also need for later.

# update application.properties

Inside application.properties you will find two fields. The first is as4Endpoint. The program defaults to the test enviroment, so it can be excluded if you are running in the test enviroment. If you are running a prod enviroment, you will need to set as4Endpoint to https://secureftpgateway.skat.dk:6384. 
The second field inside application.properties is configPath. This field contains the path to the directory containining the .config files. For this example the zip contains this folder, and the field should be set to the path of the configs folder.

# update config-file

Inside the configs folder, there is a file called 1.config. The program will dynamically create clients for each config file in this folder. For this example we will have only the one .config file, but feel free to add more if you are running mulitple CVR numbers in the same system.

these are the fields in the .config file:

- `certificatePrefix` (unique name used to differentiate between clients internally in the program)  
- `file` (path to the directory containing the certificate file)  
- `password` (the password to access the certificate)  
- `type` (the type of certificate used (fx PKCS12))  
- `alias` (the alias used to initially create the certificate)  
- `privatePassword` (if certificate type supports multiple passwords, this contains the private password. If there is only password, this is usually the same as password)  
- `gatewayPassword` (the gateway password, which can be retrieved from DMS by registraring the certificate and acessing their toolkit)  
- `notificationQueueURL` (the URL to the notification queue inside the DMS system. the endpoint looks like this: urn:fdc:dk.skat.mft.DMS/response/CVR_{CVR_NUMBER})  
- `directoryPath` (path to the directory where the user wants the directories (in, out, success, error) to be created) 
- `cvr` company CRV number


these fields needs to be filled with the appropriate values for the client to be set up. 

# running the application

now to run the application the .jar file can simply be double clicked. This will open the application in the background, creating the folders where the directoryPath points to. The console log will be available in the logs directory.
Alternatively you can run it through a terminal using this command:  
`java "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006" -jar dmsintegration-0.0.1-jackofall.jar`

This will run the client through a terminal so you can follow along with what the application is setting up.