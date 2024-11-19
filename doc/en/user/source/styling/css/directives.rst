.. _css_directives:

Directives
==========

A directive is a CSS top level declaration that allows control of some aspects of the stylesheet application or translation to SLD.
All directives are declared at the beginning of the CSS sheet and follow the same syntax:

.. code-block:: css

  @name value;
  

For example:

.. code-block:: scss

  @mode 'Flat';
  @styleName 'The name';
  @styleTitle 'The title';
  @styleAbstract 'This is a longer description';
  @autoRuleNames 'true';
  
  * { 
    stroke: black 
  }
  
  [cat = 10] { 
    stroke: yellow; stroke-width: 10 
  }

autoRuleNames
---------------
The autoRuleNames directive automatically assigns rule names to individual rules in thematic styles.
This is useful for instance when creating legends in an application where it is possible to toggle visibility of individual symbols of a theme style.
The workflow might look like so:
 
1. The application fetches the symbology of the theme layer as JSON ( `GetLegendGraphic&FORMAT=application/json`) to determine which symbols are available
2.  The application examines the JSON document and fetches the symbol for each rule by rule name (`GetLegendGraphic&rule=rulename`) and creates a legend displaying all symbols, including their title, with an accompanying radio button for toggling that particular symbol. Letting the client render the title instead of asking Geoserver to do it as part of a composite legend image can allow the legend to look nicer.
3. When a symbol is toggled, the associated filter from the JSON response is applied to the layer source so that the layer is updated in the map.
 
Here the name of the rule is not important, there just needs to be a rule name in addition to a title for each rule so that its symbology can be fetched, just like is possible when styling with SLD.

Supported directives
--------------------

.. list-table::
    :widths: 15 15 60 10
    :header-rows: 1

    - * Directive
      * Type
      * Meaning
      * Accepts Expression?
    - * ``mode``    
      * String, ``Exclusive``, ``Simple``, ``Auto``, ``Flat`` 
      * Controls how the CSS is translated to SLD. ``Exclusive``, ``Simple`` and ``Auto`` are cascaded modes, ``Flat`` turns off cascading and has the CSS 
        behave like a simplified syntax SLD sheet. See :ref:`css_cascading` for an explanation of how the various modes work
      * false
    - * ``styleName``
      * String
      * The generated SLD style name
      * No
    - * ``styleTitle``
      * String
      * The generated SLD style title  
      * No
    - * ``styleAbstract`` 
      * String
      * The generated SLD style abstract/description
      * No
    - * ``uniqueRuleNames`` 
      * Boolean
      * If set to `'true'`, Instructs the translator to give each generated SLD rule a unique name, as a progressive number starting from zero
      * No
