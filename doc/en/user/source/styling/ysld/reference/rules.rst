.. _ysld_reference_rules:

Rules
=====

A rule is a **collection of styling directives**, primarily consisting of :ref:`symbolizers <ysld_reference_symbolizers>` combined with optional conditional statements.

* **If a conditional statement exists** in a rule, then the styling directives will only be carried out **if the conditional returns true**. Otherwise, the rule will be skipped.
* **If no conditional statement exists** in a rule, then the styling directive will **always be carried out**.

.. todo:: FIGURE NEEDED

The types of conditional statements available to rules are:

* :ref:`Filters <ysld_reference_filters>` for attribute-based rendering
* :ref:`Scale <ysld_reference_scalezoom>` for scale-based rendering

Rules are contained within :ref:`feature styles <ysld_reference_featurestyles>`. There is no limit on the number of rules that can be created, and there is no restriction that all rules must be mutually exclusive (as in, some rules may apply to the same features).

Syntax
------

The following is the basic syntax of a rule. Note that the contents of the block are not all expanded; see the other sections for the relevant syntax.

::

     rules:
     - name: <text>
       title: <text>
       filter: <filter>
       else: <boolean>
       scale: [<min>,<max>]
       symbolizers:
       - ...

where:

.. list-table::
   :class: non-responsive
   :header-rows: 1
   :stub-columns: 1
   :widths: 20 10 50 20

   * - Property
     - Required?
     - Description
     - Default value
   * - ``name``
     - No
     - Internal reference to the feature style. It is recommended that the value be **lower case** and contain **no spaces**.
     - Blank
   * - ``title``
     - No
     - Human-readable description of what the rule accomplishes.
     - Blank
   * - ``filter``
     - No
     - :ref:`Filter <ysld_reference_filters>` expression which will need to evaluate to be true for the symbolizer(s) to be applied. Cannot be used with ``else``.
     - Blank (meaning that the rule will apply to all features)
   * - ``else``
     - No
     - Specifies whether the rule will be an "else" rule. An else rule applies when, after scale and filters are applied, no other rule applies. To make an else rule, set this option to ``true``. Cannot be used with ``filter``.
     - ``false``
   * - ``scale``
     - No
     - :ref:`Scale <ysld_reference_scalezoom>` boundaries showing at what scales (related to zoom levels) the rule will be applied.
     - Visible at all scales
   * - ``symbolizers``
     - Yes
     - Block containing one or more :ref:`symbolizers <ysld_reference_symbolizers>`. These contain the actual visualization directives. If the filter returns true and the view is within the scale boundaries, these symbolizers will be drawn.
     - N/A

Short syntax
------------

When a style has a single rule inside a single feature style, it is possible to omit the syntax for both and start at the first parameter inside.

So the following complete styles are equivalent::

  feature-styles:
  - rules:
    - symbolizers:
      - line:
          stroke-color: '#000000'
          stroke-width: 2

::

  line:
    stroke-color: '#000000'
    stroke-width: 2

Examples
--------

Else filter
~~~~~~~~~~~

Using ``filter`` and ``else`` together::

  rules:
  - name: small
    title: Small features
    filter: ${type = 'small'}
    symbolizers:
    - ...
  - name: large
    title: Large features
    filter: ${type = 'large'}
    symbolizers:
    - ...
  - name: else
    title: All other features
    else: true
    symbolizers:
    - ...

In the above situation:

* If a feature has a value of "small" in its ``type`` attribute, it will be styled with the "small" rule.
* If a feature has a value of "large" in its ``type`` attribute, it will be styled with the "large" rule.
* If a feature has a value of "medium" (or anything else) in its ``type`` attribute, it will be styled with the "else" rule.

Else with scale
~~~~~~~~~~~~~~~

Using ``filter``, ``else``, and ``scale`` together::

  rules:
  - name: small_zoomin
    scale: [min,10000]
    title: Small features when zoomed in
    filter: ${type = 'small'}
    symbolizers:
    - ...
  - name: small_zoomout
    scale: [10000,max]
    title: Small features when zoomed out
    filter: ${type = 'small'}
    symbolizers:
    - ...
  - name: else_zoomin
    scale: [min,10000]
    title: All other features when zoomed in
    else: true
    symbolizers:
    - ...
  - name: else_zoomout
    scale: [10000,max]
    title: All other features when zoomed out
    else: true
    symbolizers:
    - ...

In the above situation:

* If a feature has a value of "small" in its ``type`` attribute, and the map is a scale level less than 10,000, it will be styled with the "small_zoomin" rule.
* If a feature has a value of anything else other than "small" in its ``type`` attribute, and the map is a scale level less than 10,000, it will be styled with the "else_zoomin" rule.
* If a feature has a value of "small" in its ``type`` attribute, and the map is a scale level greater than 10,000, it will be styled with the "small_zoomout" rule.
* If a feature has a value of anything else other than "small" in its ``type`` attribute, and the map is a scale level greater than 10,000, it will be styled with the "else_zoomout" rule.

