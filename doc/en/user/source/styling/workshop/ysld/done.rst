.. _styling_workshop_ysld_done:

YSLD Workbook Conclusion
========================

We hope you have enjoyed this styling workshop.

Additional resources:

* :ref:`YSLD Extension <ysld_styling>`
* :ref:`YSLD Reference <ysld_reference>`

YSLD Tips and Tricks
--------------------

Converting to YSLD
^^^^^^^^^^^^^^^^^^

The REST API can be used to convert any of your existing CSS or SLD styles to YSLD.

YSLD Workshop Answer Key
------------------------

The following questions were listed through out the workshop as an opportunity to explore the material in greater depth. Please do your best to consider the questions in detail prior to checking here for the answer. Questions are provided to teach valuable skills, such as a chance to understand how feature type styles are used to control z-order, or where to locate information in the user manual.

.. _ysld.line.a1:

Classification
^^^^^^^^^^^^^^

Answer for :ref:`Challenge Classification <ysld.line.q1>`:

#. **Challenge:** Create a new style adjust road appearance based on **type**.

   .. image:: ../style/img/line_type.png

   Hint: The available values are 'Major Highway','Secondary Highway','Road' and 'Unknown'.

#. Here is an example:
  
   .. code-block:: yaml

       define: &common
         stroke-opacity: 0.25
   
       rules:
       - filter: ${type = 'Major Highway'}
         symbolizers:
         - line:
             stroke-color: '#000088'
             stroke-width: 1.25
             <<: *common
       - filter: ${type = 'Secondary Highway'}
         symbolizers:
         - line:
             stroke-color: '#8888AA'
             stroke-width: 0.75
             <<: *common
       - filter: ${type = 'Road'}
         symbolizers:
         - line:
             stroke-color: '#888888'
             stroke-width: 0.75
             <<: *common
       - filter: ${type = 'Unknown'}
         symbolizers:
         - line:
             stroke-color: '#888888'
             stroke-width: 0.5
             <<: *common
       - else: true
         symbolizers:
         - line:
             stroke-color: '#AAAAAA'
             stroke-width: 0.5
             <<: *common
             
.. _ysld.line.a2:

One Rule Classification
^^^^^^^^^^^^^^^^^^^^^^^

Answer for :ref:`Challenge One Rule Classification <ysld.line.q2>`:

#. **Challenge:** Create a new style and classify the roads based on their scale rank using expressions in a single rule instead of multiple rules with filters.

#. This exercise requires looking up information in the user guide, the search tearm *recode* provides several examples.
   
   * The YSLD Reference :ref:`theming functions <ysld_reference_functions_theming>` provides a clear example.

.. _ysld.line.a3:

Label Shields
^^^^^^^^^^^^^

Answer for :ref:`Challenge Label Shields <ysld.line.q3>`:

#. *Challenge:* Have a look at the documentation for putting a graphic on a text symbolizer in SLD and reproduce this technique in YSLD.

   .. image:: ../style/img/line_shield.png

#. The use of a label shield is a vendor specific capability of the GeoServer rendering engine. The tricky part of this exercise is finding the documentation online ( i.e. :ref:`TextSymbolizer - Graphic <sld_reference_textsymbolizer>`).
      
   .. code-block:: yaml
 
       symbolizers:
       - line:
           stroke-color: '#000000'
           stroke-width: 3
       - line:
           stroke-color: '#D3D3D3'
           stroke-width: 2
       - text:
           label: ${name}
           fill-color: '#000000'
           font-family: Ariel
           font-size: 10
           font-style: normal
           font-weight: normal
           placement: point
           graphic:
             size: 18
             symbols:
             - mark:
                 shape: square
                 stroke-color: '#000000'
                 stroke-width: 1
                 fill-color: '#FFFFFF'
