# Guide for setting up development enviroment for the tool

## setting up IDE

For this guide the IDE intelliJ will be used.  
Firstly download intelliJ from the official website (scroll down to find the free community edition if you don't have a license)  
When the IDE is installed, open the folder containing the source code inside IntelliJ.
  
Now IntelliJ might ask to install a JDK if there is no JDK present on the machine. Allow IntelliJ to download the newest JDk available.  
After this step you should be able to start running the application - however the application will fail since no path to configs has been defined yet.  

The tool uses lombok inside the spring boot application. When opening in a developent enviroment, you will need to install the lombok plugin.
To install Lombok, go to settings -> plugin and under marketplace make sure that the Lombok plugin is installed. After installation the IDE will need a restart.

<img width="572" alt="image" src="https://github.com/DK-Company-A-S/dms_integration/assets/80399524/c39508e2-8616-496e-be28-0d610f4de6b7">


under src/main/resources you will find the application.properties file. This file should contain the configpath, which tells the program where to find the config files that needs to be set up (check the usage guide for more details)  

<img width="396" alt="image" src="https://github.com/DK-Company-A-S/dms_integration/assets/80399524/444637ac-49a2-4970-92e5-c0a28aa1974d">


## packaging a fat .jar file

To compile and package jars this application is using maven. inside the intelliJ IDE there is an "m" button on the right side, where expanding "dmsintegration" and then "lifecycle" will show the user a list of commands that maven can run.  
The commands that we will need are "clean" and "package".  
the clean command will cleanup any leftovers from a previous run and also delete the .jar file present in the "target" folder (if there is one)  
after running "clean", "package" can be run. This package command has been modified using a plugin (plugin is found in the pom.xml), which makes the package command package a "fat jar" instead of a regular jar.  
In short, this means that the dependencies the application uses will be downloaded and packaged inside the jar, instead of being installed dynamically at runtime.  

<img width="292" alt="image" src="https://github.com/DK-Company-A-S/dms_integration/assets/80399524/6cbd2718-99f9-4f0b-882e-3add9b68f4d6">

when the program has been packaged. it can be found inside the "target" folder in the source code directory. Look for a .jar called dms_integration-versionnumber-jackofall.jar. This is the fat jar created.  

<img width="698" alt="image" src="https://github.com/DK-Company-A-S/dms_integration/assets/80399524/5f18a519-b5e9-4e9a-a6fe-205c75dc0a58">


Now that a .jar file is created, it can be added to a directory containing an application.properties, and the program is ready to be run.  

<img width="312" alt="image" src="https://github.com/DK-Company-A-S/dms_integration/assets/80399524/4a729363-65f6-4602-b9b6-a938654eaeea">

For a full guide on how to use the tool after packaging the jar, check the usage guide.  
