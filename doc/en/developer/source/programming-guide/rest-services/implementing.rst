.. _rest_services_implementing:

Implementing a RESTful Service
==============================

This section describes how to implement a restful service in GeoServer, using
a "Hello World" example. The service will be extremely simple and will return 
the text "Hello World" from a GET request.

Prerequisites
--------------

Before being able to proceed, GeoServer must be built on the local system. See
the :ref:`source` and :ref:`quickstart` sections for details.

Create a new module
-------------------

#. Create a new module named ``hello_rest`` somewhere on the file system.

#. Add the following ``pom.xml`` to the root of the new module:

  .. code-block:: xml

     <project xmlns="http://maven.apache.org/POM/4.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd ">
       <modelVersion>4.0.0</modelVersion>

       <groupId>org.geoserver</groupId>
       <artifactId>hello_rest</artifactId>
       <packaging>jar</packaging>
       <version>1.0-SNAPSHOT</version>
       <name>hello_rest</name>

       <dependencies>
         <dependency>
           <groupId>org.geoserver</groupId>
           <artifactId>gs-rest</artifactId>
           <version>2.8-SNAPSHOT</version> <!-- change this to the proper GeoServer version -->
         </dependency>
         <dependency>
           <groupId>org.geoserver</groupId>
           <artifactId>gs-main</artifactId>
           <version>2.8-SNAPSHOT</version> <!-- change this to the proper GeoServer version -->
           <classifier>tests</classifier>
           <scope>test</scope>
         </dependency>
         <dependency>
           <groupId>junit</groupId>
           <artifactId>junit</artifactId>
           <version>4.11</version>
           <scope>test</scope>
         </dependency>
         <dependency>
           <groupId>com.mockrunner</groupId>
           <artifactId>mockrunner</artifactId>
           <version>0.3.6</version>
          <scope>test</scope>
         </dependency>

       </dependencies>

       <build>
         <plugins>
           <plugin>
             <artifactId>maven-compiler-plugin</artifactId>
             <configuration>
               <source>1.8</source>
               <target>1.8</target>
             </configuration>
          </plugin>
        </plugins>
       </build>

     </project>

#. Create the directory ``src/main/java`` under the root of the new module::

     [hello_rest]% mkdir -p src/main/java

Create the resource class
-------------------------

#. The class ``org.geoserver.rest.AbstractResource`` is a convenient base
   class available when creating new resources. Create a new class called 
   ``HelloResource`` in the package ``org.geoserver.hellorest``, which 
   extends from ``AbstractResource``.

   .. code-block:: java

      package org.geoserver.hellorest;

      import java.util.List;
      import org.geoserver.rest.AbstractResource;
      import org.geoserver.rest.format.DataFormat;
      import org.restlet.data.Request;
      import org.restlet.data.Response;

      public class HelloResource extends AbstractResource {
         @Override
         protected List<DataFormat> createSupportedFormats(Request request, Response response) {

            return null;
         }
      }

#. The first method to implement is ``createSupportedFormats()``. The purpose
   of this method is to create mapping from an extension, to a particular
   format. For now the goal will be to return the text "Hello World" when a 
   ".txt" extension is requested by the client.

   .. code-block:: java

      import java.util.ArrayList;
      import org.geoserver.rest.format.StringFormat;
      ...

      @Override
      protected List<DataFormat> createSupportedFormats(Request request, Response response) {

         List<DataFormat> formats = new ArrayList();
         formats.add(new StringFormat( MediaType.TEXT_PLAIN ));

         return formats;
      }
	
#. The next step is to override the ``handleGet()`` method. This method is 
   called when a GET request is made for the resource.

   .. code-block:: java

      @Override
      public void handleGet() {
         //get the appropriate format
         DataFormat format = getFormatGet();

         //transform the string "Hello World" to the appropriate response
         getResponse().setEntity(format.toRepresentation("Hello World"));
      }

   The above makes use of the ``getFormatGet()`` method, whose purpose is 
   to determine the extension being requested by the client, and look up 
   the appropriate format for it. In this case when the client requests the
   ".txt" extension, the ``StringFormat`` setup in the previous step will be
   found.

Create the application context
------------------------------

#. The next step is to create an application context that tells GeoServer 
   about the resource created in the previous section. Create the directory
   ``src/main/resources`` under the root of the ``hello_rest`` module::

     [hello_rest]% mkdir src/main/resources


#. Add the following ``applicationContext.xml`` file to the ``src/main/resources`` directory under the root of the ``hello_rest`` module.

   .. code-block:: xml

      <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN" "http://www.springframework.org/dtd/spring-beans.dtd">

      <beans>
         <bean id="hello" class="org.geoserver.hellorest.HelloResource"/>

         <bean id="helloMapping" class="org.geoserver.rest.RESTMapping">
            <property name="routes">
               <map>
                 <entry>
                   <key><value>/hello.{format}</value></key>
                   <value>hello</value>
                 </entry>
              </map>
           </property>
         </bean>

     </beans>

   There are two things to note above. The first is the ``hello`` bean which
   is an instance of the ``HelloResource`` class created in the previous
   section. The second is the ``helloMapping`` bean, which defines a template
   for the uri in which the resource will be accessed. The above mapping 
   specifies that the resource will be located at ``/rest/hello.{format}``
   where ``format`` is the representation being requested by the client. As 
   implemented ``hello.txt`` is the only supported representation.

Test
----

#. Create the directory ``/src/test/java`` under the root of the 
   ``hello_rest`` module::

       [hello_rest]% mkdir -p src/test/java

#. Create a new test class called ``HelloResourceTest`` in the package
   ``org.geoserver.hellorest``, which extends from
   ``org.geoserver.test.GeoServerTestSupport``:

   .. code-block:: java

       package org.geoserver.hellorest;

       import org.geoserver.test.GeoServerTestSupport;

       public class HelloResourceTest extends GeoServerTestSupport {

          public void test() throws Exception {

          }
       }

#. Add a statement which makes a GET request for ``/rest/hello.txt`` and 
   asserts it is equal to the string "Hello World":

   .. code-block:: java

      public void test() throws Exception {
         assertEquals( "Hello World", getAsString("/rest/hello.txt"));
      }

#. Build and test the ``hello_test`` module::

     [hello_rest]% mvn install
