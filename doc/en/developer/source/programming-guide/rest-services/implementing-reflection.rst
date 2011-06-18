.. _rest_services_implementing_reflection:

Implementing a RESTful Service with Reflection
==============================================

The previous section showed how to save some effort when implementing a 
RESTful service by taking advantage of the ``MapResource`` base class. This section will show how to make use of a different base class, but for a 
similar purpose.

The class used is ``org.geoserver.rest.ReflectiveResource``. The idea with
``ReflectiveResource`` is that a resource is backed by an arbitrary object.
The ``ReflectiveResource`` class uses reflection to automatically create 
representations of the resource as XML or JSON.

Prerequisites
--------------

This section builds off the example in the previous section
:ref:`rest_services_implementing_map`. 

Create a new java bean
----------------------

#. The use of ``ReflectiveResource`` requires we have an underlying object to 
   to work with. Create a class named ``Hello`` in the package
   ``org.geoserver.hellorest``:

   .. code-block:: java

      package org.geoserver.hellorest;

      public class Hello {

         String message;

         public Hello( String message ) {
             this.message = message;
         }

         public String getMessage() {
            return message;
         }
      }

Create a new resource class
---------------------------

#. Create a new class called ``HelloReflectiveResource`` in the package 
   ``org.geoserver.hellorest``, which extends from ``ReflectiveResource``:

   .. code-block:: java

      package org.geoserver.hellorest;

      import org.geoserver.rest.ReflectiveResource;

      public class HelloReflectiveResource extends ReflectiveResource {

         @Override
         protected Object handleObjectGet() throws Exception {
             return null;
         }
      }

#. The first method to implement is ``handleObjectGet()``. The purpose of this
   method is to return the underlying object representing the resource. In 
   this case, an instance of the ``Hello`` class created in the previous step.

   .. code-block:: java

       @Override
       protected Object handleObjectGet() throws Exception {
          return new Hello( "Hello World" );
       }
	
Update the application context
------------------------------

#. The next step is to update an application context and tell GeoServer
   about the new resource created in the previous section. Update the 
   ``applicationContext.xml`` file so that it looks like the following:

   .. code-block:: xml

      <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN" "http://www.springframework.org/dtd/spring-beans.dtd">

      <beans>
         <bean id="hello" class="org.geoserver.hellorest.HelloResource"/>
         <bean id="helloMap" class="org.geoserver.hellorest.HelloMapResource"/>
         <bean id="helloReflective" class="org.geoserver.hellorest.HelloReflectiveResource"/>

         <bean id="helloMapping" class="org.geoserver.rest.RESTMapping">
            <property name="routes">
               <map>
                 <entry>
                   <key><value>/hello.{format}</value></key>
                   <!--value>hello</value-->
                   <!--value>helloMap</value-->
                   <value>helloReflective</value>
                 </entry>
              </map>
           </property>
         </bean>

     </beans>

   There are two things to note above. The first is the addition of the 
   ``helloReflective`` bean. The second is a change to the ``helloMapping``
   bean, which now maps to the ``helloReflective`` bean.
 
Test
----

#. Create a new test class called ``HelloReflectiveResourceTest`` in the
   package ``org.geoserver.hellorest``, which extends from
   ``org.geoserver.test.GeoServerTestSupport``:

   .. code-block:: java

       package org.geoserver.hellorest;

       import org.geoserver.test.GeoServerTestSupport;

       public class HelloReflectiveResourceTest extends GeoServerTestSupport {

       }

#. Add a test named ``testGetAsXML()`` which makes a GET request for
   ``/rest/hello.xml``:
   

   .. code-block:: java

      ...

      import org.w3c.dom.Document;
      import org.w3c.dom.Node;

      ...

         public void testGetAsXML() throws Exception {
           //make the request, parsing the result as a dom
           Document dom = getAsDOM( "/rest/hello.xml" );

           //print out the result
           print(dom);

           //make assertions
           Node message = getFirstElementByTagName( dom, "message");
           assertNotNull(message);
           assertEquals( "Hello World", message.getFirstChild().getNodeValue() );
         }

#. Add a second test named ``testGetAsJSON()`` which makes a GET request
   for ``/rest/hello.json``:

   .. code-block:: java

      ...

      import net.sf.json.JSON;
      import net.sf.json.JSONObject;

      ...

         public void testGetAsJSON() throws Exception {
           //make the request, parsing the result into a json object
           JSON json = getAsJSON( "/rest/hello.json");

           //print out the result
           print(json);

           //make assertions
           assertTrue( json instanceof JSONObject );
           JSONObject hello = ((JSONObject) json).getJSONObject( "org.geoserver.hellorest.Hello" );
           assertEquals( "Hello World", hello.get( "message" ) );
         }

#. Build and test the ``hello_test`` module::

     [hello_rest]% mvn clean install -Dtest=HelloMapReflectiveResourceTest
