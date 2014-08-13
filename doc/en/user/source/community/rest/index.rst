.. _community_rest:

REST PathMapper Plugin
======================

This community module provides a new **RESTUploadPathMapper** implementation supporting ECQL expression for remapping the input file. 
A new Panel inside the Global and WorkSpace Settings will be added:

	.. figure:: images/settings.png
	
	
For more informations about remapping files with REST the user can look at the following :ref:`REST` page.
	
ECQL expression configuration
-----------------------------

The user can simply remap the input file by simply writing an ECQL expression. This expression must follow some rules:

	* The expression can only use the following parameters:
	
		#. `path`: which contains the relative path of the input file, useful when the file is inside a zip structure.
		#. `name`: which contains the file name.
		
	* The expression must return a new file path, not a directory::
	
		[/<newpath>]/<file>
		
Examples
--------

#. Regular Expression:

		Input File name: *NCOM_wattemp_020_20081031T0000000_12.tiff*

		ECQL Regular Expression:: 
		
			stringTemplate(path, '(\\w{4})_(\\w{7})_(\\d{3})_(\\d{4})(\\d{2})(\\d{2})T(\\d{7})_(\\d{2})\\.(\\w{4})', '/${1}/${4}/${5}/${6}/${0}')

		Result: *NCOM/2008/10/31/NCOM_wattemp_020_20081031T0000000_12.tiff*
	
#. Substring:

		Input File name: *NCOM_wattemp_020_20081031T0000000_12.tiff*

		ECQL Regular Expression:: 
		
			Concatenate(strSubstring(path, 0, 4),'/',name)

		Result: *NCOM/NCOM_wattemp_020_20081031T0000000_12.tiff*
	


	