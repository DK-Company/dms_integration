# Guide for using the alpha tool

## Certificate
The OCES3 certificate must be located on the computer running the tool.

Take note of the location of the certificate - the absolute path of the file must be used as the value of the `oces3.file` environment variable.

## Environment variables
The following environment variables must be set:
- oces3.file
- oces3.password
- oces3.type
- oces3.alias
- oces3.privatePassword

## The application.properties file
When running the jar, a file named `application.properties` must be placed in the same directory of the jar.

This file contains the configuration of the program:
```properties
directoryPaths=C:\\Path\\To\\Directory1;C:\\Path\\To\\Directory2
```

- `directoryPaths`: list of directories where files are picked up from. Separated by semicolons.