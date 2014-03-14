.. _wps_services_implementing:

Implementing a WPS Process
==========================

This section describes how to implement a WPS process for use in GeoServer. 
It demonstrates the development artifacts and build steps 
necessary to create a WPS process, deploy it in GeoServer,
and test it.

The example process used is a simple "Hello World" process 
which accepts a single input parameter and returns a single text output.

Prerequisites
-------------

Before starting, GeoServer must be built on the local system. See
the :ref:`source` and :ref:`quickstart` sections for details.
GeoServer must be built with WPS support as described in the 
:ref:`maven_guide` section. 
Specifically, make sure GeoServer is built using the ``-Pwps`` profile.

Alternatively, the custom WPS plug-in can be deployed into an existing GeoServer
instance (which must have the WPS extension installed). 

Create the process module
-------------------------

To create a new WPS process plug-in module the first step is to create a Maven project.
For this example the project will be called "hello_wps".

#. Create a new directory named ``hello_wps`` somewhere on the file system.

#. Add the following ``pom.xml`` to the root of the new module in the ``hello_wps`` directory:

.. code-block:: xml

    <project xmlns="http://maven.apache.org/POM/4.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
	http://maven.apache.org/maven-v4_0_0.xsd ">
       <modelVersion>4.0.0</modelVersion>

       <groupId>org.geoserver</groupId>  
       <artifactId>hello_wps</artifactId>
       <packaging>jar</packaging>
       <version>2.4-SNAPSHOT</version>
       <name>hello_wps</name>
       <dependencies>
         <dependency>
           <groupId>org.geotools</groupId>
           <artifactId>gt-process</artifactId>
           <version>10-SNAPSHOT</version>
         </dependency>
         <dependency>
           <groupId>org.geoserver</groupId>
           <artifactId>main</artifactId>
           <version>2.4-SNAPSHOT</version>
           <classifier>tests</classifier>
           <scope>test</scope>
         </dependency>
         <dependency>
           <groupId>junit</groupId>
           <artifactId>junit</artifactId>
           <version>3.8.1</version>
           <scope>test</scope>
         </dependency>
         <dependency>
           <groupId>com.mockrunner</groupId>
           <artifactId>mockrunner</artifactId>
           <version>0.3.1</version>
          <scope>test</scope>
         </dependency>
       </dependencies>

       <build>
         <plugins>
           <plugin>
             <artifactId>maven-compiler-plugin</artifactId>
             <configuration>
               <source>1.6</source>
               <target>1.6</target>
             </configuration>
          </plugin>
        </plugins>
       </build>

       <repositories>
         <repository>
           <id>opengeo</id>
       	   <name>opengeo</name>
       	   <url>http://repo.opengeo.org</url>
        </repository>
       </repositories>

    </project>  

#. Create the directory ``src/main/java`` under the root of the new module::

   [hello_wps]% mkdir -p src/main/java

   The project should now have the following structure::

     hello_wps/
      + pom.xml
       + src/	
         + main/
           + java/ 


Create the process class
------------------------

#. Create the package that will contain the custom WPS process.

   For this example, create a package named ``org.geoserver.hello.wps`` inside the 
   *src/main/java* directory structure.

   [hello_wps]% mkdir -p src/main/java/org/geoserver/hello/wps

#. Create the Java class that implements the custom WPS process.

   Create a Java class called ``HelloWPS.java`` inside the created package:

  .. code-block:: java
 
     package org.geoserver.hello.wps;
 
     import org.geotools.process.factory.DescribeParameter;
     import org.geotools.process.factory.DescribeProcess;
     import org.geotools.process.factory.DescribeResult;
     import org.geoserver.wps.gs.GeoServerProcess;
     	
     @DescribeProcess(title="helloWPS", description="Hello WPS Sample")
     public class HelloWPS implements GeoServerProcess {
  
        @DescribeResult(name="result", description="output result")
        public String execute(@DescribeParameter(name="name", description="name to return") String name) {
             return "Hello, " + name;
        }
     }


Register the process in GeoServer
---------------------------------

GeoServer uses the `Spring Framework <http://www.springsource.org/spring-framework/>`_ to manage 
instantiation of components. This mechanism is used to register the process with GeoServer when it 
starts, which will make it discoverable via the WPS service interface. 

#. Create a directory ``src/main/resources`` under the root of the new module::

   [hello_wps]% mkdir -p src/main/resources

   The project should now have the following directory structure::

     hello_wps/
      + pom.xml
       + src/	
	 + main/
	   + java/ 
	   + resources/



#. Create an ``applicationContext.xml`` in the ``src/main/resources`` directory with the following contents:

    .. code-block:: xml

      <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN" "http://www.springframework.org/dtd/spring-beans.dtd">
        <beans>
          <bean id="helloWPS" class="org.geoserver.hello.wps.HelloWPS"/>
        </beans>

.. note:: A process registered in the GeoServer spring context will be assigned to the "gs" 
          process namespace.

Build and Deploy
----------------

To build the custom process, run the following command from the root of the project:

  .. code-block:: console
 
     mvn clean install

This cleans the build area, compiles the code, and creates a JAR file in the ``target`` directory.
The JAR file name is determined by the name and version given to the project in the ``pom.xml`` file.
(for this example it is ``hello_wps-2.6-SNAPSHOT.jar``).


To deploy the process module, copy this JAR file into the ``/WEB-INF/lib`` directory of GeoServer and then restart the instance.

.. note:: 
   
   For alternative deployment options (i.e. running from source), see the *Trying it out* 
   section inside :ref:`ows_services_implementing`


Test
----

You can verify that the new process was deployed successfully by using
the **WPS Request Builder**. The WPS Request Builder is a utility that allows invoking WPS processes
through the GeoServer UI. Access this utility by navigating to the *WPS Request Builder* in the *Demos*
section of the GeoServer Web Admin Interface.

In the WPS Request Builder select the process called ``gs:helloWPS`` from the **Choose process** dropdown.
The request builder displays an interface which allows calling the process, based on the
parameters and outputs described in the capabilities of the process
(which are defined by the process class annotations). 

The following image shows the WPS Request Builder running the ``gs:helloWPS`` process.
Enter the desired parameter and click on **Execute process** to run it. A window with the expected result should appear.

  .. figure:: img/helloWPS.png

     *WPS Request Builder, showing gs:HelloWPS process parameters*


