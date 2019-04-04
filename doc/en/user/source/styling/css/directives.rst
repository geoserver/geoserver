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
  @styleTitle 'The title;
  @styleAbstract 'This is a longer description'
  
  * { 
    stroke: black 
  }
  
  [cat = 10] { 
    stroke: yellow; stroke-width: 10 
  }

  
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
    - * ``styleTitle``
      * String
      * The generated SLD style title  
      * No
    - * ``styleAbstract`` 
      * String
      * The generated SLD style abstract/description
      * No
