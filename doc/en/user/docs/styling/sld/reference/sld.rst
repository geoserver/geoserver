.. _sld_reference_sld:

StyledLayerDescriptor
=====================

The root element for an SLD is ``<StyledLayerDescriptor>``.
It contains a sequence of :ref:`sld_reference_layers` defining the styled map content.

The ``<StyledLayerDescriptor>`` element contains the following elements:

.. list-table::
   :widths: 25 15 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<NamedLayer>``
     - 0..N
     - A reference to a named layer in the server catalog
   * - ``<UserLayer>``
     - 0..N
     - A layer defined in the style itself
       




       
       



