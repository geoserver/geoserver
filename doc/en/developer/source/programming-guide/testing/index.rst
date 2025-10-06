.. _testing:

Testing
=======

This section provides an overview of how testing in GeoServer works and a brief guide for 
developers to help with the process of writing tests.


Libraries
---------

GeoServer utilizes a number of commonly used testing libraries. 

JUnit
^^^^^

The well known `JUnit <http://junit.org>`_ framework is the primary test library used in 
GeoServer. The current version used is Junit 4.x. While it is possible to continue to write 
JUnit 3.x style tests with JUnit 4, new tests should be written in the JUnit4 style 
with annotations.

Current version: 4.11

XMLUnit
^^^^^^^

The `XMLUnit <http://xmlunit.sourceforge.net>`_ library provides a convenient way to make 
test assertions about the structure of XML documents. Since many components and services in 
GeoServer output XML, XMLUnit is a very useful library.

Current version: 1.3


MockRunner
^^^^^^^^^^

The `MockRunner <http://mockrunner.sourceforge.net>`_ framework provides a set of classes that
implement the various interfaces of the J2EE and Java Servlet apis. It is typically used to 
create ``HttpServletRequest`` , ``HttpServletResponse``, etc... objects for testing servlet 
based components. 

Current version: 0.3.6

EasyMock
^^^^^^^^

The `EasyMock <http://www.easymock.org>`_ library is a 
`mocking framework <http://en.wikipedia.org/wiki/Mock_object>`_ that is used to simulate 
various objects without actually creating a real version of them. This is an extremely useful 
tool when developing unit tests for a component A, that requires component B when component
B may not be so easy to create from scratch. 

Current version: 2.5.2

Testing Categories and Terminology
-----------------------------------

Software testing falls into many different categories and the GeoServer code base is no 
exception. In the GeoServer code base one may find different types of tests.

Unit 
^^^^

Tests that exercise a particular method/class/component in complete isolation. In GeoServer
these are tests that typically don't extend from any base class and look like what one would
typically expect a unit test to look like.


Integration/Mock
^^^^^^^^^^^^^^^^

Tests that exercise a component by that must integrate with another component to operate.  
In GeoServer these are tests that somehow mock up the dependencies for the component under
test either by creating it directly or via a mocking library.

System
^^^^^^

Tests that exercise a component or set of components by testing it in a fully running system.
In GeoServer these are tests that create a fully functional GeoServer system, including
a data directory/configuration and a spring context.

Helper classes are provided to help inject your classes into the system configuration including ``GeoServerExtensionsHelper``.

Writing System Tests
--------------------

System tests are the most common type of test case in GeoServer, primarily because they are 
the easiest tests to write. However they come with a cost of performance. The GeoServer system
test framework provides a fully functional GeoServer system. Creating this system is an 
expensive operation so a full system test should be used only as a last resort. 
Developers are encouraged to consider a straight unit or mock tests before resorting to a 
full system test.

In GeoServer system tests extend from the ``org.geoserver.test.GeoServerSystemTestSupport`` class.
The general lifecycle of a system test goes through the following states:

#. System initialization
#. System creation
#. Test execution
#. System destruction

Phases 1 and 2 are referred to as the setup phase. It is during this phase that two main
operations are performed. The first is the creation of the GeoServer data directory on 
disk. The second is the creation of the spring application context.

Single vs Repeated Setup
^^^^^^^^^^^^^^^^^^^^^^^^

By default, for performance reasons, the setup phase is executed only once for a system
test. This can however be configured by annotating the test class with a special annotation 
named ``TestSetup``. For example, to specify that the setup should be executed many times, 
for each test method of the class:

.. code-block:: java

  @TestSetup(run=TestSetupFrequency.REPEAT)
  public class MyTestCase extends GeoServerSystemTestSupport {
     ...
  }

This however should be used only as a last resort since as mentioned before a repeated 
setup makes the test execute very slowly. An alternative to a repeated setup is to have the
test case revert any changes that it makes during its execution, so that every test method
can execute in a consistent state. The ``GeoServerSystemTestSupport`` contains a number of 
convenience methods for doing this. Consider the following test:

.. code-block:: java

  public class MyTestCase extends GeoServerSystemTestSupport {
     
     @Before
     public void revertChanges() {
         //roll back any changes made
         revertLayer("foo");
     }

     @Test
     public void testThatChangesLayerFoo() {
        //change layer foo in some way
     }
  }

The test makes some changes to a particular layer but uses a before hook to revert any 
such changes. In general this is the recommended pattern for system tests that must are not
read-only and must modify configuration or data to execute.

Method Level SetUp
^^^^^^^^^^^^^^^^^^

A third method of controlling test setup frequency is available at the test case level. 
Annotating a test method with the ``RunTestSetup`` annotation will cause the test setup to be
run before the test method is executed. For example:

.. code-block:: java

  public class MyTestCase extends GeoServerSystemTestSupport {
     
     @Before
     public void revertChanges() {
         //roll back any changes made
         revertLayer("foo");
     }

     @Test
     public void test1() {
     }

     @Test
     public void test2() {
     }

     @Test
     @RunTestSetup
     public void test3() {
     
     }

     @Test
     public void test4() {
     }

  }

In the above method the test setup will be run twice. Once before the entire test class is
run, and again before the test3 method is executed.

Setup/Teardown Hooks
^^^^^^^^^^^^^^^^^^^^

There are a number of ways to hook into test lifecycle to provide setup and tear down 
functionality. 

JUnit @Before, @After, @BeforeClass, @AfterClass
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As with any JUnit test various annotations are available to perform tasks at various points
of the test life cycle. However with a GeoServer system test one must be wary of the task having 
a dependency on the system state. For this reason the ``GeoServerSystemTestSupport`` class 
provides its own callbacks. 

setUpTestData
~~~~~~~~~~~~~

This callback method is invoked before the system has been created. It is meant to provide the
test with a way to configure what configuration gets created in the GeoServer data directory 
for the test. By default the test setup will create a standard set of vector layers. This 
method is where that should be changed, for instance to indicate that the test requires that
raster layers be created as well. For example:

.. code-block:: java

  public class MySystemTest extends GeoServerSystemTestBase {
    
     protected void setUpTestData(SystemTestData testData) {
        // do the default by calling super
        super.setUpTestData(testData);

        // add raster layers
        testData.setUpDefaultRasterLayers();
     }
  }

Depending on whether the test uses a single or repeated setup this method will be called once
or many times.

onSetUp
~~~~~~~

This callback method is invoked after the system has been created. It is meant for standard 
post system initialization tasks. Like for instance changing some service configuration, 
adding new layers, etc... 

Depending on whether the test uses a single or repeated setup this method will be called once
or many times. For this reason this method can not be used to simply initialize fields of the
test class. For instance, consider the following:

.. code-block:: java

  public class MySystemTest extends GeoServerSystemTestBase {
    
      Catalog catalog;

      @Override
      protected void onTestSetup(SystemTestData testData) throws Exception {
         // add a layer named foo to the catalog
         Catalog catalog = getCatalog();
         catalog.addLayer(new Layer("foo"));

         // initialize the catalog field
         this.catalog = catalog;
      }

      @Test
      public void test1() {
         catalog.getLayerByName("foo");
      }

      @Test
      public void test2() {
         catalog.getLayerByName("foo");
      }
  }

Since this is a one time setup, the onSetUp method is only executed once, before the test1 
method. When the test2 method is executed it is actually a new instance of the test class, 
but the onTestSetup is not re-executed. The proper way to this initialization would be:

.. code-block:: java

  public class MySystemTest extends GeoServerSystemTestBase {
    
      Catalog catalog;

      @Override
      protected void onTestSetup(SystemTestData testData) throws Exception {
         // add a layer named foo to the catalog
         Catalog catalog = getCatalog();
         catalog.addLayer(new Layer("foo"));

         // initialize the catalog field
         this.catalog = catalog;
      }

      @Before
      public void initCatalog() {
          this.catalog = getCatalog();
      }
  }
  
System Test Data
^^^^^^^^^^^^^^^^

The GeoServer system test will create a data directory with a standard set of 
vector layers. The contents of this data directory are as follows:

Workspaces
~~~~~~~~~~

.. list-table::
   :widths: 1 3 1 1
   :header-rows: 1

   * - Workspace
     - URI
     - Layer Count
     - Default?
   * - cdf
     - http://www.opengis.net/cite/data
     - 8
     -
   * - cgf
     - http://www.opengis.net/cite/geometry
     - 6
     -
   * - cite
     - http://www.opengis.net/cite
     - 12
     -
   * - gs
     - http://geoserver.org
     - 0
     - Yes
   * - sf
     - http://cite.opengeospatial.org/gmlsf
     - 3
     -

Stores and Layers
~~~~~~~~~~~~~~~~~

.. list-table::
   :widths: 2 2 3 3
   :header-rows: 1

   * - Workspace
     - Store
     - Layer Name
     - Default Style
   * - cdf
     - cdf
     - Deletes
     - Default
   * - cdf
     - cdf
     - Fifteen
     - Default
   * - cdf
     - cdf
     - Inserts
     - Default
   * - cdf
     - cdf
     - Locks
     - Default
   * - cdf
     - cdf
     - Nulls
     - Default
   * - cdf
     - cdf
     - Other
     - Default
   * - cdf
     - cdf
     - Seven
     - Default
   * - cdf
     - cdf
     - Updates
     - Default
   * - cgf
     - cgf
     - Lines
     - Default
   * - cgf
     - cgf
     - MLines
     - Default
   * - cgf
     - cgf
     - MPoints
     - Default
   * - cgf
     - cgf
     - MPolygons
     - Default
   * - cgf
     - cgf
     - Points
     - Default
   * - cgf
     - cgf
     - Polygons
     - Default
   * - cite
     - cite
     - BasicPolygons
     - BasicPolygons
   * - cite
     - cite
     - Bridges
     - Bridges
   * - cite
     - cite
     - Buildings
     - Buildings
   * - cite
     - cite
     - DividedRoutes
     - DividedRoutes
   * - cite
     - cite
     - Forests
     - Forests
   * - cite
     - cite
     - Geometryless
     - Default
   * - cite
     - cite
     - Lakes
     - Lakes
   * - cite
     - cite
     - MapNeatline
     - MapNeatLine
   * - cite
     - cite
     - NamedPlaces
     - NamedPlaces
   * - cite
     - cite
     - Ponds
     - Ponds
   * - cite
     - cite
     - RoadSegments
     - RoadSegments
   * - cite
     - cite
     - Streams
     - Streams
   * - sf
     - sf
     - AgregateGeoFeature
     - Default
   * - sf
     - sf
     - GenericEntity
     - Default
   * - sf
     - sf
     - PrimitiveGeoFeature
     - Default

.. note::
   The ``gs`` workspace contains no layers. It is typically used as the 
   workspace for layers that are added by test cases.

Adding custom layers from a datastore
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you need to provide your test with a specific layer from a local datastore, for example to test handling a 
3D shapefile then you will need code like:

.. code-block:: java

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        DataStoreInfo storeInfo =
                createShapefileDataStore(getCatalog(), "tasmania_roads", "tasmania_roads.shp");

        createShapeLayer(getCatalog(), storeInfo);
    }

    private static DataStoreInfo createShapefileDataStore(
            Catalog catalog, String name, String file) {
        // get the file
        URL url = MultiDimensionTest.class.getResource(file);
        assertThat(url, notNullValue());
        // build the data store
        CatalogBuilder catalogBuilder = new CatalogBuilder(catalog);
        DataStoreInfo storeInfo = catalogBuilder.buildDataStore(name);
        storeInfo.setType("Shapefile");
        storeInfo.getConnectionParameters().put(ShapefileDataStoreFactory.URLP.key, url);
        catalog.add(storeInfo);
        storeInfo = catalog.getStoreByName(name, DataStoreInfo.class);
        assertThat(storeInfo, notNullValue());
        return storeInfo;
    }

    private static LayerInfo createShapeLayer(Catalog catalog, DataStoreInfo storeInfo)
            throws Exception {
        CatalogBuilder catalogBuilder = new CatalogBuilder(catalog);
        catalogBuilder.setStore(storeInfo);
        Name typeName = storeInfo.getDataStore(null).getNames().get(0);
        FeatureTypeInfo featureTypeInfo = catalogBuilder.buildFeatureType(typeName);
        catalog.add(featureTypeInfo);
        LayerInfo layerInfo = catalogBuilder.buildLayer(featureTypeInfo);
        catalog.add(layerInfo);
        layerInfo = catalog.getLayerByName(typeName.getLocalPart());
        assertThat(layerInfo, notNullValue());
        return layerInfo;
    }

Once the set up code has run you can request the layer as a WMS or WFS request using:

.. code-block:: java

  Document dom = getAsDOM("wfs?request=GetFeature&typenames=gs:tasmania_roads&version=2.0.0&service=wfs");


Other Support Classes
^^^^^^^^^^^^^^^^^^^^^

GeoServer also has several other test support classes the support specific parts of the project
such as ``GeoServerSecurityTestSupport`` and ``GeoServerWicketTestSupport.java``. These classes extend the
``GeoServerTestSupport`` class to provide additional set or methods related the thing they are testing.
For example, the wicket class (which is used to test GUI elements) provides a ``login`` and ``logout`` method,
and methods for interacting with web page components (``ComponentContentFinder``). 

If you are writing a new module you may want to consider creating a new support class for your module that
extends one of the extended support classes. In general it is worth looking for a test that does something
similar to the thing you want to test and using that as a template for your work.


+ GeoServerWicketTestSupport, requesting a simple panel, confirming output using component id

.. code:: java

  public class GraticuleStoreEditPanelTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed
    }

    @Before
    public void loginBefore() {
        super.login();
    }

    @Test
    public void testCreateModify() throws Exception {
        tester.startPage(new DataAccessNewPage(GraticuleDataStoreFactory.DISPLAY_NAME));
        tester.assertNoErrorMessage();

        tester.executeAjaxEvent(
                "dataStoreForm:parametersPanel:configsContainer:gratpanel:generateBoundsFromCRS", "click");
        FormTester ft = tester.newFormTester("dataStoreForm");
        ft.setValue("parametersPanel:configsContainer:gratpanel:steps:border:border_body:paramValue", "10");
        ft.setValue("dataStoreNamePanel:border:border_body:paramValue", "graticule10");
        ft.submit("save");

        tester.assertNoErrorMessage();

        // check the store has been created
        DataStoreInfo graticule10 = getCatalog().getDataStoreByName("graticule10");
        assertNotNull(graticule10);
        Map<String, Serializable> parameters = graticule10.getConnectionParameters();
        assertEquals("10", parameters.get(STEPS.key));
        ReferencedEnvelope world = new ReferencedEnvelope(-180, 180, -90, 90, CRS.decode("EPSG:4326", true));
        assertEquals(world, Converters.convert(parameters.get(BOUNDS.key), ReferencedEnvelope.class));

        // open again, and save (used to fail due to empty bounds forced during panel construction)
        tester.startPage(new DataAccessEditPage(graticule10.getId()));
        tester.assertNoErrorMessage();
        ft = tester.newFormTester("dataStoreForm");
        ft.submit("save");
        tester.assertNoErrorMessage();
    }
  }


+ GeoServerWicketTestSupport, requesting a page, clicking on a button, and confirming the output using component id

.. code:: java

   public class GeoServerAboutPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testLoginFormAction() throws Exception {
        logout();
        tester.executeUrl("./wicket/bookmarkable/org.geoserver.web.AboutGeoServerPage");
        assertThat(tester.getLastRenderedPage(), instanceOf(AboutGeoServerPage.class));

        String responseTxt = tester.getLastResponse().getDocument();
        TagTester tagTester = TagTester.createTagByName(responseTxt, "form");
        assertEquals("http://localhost/context/j_spring_security_check", tagTester.getAttribute("action"));
    }
    ...

+ WFSTestSupport, making a WFS request to a test data set:

  .. code:: java

      public class GetFeatureBboxTest extends WFSTestSupport {

      @Test
      public void testFeatureBoudingOn() throws Exception {
          WFSInfo wfs = getWFS();
          wfs.setFeatureBounding(true);
          getGeoServer().save(wfs);

          Document doc = getAsDOM("wfs?request=GetFeature&typeName="
                  + getLayerId(SystemTestData.BUILDINGS)
                  + "&version=1.0.0&service=wfs&propertyName=ADDRESS");
          // print(doc);

          // check it's a feature collection
          assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
          // check the collection has non null bounds
          assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection/gml:boundedBy/gml:Box)", doc);
          // check that each feature has non null bounds
          XpathEngine xpath = XMLUnit.newXpathEngine();
          assertTrue(xpath.getMatchingNodes("//cite:Buildings/gml:boundedBy/gml:Box", doc)
                          .getLength()
                  > 0);
      }

+ WMSTestSupport, adding a layer to test against, and checking output against reference image

  .. code:: java

    public class GetMapIntegrationTest extends WMSTestSupport {
      ...
      @Override
      protected void setUpTestData(SystemTestData testData) throws Exception {
          super.setUpTestData(testData);
          testData.setUpWcs11RasterLayers();
      }

      @Override
      protected void onSetUp(SystemTestData testData) throws Exception {
          super.onSetUp(testData);
          Catalog catalog = getCatalog();
          testData.addStyle("Population", "Population.sld", GetMapIntegrationTest.class, catalog);
          testData.addStyle("jiffleBandSelect", "jiffleBandSelect.sld", GetMapIntegrationTest.class, catalog);
          testData.addVectorLayer(
                  new QName(MockData.SF_URI, "states", MockData.SF_PREFIX),
                  Collections.emptyMap(),
                  "states.properties",
                  getClass(),
                  catalog);
            ...

      }
      ...
      @Test
      public void testGetMapOpaqueGroup() throws Exception {
          String url = "wms?LAYERS="
                  + OPAQUE_GROUP
                  + "&STYLES=&FORMAT=image%2Fpng"
                  + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=-0.0043,-0.0025,0.0043,0.0025";
          BufferedImage imageGroup = getAsImage(url, "image/png");

          ImageAssert.assertEquals(
                  new File("./src/test/resources/org/geoserver/wms/wms_1_1_1/opaqueGroup.png"), imageGroup, 300);
      }

   

Handling Logging
----------------

GeoServer has a lot of log handling built in but in a default system much of the logging is surpressed. The
following code shows how to initialise GeoServer's logging system. In general this should only be done
during testing of your code as excessive logging slows down the build for everyone.

.. code:: java

    GeoServerResourceLoader loader = getDataDirectory().getResourceLoader();
    LoggingUtils.initLogging(loader, "DEFAULT_LOGGING.xml", false, true, "logs/geoserver.log");
    String path = getDataDirectory().getResourceLoader().getBaseDirectory().getPath();
    LoggingInfo logging = getGeoServer().getLogging();
    final File logFile = new File(path, "logging.xml");
    logging.setLocation("logs/geoserver.log");
    logging.setStdOutLogging(true);
    logging.setLevel("VERBOSE_LOGGING.xml");
    getGeoServer().save(logging);

Writing Mock Tests
------------------

Mock tests, also referred to as integration tests, are a good way to test a component that
has dependencies on other components. It is often not simple to create the dependent component
with the correct configuration.

A mock test is just a regular unit test that uses functions from the EasyMock library to 
create mock objects. There is however a base class named ``GeoServerMockTestSupport`` that
is designed to provide a pre-created set of mock objects. These pre-created mock objects are 
designed to mimic the objects as they would be found in an actual running system. For example:

.. code-block:: java

    public class MyMockTest extends GeoServerMockTestSupport {
      
       @Test
       public void testFoo() {
          //get the mock catalog
          Catalog catalog = getCatalog();

          //create the object we actually want to test
          Foo foo = new Foo(catalog);
       }
    }

Like system tests, mock tests do a one-time setup with the same setUpTestData and onSetUp callbacks. 

The benefit of mock tests over system tests is the setup cost. Mock tests essentially have no 
setup cost which means they can execute very quickly, which helps to keep overall build times down.

EasyMock Class Extension
^^^^^^^^^^^^^^^^^^^^^^^^

By default EasyMock can only mock up interfaces. To mock up classes requires the EasyMock classextension jar and also the cglib library. These can be declared in a maven pom like so:

.. code-block:: xml

    <dependency>
      <groupId>org.easymock</groupId>
      <artifactId>easymockclassextension</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>cglib</groupId>
      <artifactId>cglib-nodep</artifactId>
      <scope>test</scope>
    </dependency>

The change is mostly transparent, however rather than importing ``org.easymock.EasyMock`` one
must import ``org.easymock.classextension.EasyMock``. 

Maven Dependencies
------------------

All of the GeoServer base test classes live in the gs-main module. However since they live in 
the test packages a special dependency must be set up in the pom of the module depending
on main. This looks like:

.. code-block:: xml

    <dependency>
      <groupId>org.geoserver</groupId>
      <artifactId>gs-main</artifactId>
      <version>${project.version}</version>
      <classifier>tests</classifier>
      <scope>test</scope>
    </dependency>

Furthermore, in maven test scope dependencies are not transitive in the same way that 
regular dependencies are. Therefore some additional dependencies must also be declared:

.. code-block:: xml

    <dependency>
     <groupId>com.mockrunner</groupId>
     <artifactId>mockrunner</artifactId>
     <scope>test</scope>
    </dependency>
    <dependency>
     <groupId>xmlunit</groupId>
     <artifactId>xmlunit</artifactId>
     <scope>test</scope>
    </dependency>
    <dependency>
     <groupId>org.easymock</groupId>
     <artifactId>easymock</artifactId>
     <scope>test</scope>
    </dependency>

Online Tests
------------

Often a test requires some external resource such as a database or a server to operate. Such
tests should never assume that resource will be available and should skip test execution, 
rather than fail, when the test is not available. 

JUnit4 provides a handy way to do this with the ``org.junit.Asssume`` class. Methods of the 
class are called from a ``@Before`` hook or from a test method. For example consider the 
common case of connecting to a database:

.. code-block:: java

    public class MyTest {
       
        Connection connect() {
            //create a connection to the database
            try {
               Conection cx = ...
               return cx;
            }
            catch(Exception e) {
               LOGGER.log(Level.WARNING, "Connection failed", e);
               return null;
            }
        }

        @Before 
        public void testConnection() {
            Connection cx = connect();
            org.junit.Assume.assumeNotNull(cx);
            cx.close();
        }

        @Test
        public void test1() {
            // test something
        }
    }

In the above example the ``assumeNotNull`` method will throw back an exception telling JUnit 
to skip execution of the test.
