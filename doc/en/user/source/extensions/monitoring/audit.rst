.. _monitor_audit:

Audit Logging 
=============

The history mode logs all requests into a database. This can put a very significant strain
on the database and can lead to insertion issues as the request table begins to host
millions of records.

As an alternative to the history mode it's possible to enable the auditing logger, which will log 
the details of each request in a file, which is periodically rolled. Secondary applications can
then process these log files and built ad-hoc summaries off line.

Configuration
-------------

The ``monitor.properties`` file can contain the following items to enable and configure file auditing::

   audit.enabled=true
   audit.path=/path/to/the/logs/directory
   audit.roll_limit=20

The ``audit.enable`` is used to turn on the logger (it is off by default).
The ``audit.path`` is the directory where the log files will be created.
The ``audit.roll_limit`` is the number of requests logged into a file before rolling happens. 
The files are also automatically rolled at the beginning of each day.

In clustered installations with a shared data directory the audit path will need to be different
for each node. In this case it's possible to specify the audit path by using a JVM system variable,
add the following to the JVM startup options and it will override whatever is specified in 
``monitor.properties``:

  -DGEOSERVER_AUDIT_PATH=/path/to/the/logs/directory

Log Files
---------

The log directory will contain a number of log files following the ``geoserver_audit_yyyymmdd_nn.log`` 
pattern. The ``nn`` is increased at each roll of the file. The contents of the log directory will look like::

  	geoserver_audit_20110811_2.log
	geoserver_audit_20110811_3.log
	geoserver_audit_20110811_4.log
	geoserver_audit_20110811_5.log
	geoserver_audit_20110811_6.log
	geoserver_audit_20110811_7.log
	geoserver_audit_20110811_8.log
	
By default each log file contents will be a xml document looking like the following::
  
	<?xml version="1.0" encoding="UTF-8" ?>
	<Requests>
		<Request id="168">
		   <Service>WMS</Service> 
		   <Version>1.1.1</Version>
		   <Operation>GetMap</Operation> 
		   <SubOperation></SubOperation>
		   <Resources>GeoSolutions:elba-deparea</Resources>
		   <ResourcesProcessingTime>4</ResourcesProcessingTime>
		   <LabelsProcessingTime>0</LabelsProcessingTime>
		   <Path>/GeoSolutions/wms</Path>
		   <QueryString>LAYERS=GeoSolutions:elba-deparea&amp;STYLES=&amp;FORMAT=image/png&amp;TILED=true&amp;TILESORIGIN=9.916,42.312&amp;SERVICE=WMS&amp;VERSION=1.1.1&amp;REQUEST=GetMap&amp;EXCEPTIONS=application/vnd.ogc.se_inimage&amp;SRS=EPSG:4326&amp;BBOX=9.58375,42.64425,9.916,42.9765&amp;WIDTH=256&amp;HEIGHT=256</QueryString>
		   <HttpMethod>GET</HttpMethod>
		   <StartTime>2011-08-11T20:19:28.277Z</StartTime> 
		   <EndTime>2011-08-11T20:19:28.29Z</EndTime>
		   <TotalTime>13</TotalTime> 
		   <RemoteAddr>192.168.1.5</RemoteAddr>
		   <RemoteHost>192.168.1.5</RemoteHost>
		   <Host>demo1.geo-solutions.it</Host> 
		   <RemoteUser>admin</RemoteUser>
		   <ResponseStatus>200</ResponseStatus>
		   <ResponseLength>1670</ResponseLength>
		   <ResponseContentType>image/png</ResponseContentType>
		   <Failed>false</Failed>
		</Request>
		...
	</Requests>

Customizing Log Contents
------------------------

The log contents are driven by three FreeMarker templates. 

``header.ftl`` is used once when a new log file is created to form the first few lines of the file. 
The default header template is::

	<?xml version="1.0" encoding="UTF-8" ?>
	<Requests>
	
``content.ftl`` is used to write out the request details. The default template dumps all the known fields about the request::

	<#escape x as x?xml>
	<Request id="${id!""}">
	   <Service>${service!""}</Service> 
	   <Version>${owsVersion!""}</Version>
	   <Operation>${operation!""}</Operation> 
	   <SubOperation>${subOperation!""}</SubOperation>
	   <Resources>${resourcesList!""}</Resources>
	   <ResourcesProcessingTime>${resourcesProcessingTimeList!""}</ResourcesProcessingTime>
	   <LabelsProcessingTime>${labellingProcessingTime!""}</LabelsProcessingTime>
	   <Path>${path!""}</Path>
	   <QueryString>${queryString!""}</QueryString>
	   <#if bodyAsString??>
	   <Body>
	   ${bodyAsString}
	   </Body>
	   </#if>
	   <HttpMethod>${httpMethod!""}</HttpMethod>
	   <StartTime>${startTime?datetime?iso_utc_ms}</StartTime> 
	   <EndTime>${endTime?datetime?iso_utc_ms}</EndTime>
	   <TotalTime>${totalTime}</TotalTime> 
	   <RemoteAddr>${remoteAddr!""}</RemoteAddr>
	   <RemoteHost>${remoteHost!""}</RemoteHost>
	   <Host>${host}</Host> 
	   <RemoteUser>${remoteUser!""}</RemoteUser>
	   <ResponseStatus>${responseStatus!""}</ResponseStatus>
	   <ResponseLength>${responseLength?c}</ResponseLength>
	   <ResponseContentType>${responseContentType!""}</ResponseContentType>
	   <CacheResult>${cacheResult!""}</CacheResult>
	   <MissReason>${missReason!""}</MissReason>
	   <#if error??>
	   <Failed>true</Failed>
	   <ErrorMessage>${errorMessage!""}</ErrorMessage>
	   <#else>
	   <Failed>false</Failed>
	   </#if>
	</Request>
	</#escape>
    

``footer.ftl`` is executed just once when the log file is closed to build the last few lines of the file.
The default footer template is::

	</Requests>
	
The administrator is free to provide alternate templates, they can be placed in the same directory
as ``monitor.properties``, with the same names as above. GeoServer will pick them up automatically.