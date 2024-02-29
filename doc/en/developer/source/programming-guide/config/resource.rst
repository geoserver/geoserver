.. _config_resource:

Resource API
============

In addition to the formal Catalog API GeoServer needs a way to manage all of the helper files such as icons and fonts, along with freemarker templates, image mosaic property files and many more.

Rather than make direct use of files GeoServer has introduced a Resource API to access this content.  While the primary motivation is to allow sharing resources in a cluster, eventually using a different storage, e..g. database or distributed memory. We took this opportunity to optimize the most common use-cases for file interaction in our codebase.

.. note:: Make use of the Resource API for "file" interaction
   
   While the Resource API does offer the ability to unpack a File onto disk for interaction with libraries like Freemarker that only work with files, the majority of interaction can be handled with input and output streams.

Reference:

* `GSIP-137 - ResourceStore Rest API <https://github.com/geoserver/geoserver/wiki/GSIP-137>`__
* `GSIP-136 - Resource Notification Dispatcher <https://github.com/geoserver/geoserver/wiki/GSIP-136>`__
* `GSIP-132 - GSIP 132 - Resource Store changes <https://github.com/geoserver/geoserver/wiki/GSIP-132>`__

.. note::

   The methods in the Resource API use String parameter names consistently:

   * ``path`` relative location of resource within the data directory.
  
     Paths are represented similar to a File URL, ``file://`` prefix used internally and
     by the resource REST API.
   
   * ``file`` a java File reference.
  
   * ``url`` a location resolved with respect to the data directory.
  
     A large number of special cases developed over time distilled into  ``Resources.fromUrl(base,url)`` and ``Files.url(base,url)`` methods.
  

ResourceStore
-------------

Used to manage configuration storage (file system, test harness, or database blob).
 
InputStream used to access configuration information:

.. code-block:: java

  Properties properties = new Properties();
  try (InputStream in = resourceStore.get("module/configuration.properties").in() ){
    properties.load(in);
  }

An OutputStream is provided for storage (a Resource will be created as needed):

.. code-block:: java

   Properties properties = new Properties();
   properties.put("hello","world");
   try (OutputStream out = resourceStore.get("module/configuration.properties").out() ){
       properties.store( out, null );
   }

A Resource can also be extracted to a file if needed:

.. code-block:: java

   File file = resourceStore.get("module/logo.png");
   BufferedImage img = ImageIO.read( file );

The base directory is available using ``Paths.BASE`` (as ``""`` but relative paths (``.`` and
``..`` are not supported). Path assumes a unix-like file system, all paths are relative and use forward slash
{@code /} as the separator.

Resource
--------

Resource used for configuration storage. Described by ``getType()`` as a ``Type.DIRECTORY``, ``Type.RESOURCE``, or are considered ``Type.UNDEFINED``.

Resource contents are streamed using ``out()`` and ``in()`` methods. The entire contents can be managed in one go using ``setContents(bytes)`` and ``getContents()``.

.. code-block:: java

   try (OutputStream out = resource.out() ){
      properties.store(out)
   }

Resource ``path()`` provides the complete path relative to the ``ResourceStore`` base directory. Use ``name()`` to retrieve the resource name (as the last component in the path name sequence).

Resource creation is handled in a lazy fashion, use ``file()`` or ``out()`` and the resource will be created as required.

Use ``dir()`` to create a empty directory. Directories have the ability to ``list()`` their contents:

.. code-block:: java
   
   for( Resource child : resource.list()) {
      ...    
   }

When creating a resource with ``file()``, ``out()`` or ``dir()`` any required parent directories are created to produce the completed path).

Once created resources can be managed with ``delete()``, ``renameTo(resource)`` methods.

Resource supports ``addListener(listener)`` / ``removeListener(listener)`` event notification allowing code to watch a file for change. A single listener can watch for changes within a folder, with the events providing the path of changed files.

Resource ``lock()`` is also supported.

Paths
-----

The ``Paths`` facade provides methods for working with the relative paths used by ResourceStore.

Helpful methods are provided for working with paths and names:

* ``name(path)``
* ``extension(path)``
* ``parent(path)``
* ``sidecar(path, extension)``
* ``names(path)`` processes the path into a list of names as discussed below.

The definition of a path has been expanded to work with the external locations (with ``Paths.isAbsolute(path)`` and ``Paths.names(path)``).

Paths are broken down into a sequence of names, as listed by ``Paths.names(path)``:

* ``Path.names("data/tasmania/roads.shp")`` is represented as a list of ``data``, ``tasmania``, ``roads.shp``.
* On linux ``Path.names("/src/gis/cadaster/district.geopkg")`` starts with a marker to indicate an absolute path, resulting in ``/``, ``src``, ``gis``, ``cadaster``, ``district.geopkg``.
* On windows ``Path.names("D:/gis/cadaster/district.geopkg")`` starts with a marker to indicate an absolute path, resulting in ``D:/``, ``gis``, ``cadaster``, ``district.geopkg``.



Paths.convert
^^^^^^^^^^^^^

The ``convert`` methods are used to process file references into resource paths:

* ``Paths.convert(base,file)`` - uses URI relativize method to determine relative path (between file and base)
* ``Paths.convert(base,folder, fileLocation)`` - can resolve relative location, limited to content within the base directory
* ``Paths.convert(base, filename)``

Resources
---------

The ``Resources`` facade provides lots of common activities for working with Resource.

Most of these perform common activities or check on resource status ``exists(resource)``, ``hidden(resource)``.

Resources methods provide the flexibility to work with with Resource while not getting caught out in the dfference between DIRECTORY and RESOURCE type.

.. code-block:: java
    
   if( Resources.exists(resource)) {
       // may be a file or a directory
       File fileLocation = Resources.find(resource);
       ...
   }

There are also methods to copy contents into a resource:

.. code-block:: java

   Resources.copy( file, targetDirectory);

There are also method for working with directories recursively and filtering content:

.. code-block:: java
   
   for (Resource svg : Resources.list( resource, new ExtensionFilter("svg"), true )) {
      ...    
   }

Resources.fromUrl
^^^^^^^^^^^^^^^^^

There is an important method ``Resources.fromURL( baseDirectory, url)`` that is used by a lot of code trying to understand data references:

* ``Resources.fromURL( null, "/src/gis/cadaster/district.geopgk")`` - absolute file path (linux)
* ``Resources.fromURL( baseDirectory, "D:\\gis\\cadaster\\district.geopkg")`` - absolute file path (windows)
* ``Resources.fromURL( baseDirectory, "file:///D:/gis/cadaster/district.geopkg")`` - absolute file url (windows)
* ``Resources.fromURL( baseDirectory, "ftp://veftp.gsfc.nasa.gov/bluemarble/")`` - null (external reference)

For the absolute file references above, see the next section on ``Files.url``.

Files
-----

The ``Files`` facade provides methods for working with file objects, and one method of critical importace to the Resource API.

The ``Files.asResource(file)`` method creates a ``ResourceAdapter`` wrapper around an absolute file location. Allows the use of Resource API when working with content outside of the data directory.

Files.url
^^^^^^^^^

The other key method is ``Files.url( baseDirectory, url)`` which is used to look up files based on a user provided URL (or path).

* ``Files.fromURL( null, "resource:styles/logo.svg")`` - internal url format restricted to data directory content
* ``Files.fromURL( null, "/src/gis/cadaster/district.geopgk")`` - absolute file path (linux)
* ``Files.fromURL( baseDirectory, "D:\\gis\\cadaster\\district.geopkg")`` - absolute file path (windows)
* ``Files.fromURL( baseDirectory, "file:///D:/gis/cadaster/district.geopkg")`` - absolute file url (windows)
* ``Files.fromURL( baseDirectory, "ftp://veftp.gsfc.nasa.gov/bluemarble/")`` - null (external reference ignored as we cannot determine a file)
* ``Files.fromURL( baseDirectory, "sde://user:pass@server:port")`` - null (custom strings are ignored as we cannot determine a file)


GeoServerDataDirectory
----------------------

``GeoServerDataDirectory`` is a special ``ResourceStore`` allowing the use of catalog configuration objects to act
as a reference point (rather than having to remember the structure of the data directory).

.. code-block:: java
   
   Resource icon = dataDirectory.get( workspaceInfo, "airports.svg");

``GeoServerDataDirectory`` has plenty of methods that still provide direct file access, internally however they are implemented using the Resource API.

.. code-block:: java

   public File findDataRoot() throws IOException {
       Resource directory = get("data");
       return Resources.directory(directory);
   }

GeoServerResourceLoader
-----------------------

The class ``GeoServerResourceLoader`` operates as a facade mimicking some of the early file based
interaction in our codebase to help during the migration to the ``ResourceStore`` API.

The use of ``location`` parameters here can reference a relative location in the data directory, or an absolute file location on disk.

Each method here can be expressed using the utility classes:

.. code-block:: java

   // Using GeoServerResourceLoader to work with local file
   File configuration = loader.createFile(location);
   try (OutputStream out = new FileOutputStream(configuration)) {
       xstream.toXML(ogrConfiguration, out);
   }
   
   // Using Paths and Resources to work with local file
   Resource resource = resources.get(Paths.convert(location));
   File configuration = Resources.createNewFile(resource);
   try (OutputStream out = new FileOutputStream(configuration)) {
     xstream.toXML(ogrConfiguration, out);
   }
   
   // Using Resource directly to work in clustered environment
   Resource resource = resourceStore.get(Paths.convert(location));
   try (OutputStream out = resource.out()) {
     xstream.toXML(ogrConfiguration, out);
   }
