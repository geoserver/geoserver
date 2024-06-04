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
The uniqueRuleNames directive automatically assigns rule names to individual rules in the thematic styles. These can then be looked up via
REQUEST=GetLegendGraphic&FORMAT=application/json
and the rendered symbology for the individual rules can be fetched via getLegendGraphic-requests including the `rule` parameter,  just like is currently possible for theme styles with rule names written as SLD

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
