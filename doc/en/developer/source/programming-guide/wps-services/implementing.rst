.. _wps_services_implementing:

Implementing a WPS Service
==========================

This section describes how to implement a WPS service in GeoServer, using
a "Hello World" example. The service is designed to be extremely simple, 
taking a single parameter and returning text. It is meant to show the 
developer how to put together all the pieces necessary to build a WPS
service on top of GeoServer

Prerequisites
-------------

Before being able to proceed, GeoServer must be built on the local system. See
the :ref:`source` and :ref:`quickstart` sections for details. In addition to these,
GeoServer must be built with WPS support as described in the 
:ref:`maven_guide` section, make sure it is built with the ``-Pwps`` profile

You can also create a custom WPS module and deploy it into an existing GeoServer
with the WPS extension already deployed into it. 

Create a new module
-------------------
In order to create a new WPS plug-in the first step is to create a custom Maven project.
The project will be called "hello_wps".

#. Create a new directory named ``hello_wps`` somewhere on the file system.

#. Add the following ``pom.xml`` to the root of the new module, inside the hello_wps directory:

 .. code-block:: xml

    <project xmlns="http://maven.apache.org/POM/4.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
	http://maven.apache.org/maven-v4_0_0.xsd ">
       <modelVersion>4.0.0</modelVersion>

       <groupId>org.geoserver</groupId>  
       <artifactId>hello_wps</artifactId>
       <packaging>jar</packaging>
       <version>2.2-SNAPSHOT</version>
       <name>hello_wps</name>
       <dependencies>
	<dependency>
	  <groupId>org.geotools</groupId>
     	  <artifactId>gt-process</artifactId>
     	  <version>8-SNAPSHOT</version>
   	</dependency>
	<dependency>
           <groupId>org.geoserver</groupId>
           <artifactId>main</artifactId>
           <version>2.2-SNAPSHOT</version>
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
               <source>1.5</source>
               <target>1.5</target>
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

   The project that was just created should have the following structure::

     hello_wps/
      + pom.xml
       + src/	
	 + main/
	   + java/ 



Create the process class
------------------------

#. Create the package that will contain your custom WPS process

   Package naming plays an important role in creating a WPS process. The rightmost
   part of the package name will be the namespace for the WPS process being created.
   Create a package named ``org.geoserver.wps.gs`` inside the *src/main/java* directory
   structure. The namespace for the new WPS process will be ``gs``.


#. Create the Java class that will expose your custom WPS process

   Create a Java class called HelloWPS.java inside the previous package:

  .. code-block:: java
 
 
     import org.geotools.process.factory.DescribeParameter;
     import org.geotools.process.factory.DescribeProcess;
     import org.geotools.process.factory.DescribeResult;
     import org.geotools.process.gs.GSProcess;	
     	
     @DescribeProcess(title="helloWPS", description="Hello WPS Sample")
     public class HelloWPS implements GSProcess {
  
	@DescribeResult(name="result", description="output result")
	public String execute(@DescribeParameter(name="name", description="name to return") String name) {
	  return "Hello, " + name;
	}
  
     }


Register process with GeoServer
-------------------------------

GeoServer uses the `Spring Framework <http://www.springsource.org/spring-framework/>`_ to manage instantiation of its different components. We are going to 
use the same mechanism to make this service discoverable by telling GeoServer to include its functionality
when it starts. 

#. Create the directory ``src/main/resources`` under the root of the new module::

   [hello_wps]% mkdir -p src/main/resources

   The project should have the following directory structure::

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
          <bean id="helloWPS" class="org.geoserver.wps.gs.HelloWPS"/>
        </beans>


Build, deploy and test
----------------------

In order to build your custom process, run the following command from the root of your project:

  .. code-block:: console
 
     mvn clean install

This will clean previous runs, compile your code, and create a JAR file in the target directory.
The JAR file name is controlled by the name and version given to the project upon creation
(hello_wps-2.2-SNAPSHOT.jar in this example).


To deploy your module, copy this JAR file inside the ``/WEB-INF/lib`` directory of GeoServer and then restart it.

  .. note:: For alternative deployment options (i.e. running from source), see the *Trying it out* 
     	    section inside :ref:`ows_services_implementing`


Once GeoServer is running again, you can verify that the new process was deployed successfully by running
the WPS Request Builder. The WPS Request Builder is a utility that can run tests of existing WPS processes
through the UI. You can access this utility by navigating to the WPS Request Builder inside the Demos
section of the GeoServer Web Admin Interface.


Once in the WPS request builder, select the process called gs:helloWPS from the **Choose process** dropdown.
The request builder will generate the necessary interface to be able to test the process, based on the
parameters and expected outputs described in the capabilities of the process. 

The following image show an example of the WPS Request Builder running the helloWPS process, enter the 
desired parameter and click on **Execute process** to run it. A window with the expected result should appear.

  .. figure:: img/helloWPS.png

     *WPS Request Builder*


