.. _crs_gridshift:

.. |EPSG_V| replace:: EPSG version 7.9.0

Using Grid Shift Transformations
================================

GeoServer supports high precision coordinate transformations via NTv2 and NADCON grid shift files.

These files are not shipped out with GeoServer. They need to be downloaded, usually from yor National Mapping Agency website.

#. Locate the *Grid File Name(s)* from the tables below.
#. Get the Grid File(s) from your National Mapping Agency (NTv2) or the `US National Geodetic Survey <http://www.ngs.noaa.gov/TOOLS/Nadcon/Nadcon.shtml>`_ (NADCON).
#. Copy the Grid File(s) in the :file:`user_projections` directory inside your data directory.

.. Warning::

   Grid Shift files are only valid in the geographic domain for which they where made; trying to transform coordinates outside this domain will result in no trasformation at all. Make sure which area is covered by your Grid Shift files.


List of available Grid Shift transformations
--------------------------------------------

The list of supported Grid Shift transformations is determined by the EPSG database. In |EPSG_V|, the available transformations are:


NTv2
````

.. csv-table::
   :header: Source CRS, Target CRS, Grid File Name, Source Info

   4122,4326,NB7783v2.gsb,OGP
   4122,4326,NS778301.gsb,OGP
   4122,4326,PE7783V2.gsb,OGP
   4122,4617,NB7783v2.gsb,New Brunswick Geographic Information Corporation land and water information standards manual.
   4122,4617,NS778301.gsb,Nova Scotia Geomatics Centre -  Contact aflemmin@linux1.nsgc.gov.ns.ca or telephone 902-667-6409
   4122,4617,PE7783V2.gsb,PEI Department of Transportation & Public Works
   4149,4150,CHENYX06.gsb,Bundesamt für Landestopographie; www.swisstopo.ch
   4171,4275,rgf93_ntf.gsb,ESRI
   4202,4283,A66 National (13.09.01).gsb,GDA Technical Manual. http://www.icsm.gov.au/gda
   4202,4283,SEAust_21_06_00.gsb,Office of Surveyor General Victoria; http://www.land.vic.gov.au/
   4202,4283,nt_0599.gsb,GDA Technical Manual. http://www.icsm.gov.au/gda
   4202,4283,tas_1098.gsb,http://www.delm.tas.gov.au/osg/Geodetic_transform.htm
   4202,4283,vic_0799.gsb,Office of Surveyor General Victoria; http://www.land.vic.gov.au/
   4202,4326,A66 National (13.09.01).gsb,OGP
   4203,4283,National 84 (02.07.01).gsb,GDA Technical Manual. http://www.icsm.gov.au/gda
   4203,4283,wa_0400.gsb,http://www.dola.wa.gov.au/lotl/survey_geodesy/gda1994/download.html
   4203,4283,wa_0700.gsb,"Department of Land Information, Government of Western Australia; http://www.dola.wa.gov.au/"
   4203,4326,National 84 (02.07.01).gsb,OGP
   4225,4326,CA7072_003.gsb,OGP
   4225,4674,CA7072_003.gsb,IBGE.
   4230,4258,SPED2ETV2.gsb,"Instituto Geográfico Nacional, www.cnig.es"
   4230,4258,sped2et.gsb,"Instituto Geográfico Nacional, www.cnig.es"
   4230,4326,SPED2ETV2.gsb,OGP
   4230,4326,sped2et.gsb,OGP
   4258,4275,rgf93_ntf.gsb,OGP
   4267,4269,NTv2_0.gsb,http://www.geod.nrcan.gc.ca/products/html-public/GSDapps/English/NTv2_Fact_Sheet.html
   4267,4269,QUE27-83.gsb,Geodetic Service of Quebec. Contact alain.bernard@mrn.gouv.qc.ca
   4267,4326,NTv2_0.gsb,OGP
   4267,4326,QUE27-98.gsb,OGP
   4267,4326,SK27-98.gsb,OGP
   4267,4617,QUE27-98.gsb,Geodetic Service of Quebec. Contact alain.bernard@mrn.gouv.qc.ca
   4267,4617,SK27-98.gsb,Dir Geodetic Surveys; SaskGeomatics Div.; Saskatchewan Property Management Company.
   4269,4326,AB_CSRS.DAC,OGP
   4269,4326,NAD83-98.gsb,OGP
   4269,4326,SK83-98.gsb,OGP
   4269,4617,AB_CSRS.DAC,Geodetic Control Section; Land and Forest Svc; Alberta Environment; http://www3.gov.ab.ca/env/land/dos/
   4269,4617,NAD83-98.gsb,Geodetic Service of Quebec. Contact alain.bernard@mrn.gouv.qc.ca
   4269,4617,SK83-98.gsb,Dir Geodetic Surveys; SaskGeomatics Div.; Saskatchewan Property Management Company.
   4272,4167,nzgd2kgrid0005.gsb,Land Information New Zealand: LINZS25000 Standard for New Zealand Geodetic Datum 2000; 16 November 2007.
   4272,4326,nzgd2kgrid0005.gsb,OGP
   4277,4258,OSTN02_NTv2.gsb,"Ordnance Survey of Great Britain, http://www.gps.gov.uk"
   4277,4326,OSTN02_NTv2.gsb,OGP
   4314,4258,BETA2007.gsb,BKG via EuroGeographics http://crs.bkg.bund.de/crs-eu/
   4314,4326,BETA2007.gsb,OGP
   4326,4275,rgf93_ntf.gsb,OGP
   4608,4269,May76v20.gsb,Geodetic Survey of Canada  http://www.geod.nrcan.gc.ca/
   4608,4326,May76v20.gsb,OGP
   4609,4269,CGQ77-83.gsb,Geodetic Service of Quebec. Contact alain.bernard@mrn.gouv.qc.ca
   4609,4326,CGQ77-98.gsb,OGP
   4609,4617,CGQ77-98.gsb,Geodetic Service of Quebec. Contact alain.bernard@mrn.gouv.qc.ca
   4618,4326,SAD69_003.gsb,OGP
   4618,4674,SAD69_003.gsb,IBGE.
   4745,4326,BETA2007.gsb,OGP
   4746,4326,BETA2007.gsb,OGP
   4749,4644,RGNC1991_NEA74Noumea.gsb,ESRI
   4749,4662,RGNC1991_IGN72GrandeTerre.gsb,ESRI
   5524,4326,CA61_003.gsb,OGP
   5524,4674,CA61_003.gsb,IBGE.
   5527,4326,SAD96_003.gsb,OGP
   5527,4674,SAD96_003.gsb,IBGE.

.. The SQL statement::
   SELECT DISTINCT source_crs_code SOURCE_CRS, target_crs_code TARGET_CRS, val.param_value_file_ref GRID_FILE_NAME, information_source SOURCE_INFO
   FROM epsg_coordoperationparamvalue val, epsg_coordoperation op  
   WHERE val.coord_op_method_code = 9615 AND val.coord_op_code = op.coord_op_code AND op.deprecated = 0
   ORDER BY SOURCE_CRS, TARGET_CRS, GRID_FILE_NAME, SOURCE_INFO


NADCON
``````

.. csv-table::
   :header: Source CRS, Target CRS, Version, Latitude shift file, Longitude shift file

   4135,4269,NGS-Usa HI,hawaii.las,hawaii.los
   4136,4269,NGS-Usa AK StL,stlrnc.las,stlrnc.los
   4137,4269,NGS-Usa AK StP,stpaul.las,stpaul.los
   4138,4269,NGS-Usa AK StG,stgeorge.las,stgeorge.los
   4139,4269,NGS-PRVI,prvi.las,prvi.los
   4169,4152,NGS-Asm E,eshpgn.las,eshpgn.los
   4169,4152,NGS-Asm W,wshpgn.las,wshpgn.los
   4267,4269,NGS-Usa AK,alaska.las,alaska.los
   4267,4269,NGS-Usa Conus,conus.las,conus.los
   4269,4152,NGS-Usa AL,alhpgn.las,alhpgn.los
   4269,4152,NGS-Usa AR,arhpgn.las,arhpgn.los
   4269,4152,NGS-Usa AZ,azhpgn.las,azhpgn.los
   4269,4152,NGS-Usa CA n,cnhpgn.las,cnhpgn.los
   4269,4152,NGS-Usa CO,cohpgn.las,cohpgn.los
   4269,4152,NGS-Usa CA s,cshpgn.las,cshpgn.los
   4269,4152,NGS-Usa ID MT e,emhpgn.las,emhpgn.los
   4269,4152,NGS-Usa TX e,ethpgn.las,ethpgn.los
   4269,4152,NGS-Usa FL,flhpgn.las,flhpgn.los
   4269,4152,NGS-Usa GA,gahpgn.las,gahpgn.los
   4269,4152,NGS-Usa HI,hihpgn.las,hihpgn.los
   4269,4152,NGS-Usa IA,iahpgn.las,iahpgn.los
   4269,4152,NGS-Usa IL,ilhpgn.las,ilhpgn.los
   4269,4152,NGS-Usa IN,inhpgn.las,inhpgn.los
   4269,4152,NGS-Usa KS,kshpgn.las,kshpgn.los
   4269,4152,NGS-Usa KY,kyhpgn.las,kyhpgn.los
   4269,4152,NGS-Usa LA,lahpgn.las,lahpgn.los
   4269,4152,NGS-Usa DE MD,mdhpgn.las,mdhpgn.los
   4269,4152,NGS-Usa ME,mehpgn.las,mehpgn.los
   4269,4152,NGS-Usa MI,mihpgn.las,mihpgn.los
   4269,4152,NGS-Usa MN,mnhpgn.las,mnhpgn.los
   4269,4152,NGS-Usa MO,mohpgn.las,mohpgn.los
   4269,4152,NGS-Usa MS,mshpgn.las,mshpgn.los
   4269,4152,NGS-Usa NE,nbhpgn.las,nbhpgn.los
   4269,4152,NGS-Usa NC,nchpgn.las,nchpgn.los
   4269,4152,NGS-Usa ND,ndhpgn.las,ndhpgn.los
   4269,4152,NGS-Usa NewEng,nehpgn.las,nehpgn.los
   4269,4152,NGS-Usa NJ,njhpgn.las,njhpgn.los
   4269,4152,NGS-Usa NM,nmhpgn.las,nmhpgn.los
   4269,4152,NGS-Usa NV,nvhpgn.las,nvhpgn.los
   4269,4152,NGS-Usa NY,nyhpgn.las,nyhpgn.los
   4269,4152,NGS-Usa OH,ohhpgn.las,ohhpgn.los
   4269,4152,NGS-Usa OK,okhpgn.las,okhpgn.los
   4269,4152,NGS-Usa PA,pahpgn.las,pahpgn.los
   4269,4152,NGS-PRVI,pvhpgn.las,pvhpgn.los
   4269,4152,NGS-Usa SC,schpgn.las,schpgn.los
   4269,4152,NGS-Usa SD,sdhpgn.las,sdhpgn.los
   4269,4152,NGS-Usa TN,tnhpgn.las,tnhpgn.los
   4269,4152,NGS-Usa UT,uthpgn.las,uthpgn.los
   4269,4152,NGS-Usa VA,vahpgn.las,vahpgn.los
   4269,4152,NGS-Usa WI,wihpgn.las,wihpgn.los
   4269,4152,NGS-Usa ID MT w,wmhpgn.las,wmhpgn.los
   4269,4152,NGS-Usa OR WA,wohpgn.las,wohpgn.los
   4269,4152,NGS-Usa TX w,wthpgn.las,wthpgn.los
   4269,4152,NGS-Usa WV,wvhpgn.las,wvhpgn.los
   4269,4152,NGS-Usa WY,wyhpgn.las,wyhpgn.los
   4675,4152,NGS-Gum,guhpgn.las,guhpgn.los


.. The SQL statement::
   SELECT DISTINCT source_crs_code SOURCE_CRS, target_crs_code TARGET_CRS, coord_tfm_version VERSION, REPLACE ( REPLACE (val.param_value_file_ref, '.las'), '.los') + '.las' GRID_FILE_NAME_1, REPLACE ( REPLACE (val.param_value_file_ref, '.las'), '.los') + '.los' GRID_FILE_NAME_2
   FROM epsg_coordoperationparamvalue val, epsg_coordoperation op  
   WHERE val.coord_op_method_code = 9613 AND val.coord_op_code = op.coord_op_code AND op.deprecated = 0 AND information_source != 'OGP'
   ORDER BY SOURCE_CRS, TARGET_CRS, GRID_FILE_NAME_1, GRID_FILE_NAME_2, VERSION

