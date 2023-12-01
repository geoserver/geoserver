.. _sld_reference_styles:

Styles
======

The style elements specify the styling to be applied to a layer.

UserStyle
---------

The **UserStyle** element defines styling for a layer.

The ``<UserStyle>`` element contains the following elements:

.. list-table::
   :widths: 25 15 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<Name>``
     - No
     - The name of the style,
       used to reference it externally.
       (Ignored for catalog styles.)
   * - ``<Title>``
     - No
     - The title of the style.
   * - ``<Abstract>``
     - No
     - The description for the style.
   * - ``<IsDefault>``
     - No
     - Whether the style is the default one for a named layer.
       Used in SLD **Library Mode**.
       Values are ``1`` or ``0`` (default).
   * - ``<FeatureTypeStyle>``
     - 1..N
     - Defines the symbology for rendering a single feature type.
      
       
FeatureTypeStyle
----------------

The **FeatureTypeStyle** element specifies the styling 
that is applied to a single feature type of a layer.
It contains a list of rules which determine the symbology
to be applied to each feature of a layer.

The ``<FeatureTypeStyle>`` element contains the following elements:

.. list-table::
   :widths: 25 15 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<Name>``
     - No
     - Not used at present
   * - ``<Title>``
     - No
     - The title for the style.
   * - ``<Abstract>``
     - No
     - The description for the style.
   * - ``<FeatureTypeName>``
     - No
     - Identifies the feature type the style is to be applied to.
       Omitted if the style applies to all features in a layer.
   * - ``<Rule>``
     - 1..N
     - A styling rule to be evaluated.  See :ref:`sld_reference_rules`

Usually a layer contains only a single feature type, so the ``<FeatureTypeName>``
is omitted.

Any number of ``<FeatureTypeStyle>`` elements can be specified in a style.
In GeoServer each one is rendered into a separate image buffer.
After all features are rendered the buffers are composited to form the final layer image.
The compositing is done in the order the FeatureTypeStyles are
given in the SLD, with the first one on the bottom
(the "Painter's Model").
This effectively creates "virtual layers", 
which can be used to achieve styling effects such as cased lines.


