 .. _community_metadata_configuration:

Getting Started
===============

.. contents:: :local:
    :depth: 1

Installation
------------

To install the GeoServer Metadata extension:

-  Download the extension from the `GeoServer Download
   Page <http://geoserver.org/download>`__. The file name is called
   ``geoserver-*-metadata-plugin.zip``, where ``*`` is the
   version/snapshot name.

-  Extract this file and place the JARs in ``WEB-INF/lib``.

-  Perform any configuration required by your servlet container, and
   then restart.  On startup, Metadata module will create a configuration
   directory ``metadata`` in the GeoServer Data Directory. The module will scan all `yaml <https://yaml.org/>`__ files in the ``metadata`` directory.

Basic configuration
--------------------
By default the metadata module will add an extra tab to the edit layer page. Open the layer: navigate to :menuselection:`Layers --> Choose the layer --> Metadata tab`.

.. figure:: images/empty-default.png
  
  The initial UI. Note the :guilabel:`Metadata fields` panel is still empty

The content of the :guilabel:`Metadata fields` is configured by placing one or multiple `yaml <https://yaml.org/>`__ files describing the UI compontents in the metadata configuration folder, see :ref:`tutorial_metadata` for a real life example.

Example UI configuration:

.. code:: YAML

  attributes:
    - key: metadata-identifier
      fieldType: UUID
    - key: metadata-datestamp
      label: Date
      fieldType: DATETIME
    - key: data-language
      fieldType: DROPDOWN
      values:
            - dut
            - eng
            - fre
            - ger
    - key: topic-category
      fieldType: SUGGESTBOX
      occurrence: REPEAT
      values:
            - farming
            - biota
            - boundaries
            - climatologyMeteorologyAtmosphere
            - economy
            - elevation 
    - key: data-date
      fieldType: COMPLEX
      typename: data-identification-date
      occurrence: REPEAT            
  types:    
     - typename: data-identification-date
       attributes:
        - key: date
          fieldType: DATE
        - key: date-type
          fieldType: DROPDOWN
          values:
            - creation
            - publication
            - revision  

This configuration results in the following GUI:

.. figure:: images/basic-gui.png



There are 2 main parts in the `yaml <https://yaml.org/>`__:

    - **attributes:** a list of GUI components that will be rendered in the tab. They can be a basic type or a complex type, a complex type is a collection of basic types.
    - **types:** a list that defines the fields in each complex type.

:ref:`community_metadata_uiconfiguration` gives an overview of all supported types and advanced features.


