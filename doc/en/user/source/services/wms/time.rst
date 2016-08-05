.. _wms_time:

Time Support in GeoServer WMS
=============================

GeoServer supports a ``TIME`` attribute in GetMap requests for layers that are properly configured with a time dimension. This is used to specify a temporal subset for rendering.

For example, you might have a single dataset with weather observations collected over time and choose to plot a single day's worth of observations.

The attribute to be used in ``TIME`` requests can be set up through the GeoServer web interface by navigating to :menuselection:`Layers -> [specific layer] -> Dimensions tab`.

.. note:: Read more about how to :ref:`use the web interface to configure an attribute for TIME requests <data_webadmin_layers_edit_dimensions>`.

Specifying a time
-----------------

The format used for specifying a time in the WMS TIME parameter is based on `ISO-8601 <http://en.wikipedia.org/wiki/ISO_8601>`_. Times may be specified up to a precision of 1 millisecond; GeoServer does not represent time queries with more precision than this.

The parameter is::

  TIME=<timestring>

Times follow the general format::

  yyyy-MM-ddThh:mm:ss.SSSZ

where:

* ``yyyy``: 4-digit year
* ``MM``: 2-digit month
* ``dd``: 2-digit day
* ``hh``: 2-digit hour
* ``mm``: 2-digit minute
* ``ss``: 2-digit second
* ``SSS``: 3-digit millisecond

The day and intraday values are separated with a capital ``T``, and the entire thing is suffixed with a ``Z``, indicating `UTC <http://en.wikipedia.org/wiki/Coordinated_Universal_Time>`_ for the time zone. (The WMS specification does not provide for other time zones.)

GeoServer will apply the ``TIME`` value to all temporally enabled layers in the ``LAYERS`` parameter of the GetMap request.
Layers without a temporal component will be served normally, allowing clients to include reference information like political boundaries along with temporal data.

.. list-table::
   :header-rows: 1

   * - Description
     - Time specification
   * - December 12, 2001 at 6:00 PM
     - ``2001-12-12T18:00:00.0Z``
   * - May 5, 1993 at 11:34 PM
     - ``1993-05-05T11:34:00.0Z``

Specifying an absolute interval
-------------------------------

A client may request information over a continuous interval instead of a single instant by specifying a start and end time, separated by a ``/`` character.

In this scenario the start and end are *inclusive*; that is, samples from exactly the endpoints of the specified range will be included in the rendered tile.

.. list-table::
   :header-rows: 1

   * - Description
     - Time specification
   * - The month of September 2002
     - ``2002-09-01T00:00:00.0Z/2002-09-30T23:59:59.999Z``
   * - The entire day of December 25, 2010
     - ``2010-12-25T00:00:00.0Z/2010-12-25T23:59:59.999Z``

Specifying a relative interval
------------------------------

A client may request information over a relative time interval instead of a set time range by specifying a start or end time with an associated duration, separated by a ``/`` character.

One end of the interval must be a time value, but the other may be a duration value as defined by the ISO 8601 standard.  The special keyword ``PRESENT`` may be used to specify a time relative to the present server time.

.. list-table::
   :header-rows: 1

   * - Description
     - Time specification
   * - The month of September 2002
     - ``2002-09-01T00:00:00.0Z/P1M``
   * - The entire day of December 25, 2010
     - ``2010-12-25T00:00:00.0Z/P1D``
   * - The entire day preceding December 25, 2010
     - ``P1D/2010-12-25T00:00:00.0Z``
   * - 36 hours preceding the current time
     - ``PT36H/PRESENT``

.. note::
   
   The final example could be paired with the KML service to provide a :ref:`google_earth` network link which is always updated with the last 36 hours of data.

Reduced accuracy times
----------------------

The WMS specification also allows time specifications to be truncated by omitting some of the time string. In this case, GeoServer treats the time as a range whose length is equal to the *most precise unit specified* in the time string.

For example, if the time specification omits all fields except year, it identifies a range one year long starting at the beginning of that year.

.. note:: GeoServer implements this by adding the appropriate unit, then subtracting 1 millisecond. This avoids surprising results when using an interval that aligns with the actual sampling frequency of the data - for example, if yearly data is natively stored with dates like 2001-01-01T00:00:00.0Z, 2002-01-01T00:00:00Z, etc. then a request for 2001 would include the samples for both 2001 and 2002, which wouldn't be desired.

.. list-table::
   :header-rows: 1

   * - Description
     - Reduced Accuracy Time
     - Equivalent Range
   * - The month of September 2002
     - ``2002-09``
     - ``2002-09-01T00:00:00.0Z/2002-09-30T23:59:59.999Z``
   * - The day of December 25, 2010
     - ``2010-12-25``
     - ``2010-12-25T00:00:00.0Z/2010-12-25T23:59:59.999Z``

Reduced accuracy times with ranges
----------------------------------

Reduced accuracy times are also allowed when specifying ranges. In this case, GeoServer effectively expands the start and end times as described above, and then includes any samples from after the beginning of the start interval and before the end of the end interval.

.. note:: Again, the ranges are inclusive.

.. list-table::
   :header-rows: 1

   * - Description
     - Reduced Accuracy Time
     - Equivalent Range
   * - The months of September through December 2002
     - ``2002-09/2002-12``
     - ``2002-09-01T00:00:00.0Z/2002-12-31T23:59:59.999Z``
   * - 12PM through 6PM, December 25, 2010
     - ``2010-12-25T12/2010-12-25T18``
     - ``2010-12-25T12:00:00.0Z/2010-12-25T18:59:59.999Z``

.. note:: In the last example, note that the result may not be intuitive, as it includes all times from 6PM to 6:59PM.

Specifying a list of times
--------------------------

GooServer can also accept a list of discrete time values. This is useful for some applications such as animations, where one time is equal to one frame. 

The elements of a list are separated by commas.

.. note::
   
    GeoServer currently does not support lists of ranges, so all list queries effectively have a resolution of 1 millisecond. If you use reduced accuracy notation when specifying a range, each range will be automatically converted to the instant at the beginning of the range.

If the list is evenly spaced (for example, daily or hourly samples) then the list may be specified as a range, using a start time, end time, and period separated by slashes.

.. list-table::
   :header-rows: 1

   * - Description
     - List notation
     - Equivalent range notation
   * - Noon every day for August 12-14, 2012
     - ``TIME=2012-08-12T12:00:00.0Z,2012-08-13T12:00:00.0Z,2012-08-14T12:00:00.0Z``
     - ``TIME=2012-08-12T12:00:00.0Z/2012-08-18:T12:00:00.0Z/P1D``
   * - Midnight on the first of September, October, and November 1999
     - ``TIME=1999-09-01T00:00:00.0Z,1999-10-01T00:00:00.0Z,1999-11-01T00:00:00.0Z``
     - ``TIME=1999-09-01T00:00:00.0Z/1999-11-01T00:00:00.0Z/P1M``

Specifying a periodicity
------------------------

The periodicity is also specified in ISO-8601 format: a capital P followed by one or more interval lengths, each consisting of a number and a letter identifying a time unit:

.. list-table::
   :header-rows: 1

   * - Unit
     - Abbreviation
   * - Years
     - ``Y``
   * - Months
     - ``M``
   * - Days
     - ``D``
   * - Hours
     - ``H``
   * - Minutes
     - ``M``
   * - Seconds
     - ``S``

The Year/Month/Day group of values must be separated from the Hours/Minutes/Seconds group by a ``T`` character. The T itself may be omitted if hours, minutes, and seconds are all omitted. Additionally, fields which contain a 0 may be omitted entirely.

Fractional values are permitted, but only for the most specific value that is included.

.. note:: The period must divide evenly into the interval defined by the start/end times. So if the start/end times denote 12 hours, a period of 1 hour would be allowed, but a period of 5 hours would not. 

For example, the multiple representations listed below are all equivalent.

* One hour::

        P0Y0M0DT1H0M0S

        PT1H0M0S

        PT1H

* 90 minutes::

        P0Y0M0DT1H30M0S

        PT1H30M

        P90M

* 18 months::

        P1Y6M0DT0H0M0S

        P1Y6M0D

        P0Y18M0DT0H0M0S

        P18M

  .. note:: ``P1.25Y3M`` would not be acceptable, because fractional values are only permitted in the most specific value given, which in this case would be months. 

