# Usage of the Automate Test.
With the `Makefile` you can automate the tests with just a command.

### commands:
```shell
make # will show you the help and the commands. 
```
* To run clean the ENV. ex:
```shell
make clean suite=<suite test>
```
* To build the geoserver image.
```shell
make build suite=<suite test>
```
* To test the suite.
```shell
make test suite=<suite test>
```
* To run all the steps from the beginning.
```shell
make all suite=<suite test>
```