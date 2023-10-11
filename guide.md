# Guide for using the test tool

## Adding directories
Directories can be added for files to be picked up.

A directory must follow this structure:
```
├── base directory
│   ├── error
│   ├── out
│   ├── success
│   ├── certificate.config
```

The `certificate.config` must contain a single line with the value of the certificate prefix used for this directory.

The certificate prefix is used to retrieve the certificate credentials from environment variables.

## Certificate
The OCES3 certificate must be located on the computer running the tool.

Take note of the location of the certificate - the absolute path of the file must be used as the value of the `<certificate prefix>.file` environment variable.

## Environment variables
The following environment variables must be set:
- `<certificate prefix>.file`
- `<certificate prefix>.password`
- `<certificate prefix>.type`
- `<certificate prefix>.alias`
- `<certificate prefix>.privatePassword`

## The application.properties file
When running the jar, a file named `application.properties` must be placed in the same directory of the jar.

This file contains the configuration of the program:
```properties
directoryPaths=C:\\Path\\To\\Directory1;C:\\Path\\To\\Directory2
```

- `directoryPaths`: list of directories where files are picked up from. Separated by semicolons.

## Files to be picked up
Files that should be sent to DMS must follow a naming convention to be picked up by the tool.

```
<ProcedureType>_<DmsService>_<any string>.xml
```

- `ProcedureType`: the type of the declaration, e.g. _B1_, _H7_, etc.
- `DmsService`: values can be either _dms.export2_ or _dms.import2_
- `any string`: the last part of the filename can contain information such as a timestamp, etc.

`ProcedureType`, `DmsService` and the arbitrary string must be separated by underscores (`_`).

Files must have the `.xml` extension.