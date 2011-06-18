.. _rest_services_implementing_map:

Implementing a RESTful Service with Maps
========================================

The previous section showed how to implement a very simple restful service. 
This section will show how to make use of some existing base classes in order
to save time when supporting additional formats for representing resources.

The class used is ``org.geoserver.rest.MapResource``. The idea with
``MapResource`` is that a resource is backed by a data structure contained
by a ``java.util.Map``. With this map, the ``MapResource`` class can 
automatically create representations of the resource in either XML or JSON.

Prerequisites
--------------

This section builds off the example in the previous section
:ref:`rest_services_implementing`. 

Create a new resource class
---------------------------

#. Create a new class called ``HelloMapResource`` in the package 
   ``org.geoserver.hellorest``, which extends from ``MapResource``:

   .. code-block:: java

      package org.geoserver.hellorest;

      import java.util.Map;
      import org.geoserver.rest.MapResource;

      public class HelloMapResource extends MapResource {
         @Override
         public Map getMap() throws Exception {

            return null;
         }
      }

#. The first method to implement is ``getMap()``. The purpose of this method
   is to create a map based data structure which represents the resource. For
   now a simple map will be created with a single key "message", containing 
   the "Hello World" string:

   .. code-block:: java

      @Override
      public Map getMap() throws Exception {
         HashMap map = new HashMap();
         map.put( "message", "Hello World");

         return map;
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

         <bean id="helloMapping" class="org.geoserver.rest.RESTMapping">
            <property name="routes">
               <map>
                 <entry>
                   <key><value>/hello.{format}</value></key>
                   <!--value>hello</value-->
                   <value>helloMap</value>
                 </entry>
              </map>
           </property>
         </bean>

     </beans>

   There are two things to note above. The first is the addition of the 
   ``helloMap`` bean. The second is a change to the ``helloMapping`` bean,
   which now maps to the ``helloMap`` bean, rather than the ``hello`` bean.
 
Test
----

#. Create a new test class called ``HelloMapResourceTest`` in the package
   ``org.geoserver.hellorest``, which extends from
   ``org.geoserver.test.GeoServerTestSupport``:

   .. code-block:: java

       package org.geoserver.hellorest;

       import org.geoserver.test.GeoServerTestSupport;

       public class HelloMapResourceTest extends GeoServerTestSupport {

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
           assertEquals( "Hello World", ((JSONObject)json).get( "message" ) );
         }

#. Build and test the ``hello_test`` module::

     [hello_rest]% mvn clean install -Dtest=HelloMapResourceTest
