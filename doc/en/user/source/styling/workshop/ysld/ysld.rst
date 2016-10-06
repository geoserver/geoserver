.. _styling_workshop_ysld_quickstart:

YSLD Quickstart
===============

In the last section, we saw how the OGC defines style using XML documents (called SLD files).

We will now explore GeoServer styling in greater detail using a tool to generate our SLD files. The **YSLD** GeoServer extension is used to generate SLD files using a clearer, more consice language based on **YAML**. Unlike **CSS**, the **YSLD** styling language has a one-to-one correspondance to SLD, meaning that each line of YSLD translates directly to one or more lines of SLD. Aditionally, A YSLD style can be converted to SLD *and back* without loss of information.

Using the YSLD extension to define styles results in shorter examples that are easier to understand. At any point we will be able to review the generated SLD file.

Reference:

* :ref:`YSLD Reference <ysld_reference>`

Syntax
------

This section provides a quick introduction to YSLD syntax for mapping professionals who may not be familiar with YAML.

Property Syntax
^^^^^^^^^^^^^^^

Individual statements (or directives) in a YSLD styling document are designed as key-value, or property-value pairs of the following form:

 .. code-block:: yaml
    
    <property>: <value>

The :kbd:`<property>` is a string denoting the property name, while the :kbd:`<value>` can be one of a number of different types depending on context.

=========== ====================================================
Integer     Numerical value. May be surrounded by quotes.
Float       Numerical value. May be surrounded by quotes.
Text        Text value.  If value is amiguous, use single quotes.
Color       Hexadecimal color of the form :kbd:`'#RRGGBB'`.
Tuple       A list of values in brackets. e.g. :kbd:`[0, 1]`
Expression  CQL expression surrounded by :kbd:`${ }`
=========== ====================================================

Mappings and lists
^^^^^^^^^^^^^^^^^^

There are three types of objects in a YSLD document:

#. Scalar, a simple value

#. Mapping, a collection of key-value (property-value) pairs

#. List, any collection of objects. A list can contain mappings, scalars, and even other lists.

   Lists require dashes for every entry, while mappings do not.

For example, a symbolizer block is a list, so every entry requires its own dash:

 .. :code-block:: yaml

    - symbolizer:
      - polygon:
          ...
      - text:
          ...


The :kbd:`polygon:` and :kbd:`text:` objects (the individual symbolizers themselves) are mappings, and as such, the contents do not require dashes, only indents:

 .. code-block:: yaml

    - polygon:
        stroke-color: '#808080'
        fill-color: '#FF0000'

The dash next to polygon means that the item itself is contained in a list, not that it contains a list. And the placement of the dash is at the same level of indentation as the list title.

If you have a list that contains only one item, and there is no other content at higher levels of the list, you may omit the enclosing elements. For example, the following are equivalent:

 .. code-block:: yaml

    feature-styles:
    - rules:
      - symbolizers:
        - point:
            symbols:
            - mark:
                shape: circle
                fill-color: 'gray'



 .. code-block:: yaml

    point:
      symbols:
      - mark:
          shape: circle
          fill-color: 'gray'

This is usefull for making your styles more concise.

Indentation
^^^^^^^^^^^

Indentation is very important in YSLD. All directives must be indented to its proper place to ensure proper hierarchy. Improper indentation will cause a style to be rendered incorrectly, or not at all.

For example, the polygon symbolizer, since it is a mapping, contains certain parameters inside it, such as the color of the fill and stroke. These must be indented such that they are “inside” the polygon block.

In this example, the following markup is **correct**:

 .. code-block:: yaml

    - polygon:
        fill-color: '#808080'
        fill-opacity: 0.5
        stroke-color: black
        stroke-opacity: 0.75

The parameters inside the polygon (symbolizer) are indented, meaning that they are referencing the symbolizer and are not “outside it.”

Compare to the following **incorrect** markup:

 .. code-block:: yaml

    - polygon:
      fill-color: '#808080'
      fill-opacity: 0.5
      stroke-color: black
      stroke-opacity: 0.75

Rules
^^^^^

We have already seen a CSS style composed of a single rule:

 .. code-block:: yaml
   
    point:
      symbols:
      - mark:
          shape: circle
          fill-color: 'gray'
   
We can make a style consisting of more than one rule, carefully choosing the selector for each rule. In this case we are using a selector to style capital cities with a star, and non-capital with a circle:

 .. code-block:: yaml
   
    rules:
      - filter: ${FEATURECLA = 'Admin-0 capital'}
        scale: [min, max]
        symbolizers:
        - point:
            size: 6
            symbols:
            - mark:
                shape: star
                stroke-color: 'black'
                stroke-width: 1
                fill-color: 'gray'
      - filter: ${FEATURECLA <> 'Admin-0 capital'}
        scale: [min, max]
        symbolizers:
        - point:
            size: 6
            symbols:
            - mark:
                shape: circle
                stroke-color: 'black'
                stroke-width: 1
                fill-color: 'gray'

The feature attribute test performed above uses **Constraint Query Language (CQL)**. This syntax can be used to define filters to select content, similar to how the SQL WHERE statement is used. It can also be used to define expressions to access attribute values allowing their use when defining style properties.

Rule selectors can also be triggered based on the state of the rendering engine. In this example we are only applying labels when zoomed in:

 .. code-block:: yaml

    rules:
      - scale: [min, '2.0E7']
        symbolizers:
        - text:
            label: ${NAME}
            fill-color: 'gray'

In the above example the label is defined using the CQL Expression :kbd:`NAME`. This results in a dynamic style that generates each label on a case-by-case basis, filling in the label with the feature attribute :kbd:`NAME`.

Reference:

* `Filter Syntax <http://suite.opengeo.org/docs/latest/cartography/ysld/reference/filters.html>`__ (YSLD Reference)
* :ref:`ECQL Reference <filter_ecql_reference>` (User Guide)

Variables
^^^^^^^^^

Up to this point we have been styling individual features, documenting how each shape is represented.

When styling multiple feaures, or using filters to style individual features in different yars, you may need to repeat styling information.

Variables in YSLD allow for a certain directive or block of directives to be defined by name and later reused. This can greatly simplify the styling document.

The two most-common use cases for using variables are:

* To create a more-friendly name for a value (such as using myorange instead of :kbd:`#EE8000`)

* To define a block of directives to remove redundant content and to decrease file length

It is customary, but not required, to place all definitions at the very top of the YSLD file.

The syntax for defining a variable as a single value is:

 .. code-block:: yaml

    define: &variable <value>

The defined variable can then be used as a value by variable name with a :kbd:`*`:

 .. code-block:: yaml

    <directive>: *variable

The syntax for defining a variable as a content block is:

 .. code-block:: yaml

    define: &varblock
      <directive>: <value>
      <directive>: <value>
      ...
      <block>:
      - <directive>: <value>
        <directive>: <value>
      ...

The syntax for using a variable block is to prepend the variable name with :kbd:`<<: *`. For example:

 .. code-block:: yaml

    <block>:
    - <directive>: <value>
      <<: *varblock


* `Variables <http://suite.opengeo.org/docs/latest/cartography/ysld/reference/variables.html>`__ (YSLD Reference)

Compare YSLD to SLD
-------------------
   
As noted above, YSLD has a one-to-one correspondance with SLD, it merely uses a different markup language to diplay the same content. We can compare a SLD style with a YSLD style to see this correspondence:

SLD Style
^^^^^^^^^

Here is an example :download:`SLD file <../files/airports0.sld>` for reference: 

.. literalinclude:: ../files/airports0.sld
   :language: xml
   :linenos:

YSLD Style
^^^^^^^^^^

Here is the same example as :download:`YSLD <../files/airports0.ysld>`: 

.. literalinclude:: ../files/airports0.ysld
   :language: yaml
   :linenos:

We use a point symbolizer to indicate we want this content drawn as a **Point** (line 16 in the SLD, line 5 in the YSLD). The point symbolizer declares an external graphic, which contains the URL :kbd:`airports.svg` indicating the image that should be drawn (line 20 in the SLD, line 9 in the YSLD).

Tour
----

To confirm everything works, let's reproduce the airports style above.

#. Navigate to the **Styles** page.

#. Each time we edit a style, the contents of the associated SLD file are replaced. Rather then disrupt any of our existing styles we will create a new style. Click :guilabel:`Add a new style` and choose the following:

   .. list-table:: 
      :widths: 30 70
      :header-rows: 0

      * - Name:
        - :kbd:`airports0`
      * - Workspace:
        - (leave empty)
      * - Format:
        - :kbd:`YSLD`

#. Replace the initial YSLD definition with with our airport YSLD example and click :guilabel:`Apply`:

    .. literalinclude:: ../files/airports0.ysld
       :language: yaml

#. Click the :guilabel:`Layer Preview` tab to preview the style. We want to preview on the aiports layer, so click the name of the current layer and select :kbd:`ne:airports` from the list that appears. You can use the mouse buttons to pan and scroll wheel to change scale.

   .. figure:: ../style/img/css_02_choose_data.png

      Choosing the airports layer

   .. figure:: ../style/img/css_06_preview.png

      Layer preview

#. Click :guilabel:`Layer Data` for a summary of the selected data.

   .. figure:: ../style/img/css_07_data.png

      Layer attributes

Bonus
-----

Finished early? For now please help your neighbour so we can proceed with the workshop.

If you are really stuck please consider the following challenge rather than skipping ahead.

Explore Data
^^^^^^^^^^^^

#. Return to the :guilabel:`Data` tab and use the :guilabel:`Compute` link to determine the minimum and maximum for the **scalerank** attribute.

   .. only:: instructor
 
      .. admonition:: Instructor Notes

         Should be 2 and 9 respectively.

Challenge Compare SLD Generation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

# The rest API can be used to review your YAML file directly.
   
  Browser:
  
  * `view-source:http://localhost:8080/geoserver/rest/styles/airport0.yaml <view-source:http://localhost:8080/geoserver/rest/styles/airport0.yaml>`__

  Command line::

     curl -v -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/styles/airports0.yaml

#. The REST API can also be used generate an SLD file:
   
   Browser:
   
   * `view-source:http://localhost:8080/geoserver/rest/styles/airport0.sld?pretty=true <view-source:http://localhost:8080/geoserver/rest/styles/airport0.sld?pretty=true>`__

  Command line::

     curl -v -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/styles/airports0.sld?pretty=true

#. Compare the generated SLD differ above with the hand written :download:`SLD file <../files/airports0.sld>` used as an example?
   
   **Challenge:** What differences can you spot?
   
   .. only:: instructor
    
      .. admonition:: Instructor Notes      

         Generated SLD does not include name or title information; this can of course be added. Please check the YSLD reference for details.

         The second difference is with the use of a fallback Mark when defining a PointSymbolizer.