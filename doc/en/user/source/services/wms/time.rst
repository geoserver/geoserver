.. _wms_time:

Time Support in Geoserver WMS
=============================

For layers that are properly configured with a TIME dimension, GeoServer supports a TIME attribute in GetMap requests to specify a temporal subset for rendering.
For example, you might have a single dataset with weather observations collected over time and choose to plot a single day's worth of observations.

Specifying a Time
-----------------

The format used for specifying a time in the WMS TIME parameter is based on `ISO-8601 <http://en.wikipedia.org/wiki/ISO_8601>`_.
Times may be specified to a precision of 1 millisecond; GeoServer does not represent time queries with more precision than this.
Times follow the general format::

    yyyy-MM-ddThh:mm:ss.SSSZ

That is, a day specified by a 4-digit year, 2-digit month, and 2-digit day-of-month field, and an instant on that day specified by 2-digit hour, minute, and second fields, with an arbitrary number of decimal digits after the seconds field. 
The day and instant seconds are separated with a capital 'T', and the entire thing is suffixed with a 'Z' (indicating 'Zulu' or `UTC <http://en.wikipedia.org/wiki/Coordinated_Universal_Time>` for the time zone.  The WMS specification does not provide for other time zones.)

GeoServer will apply the TIME value to all temporally enabled layers in the LAYERS parameter of the GetMap request.
Layers without a temporal component will be served normally - allowing clients to include reference information like political boundaries along with temporal data.

Examples
........

   * December 12, 2001 at 6:00 PM would be represented as::

       TIME=2001-12-12T18:00:00.0Z

   * May 5, 1993 at 11:34 PM would be represented as::

       TIME=1993-05-05T11:34:00.0Z

Specifying a Periodicity
------------------------

The periodicity is also specified in ISO-8601 format: a capital P followed by one or more interval lengths, each consisting of a number and a letter identifying a time unit:

.. list-table::
   
   * - **Unit**
     - **Abbreviation**
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

The Year/Month/Day group of values must be separated from the Hours/Minutes/Seconds group by a ``T`` character.
Additionally, fields which contain a 0 may be omitted entirely, and the T may be omitted if hours, minutes, and seconds are all omitted.
Fractional values are permitted, but only for the most specific value that is included.

The period must divide evenly into the interval defined by the start/end times.

Examples of Periods
...................

    * One hour::

        P0Y0M0DT1H0M0S

        PT1H0M0S

        PT1H

    * 90 minutes (equivalent to 1 hour, 30 minutes)::

        P0Y0M0DT1H30M0S

        PT1H30M

        P90M

    * 18 months::

        P1Y6M0DT0H0M0S

        P1Y6M0D

        P0Y18M0DT0H0M0S

        P18M

        *But not* `` P1.25Y3M

Specifying an Interval
----------------------

A client may request information over a continuous interval instead of a single instant by specifying a start and end time, separated by a ``/`` character.
In this scenario the start and end are both inclusive; that is, samples from exactly the endpoints of the specified range will be included in the rendered tile.

Examples
........


.. list-table::

   * - Description
     - Time specification
   * - The month of September 2002
     - ``2002-09-01T00:00:00.0Z/2002-10-01T23:59:59.999Z``
   * - The entire day of December 25, 2010
     - ``2010-12-25T00:00:00.0Z/2010-12-25T23:59:59.999Z``

.. note:: 

   Because the time interval is inclusive, we cannot precisely specify a concept such as "all times within day x".
   We must choose between incorrectly accepting observations that occur at the end point, and incorrectly excluding some fraction of the final second of the interval.
   In practice, GeoServer and many data storage engines have limited resolution in their representations, so approximating a range to the nearest millisecond is 'as good as we can do.'
   It is possible that this technical constraint may be lifted at some point in the future.

Specifying a Relative Interval
------------------------------

A client may request information over a relative time interval instead of a set time range by specifying a start or end time with an associated duration, separated by a ``/`` character.
One end of the interval must be a time value, but the other may be a duration value as defined by the ISO 8601 standard.  The special keyword PRESENT may be used to specify a time relative to the present server time.

Examples
........


.. list-table::

   * - Description
     - Time specification
   * - The month of September 2002
     - ``2002-09-01T00:00:00.0Z/P1M``
   * - The entire day of December 25, 2010
     - ``2010-12-25T00:00:00.0Z/P1D``
   * - The entire day preceding December 25, 2010
     - ``P1D/2010-12-25T00:00:00.0Z``
   * - 36 hours preceding the present time
     - ``PT36H/PRESENT``

.. note::
   
    The final example could be paired with the KML service to provide a Google Earth network link always updated with the last 36 hours of data.

Reduced Accuracy Times
----------------------

The WMS specification also allows time specifications to be truncated, by omitting some suffix of the time string.
In this case, GeoServer treats the time as a range whose length is equal to one of the most precise unit specified in the time string.
If time specification omits all fields except year, it identifies a range one year long starting at the beginning of that year, etc.

GeoServer implements this by adding the appropriate unit, then subtracting 1 millisecond.
This avoids surprising results when using an interval that aligns with the actual sampling frequency of the data - for example, if yearly data is natively stored with dates like 2001-01-01T00:00:00.0Z, 2002-01-01T00:00:00Z, etc. then a request for 2001 would include the samples for both 2001 and 2002 otherwise.

Examples
........

.. list-table::

   * - Description
     - Reduced Accuracy Time
     - Equivalent Range
   * - The month of September 2002
     - ``2002-09``
     - ``2002-09-01T00:00:00.0Z/2002-10-01T23:59:59.999Z``
   * - The day of December 25, 2010
     - ``2010-12-25``
     - ``2010-12-25T00:00:00.0Z/2010-12-25T23:59:59.999Z``

Ranges with Reduced Accuracy Times
----------------------------------

Reduced accuracy times are also allowed when specifying ranges.
In this case, GeoServer effectively expands the start and end times as described above, and then includes any samples from after the beginning of the start interval and before the end of the end interval.

.. list-table::

   * - Description
     - Reduced Accuracy Time
     - Equivalent Range
   * - The months of September through December 2002
     - ``2002-09/2002-12``
     - ``2002-09-01T00:00:00.0Z/2002-12-31T23:59:59.999Z``
   * - 12pm through 6pm, December 25, 2010
     - ``2010-12-25T12/2010-12-25T18``
     - ``2010-12-25T12:00:00.0Z/2010-12-25T18:59:59.999Z``

Specifying a List of Times
--------------------------

For some formats, GeoServer can generate an animation.
In this case, the client must specify multiple times, one for each frame.
When multiple times are needed, the client should simply format each time as described above, and separate them with commas.

If the list is evenly spaced (for example, daily or hourly samples) then the list may be specified as a range, using a start time, end time, and period separated by slashes.

Examples
........

.. list-table::

   * - Description
     - List notation
     - Range notation
   * - Noon every day for the week of August 12-18, 2012
     - ``TIME=2012-08-12T12:00:00.0Z,2012-08-13T12:00:00.0Z,2012-08-14T12:00:00.0Z,2012-08-15T12:00:00.0Z,2012-08-16T12:00:00.0Z,2012-08-17T12:00:00.0Z,2012-08-18T12:00:00.0Z``
     - ``TIME=2012-08-12T12:00:00.0Z/2012-08-18:T12:00:00.0Z/P1D``
   * - Midnight on the first of September, October, and November 1999
     - ``TIME=1999-09-01T00:00:00.0Z,1999-10-01T00:00:00.0Z,1999-11-01T00:00:00.0Z``
     - ``TIME=1999-09-01T00:00:00.0Z/1999-11-01T00:00:00.0Z/P1M``

.. note::
   
    GeoServer currently does not support lists of ranges, so all list queries effectively have a resolution of 1 millisecond.
    If you use reduced accuracy notation when specifying a range, each range will be automatically converted to the instant at the beginning of the range.
