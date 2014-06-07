.. _data_VPF:

VPF
===

.. note:: GeoServer does not come built-in with support for VPF; it must be installed through an extension. Proceed to :ref:`vpf_install` for installation details.

Vector Product Format (VPF) is a military standard for vector-based digital map products produced by the U.S. Department of Defense. For more information visit `The National Geospatial-Intelligence Agency <http://www.nga.mil/portal/site/nga01/index.jsp?epi-content=GENERIC&itemID=a2986591e1b3af00VgnVCMServer23727a95RCRD&beanID=1629630080&viewID=Article>`_.

.. _vpf_install:

Installing the VPF extension
----------------------------

#. Download the VPF extension from the `GeoServer download page <http://geoserver.org/download>`_.

   .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

Adding a VPF file
-----------------

Once the extension is properly installed :guilabel:`Vector Product Format Library` will be an option in the :guilabel:`Vector Data Sources` list when creating a new data store.

.. figure:: images/vpfcreate.png
   :align: center

   *VPF in the list of new data sources*

Configuring a VPF data store
----------------------------

.. figure:: images/vpfconfigure.png
   :align: center

   *Configuring a VPF data store*

