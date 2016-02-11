Simple Ping App With Concurrent Host List Treatment
=======================================================

Functionality
    Given some hosts in the configured *HostsFile* :
        google.com
        duckduckgo.com




How to Run
----------------------------------
This project has been developed using maven. So in order to compile run for example *mvn clean install* ; also you can use *-DskipTest* to avoid running the tests.

The jar can be run as :
> java -jar .\SimplePing\target\SimplePing-1.0.jar -n 5 -hf .\HostLst.txt -d 10000

or run simply the jar to know the parameters:
> java -jar .\SimplePing\target\SimplePing-1.0.jar

TODO: Put the execute command into a bash file (*.bat* or *.sh*) to run it dynamically or to set it as a base command. For example:
> java -jar jobs/SimplePing-1.0.jar $1

.