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
  @styleTitle 'The title;
  @styleAbstract 'This is a longer description';
  @uniqueRuleNames 'true';
  
  * { 
    stroke: black 
  }
  
  [cat = 10] { 
    stroke: yellow; stroke-width: 10 
  }

uniqueRuleNames
---------------
The ``uniqueRuleNames`` directive serves a crucial role when it comes to generating legend graphics for complex styles. In the context of our example, where multiple strokes are used to represent different aspects of a highway, setting ``uniqueRuleNames`` to `'true'` ensures that each SLD rule generated from the CSS will have a unique, sequential name. These names are essential for referencing specific rules in a ``GetLegendGraphic`` request, allowing for the creation of a detailed and informative legend. For instance, a client can request the legend graphic for the 'Highway' rule and 'Highway.overlay1' rule separately, and then compose these graphics to accurately reflect the composite symbol for highways on the map. This feature is particularly beneficial when dealing with styles that have multiple layers or symbolizers, as it provides a method to individually access and document the styling of each layer.

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
