.. _crs_coordtransforms:

.. |EPSG_V| replace:: EPSG version 11.0.31

Coordinate Operations
=====================

Coordinate operations are used to convert coordinates from a `source CRS` to a `target CRS`.

If source and target CRSs are referred to a different datum, a datum transform has to be applied. Datum transforms are not exact, they are determined empirically. For the same pair of CRS, there can be many datum transforms and versions, each one with its own domain of validity and an associated transform error. Given a CRS pair, GeoServer will automatically pick the most accurate datum transform from the EPSG database, unless a custom operation is declared.

* Coordinate operations can be queried and tested using the `Reprojection Console`_.
* To enable higher accuracy Grid Shift transforms, see `Add Grid Shift Transform files`_.
* See `Define a custom Coordinate Operation`_ to declare new operations. Custom operations will take precedence over the EPSG ones.

Reprojection Console
--------------------

The reprojection console (available in :ref:`demos`) lets you quickly test coordinate operations. Use it to convert a single coordinate or WKT geometry, and to see the operation details GeoServer is using. It is also useful to learn by example when you have to `Define a custom Coordinate Operation`_.

Read more about the :ref:`demos_reprojectionconsole`.

Add Grid Shift Transform files
------------------------------

GeoServer supports NTv2 and NADCON grid shift transforms. Grid files are not shipped out with GeoServer. They need to be downloaded, usually from your National Mapping Agency website.

.. warning::

   Grid Shift files are only valid in the specific geographic domain for which they where made; trying to transform coordinates outside this domain will result in no trasformation at all. Make sure that the Grid Shift files are valid in the area you want to transform.

#. Search for the *Grid File Name(s)* int the tables below, which are extracted from |EPSG_V|. If you need to use a Grid Shift transform not declared in EPSG, you will need to `Define a custom Coordinate Operation`_.
#. Get the Grid File(s) from your National Mapping Agency (NTv2) or the `US National Geodetic Survey <http://www.ngs.noaa.gov/TOOLS/Nadcon/Nadcon.shtml>`_ (NADCON).
#. Copy the Grid File(s) in the :file:`user_projections` directory inside your data directory.
#. Use the `Reprojection Console`_ to test the new transform.


List of available Grid Shift transforms
```````````````````````````````````````

The list of Grid Shift transforms declared in |EPSG_V| is:

NTv2
....

.. csv-table::
   :header: Source CRS, Target CRS, Grid File Name, Source Info

   3906,8685,MGI1901_TO_SRBETRS89_NTv2.gsb,"Republic Geodetic Authority, Serbia; www.rgz.gov.rs"
   4122,4269,GS7783.GSB,Nova Scotia Geomatics Centre via ISO Geodetic Register.
   4122,4326,NB7783v2.gsb,OGP
   4122,4326,NS778301.gsb,OGP
   4122,4326,PE7783V2.gsb,OGP
   4122,8237,NB7783v2.gsb,New Brunswick Geographic Information Corporation land and water information standards manual.
   4122,8237,PE7783V2.gsb,PEI Department of Transportation & Public Works
   4122,8240,NS778301.gsb,Nova Scotia Geomatics Centre -  Contact aflemmin@linux1.nsgc.gov.ns.ca or telephone 902-667-6409
   4122,8252,NS778302.gsb,Nova Scotia Geomatics Centre via ISO Geodetic Register.
   4149,4150,CHENyx06a.gsb,Bundesamt für Landestopografie swisstopo; www.swisstopo.ch
   4149,4151,CHENyx06_ETRS.gsb,Bundesamt für Landestopografie swisstopo; www.swisstopo.ch
   4149,4258,CHENyx06_ETRS.gsb,Bundesamt für Landestopografie swisstopo; www.swisstopo.ch
   4149,4326,CHENyx06_ETRS.gsb,IOGP
   4170,4674,SIRGAS1995-to-SIRGAS2000.gsb,"SIRGAS Working Group I, ""Relación y modelos de transformación entre las soluciones SIRGAS95, SIRGAS2000 y SIR17P01. Resultados obtenidos"", presented at SIMPOSIO SIRGAS 2021 Perú, 29 Nov-01 Dec 2021."
   4171,4275,rgf93_ntf.gsb,ESRI
   4202,4283,A66 National (13.09.01).gsb,GDA Technical Manual. http://www.icsm.gov.au/gda
   4202,4283,SEAust_21_06_00.gsb,Office of Surveyor General Victoria; http://www.land.vic.gov.au/
   4202,4283,nt_0599.gsb,GDA Technical Manual. http://www.icsm.gov.au/gda
   4202,4283,tas_1098.gsb,http://www.delm.tas.gov.au/osg/Geodetic_transform.htm
   4202,4283,vic_0799.gsb,Office of Surveyor General Victoria; http://www.land.vic.gov.au/
   4202,4326,A66 National (13.09.01).gsb,OGP
   4203,4283,National 84 (02.07.01).gsb,GDA Technical Manual. http://www.icsm.gov.au/gda
   4203,4283,wa_0700.gsb,"Department of Land Information, Government of Western Australia; http://www.dola.wa.gov.au/"
   4203,4326,National 84 (02.07.01).gsb,OGP
   4207,4258,DLx_ETRS89_geo.gsb,Instituto Geografico Portugues; http://www.igeo.pt
   4225,4326,CA7072_003.gsb,OGP
   4225,4674,CA7072_003.gsb,IBGE.
   4230,4258,"100800401.gsb","Geodesy Unit, Cartographic Institute of Catalonia (ICC);  http://www.icc.cat"
   4230,4258,BALR2009.gsb,"National Geographic Institute of Spain (IGN), http://www.ign.es"
   4230,4258,PENR2009.gsb,"National Geographic Institute of Spain (IGN), http://www.ign.es"
   4230,4258,SPED2ETV2.gsb,"Instituto Geográfico Nacional, www.cnig.es"
   4230,4326,SPED2ETV2.gsb,OGP
   4230,4670,"35160622_47161840_E50_F89.gsb","Istituto Geografico Militare (IGM), www.igmi.org"
   4230,6706,"35160622_47161840_E50_F00.gsb","Istituto Geografico Militare (IGM), www.igmi.org"
   4237,9067,hu_bme_hd72corr.gsb,Budapest University of Technology and Economics (BME) - Faculty of Civil Engineering. https://github.com/OSGeoLabBp/eov2etrs/blob/master/etrs2eov_doc.rst
   4237,9067,hu_sgo_hd72corr.gsb,"Lechner Knowledge Center, Satellite Geodesy Observatory (SGO), https://eht2.gnssnet.hu"
   4258,4275,rgf93_ntf.gsb,OGP
   4258,9364,TN15-ETRS89-to-TPEN11-IRF.gsb,Network Rail.
   4258,9372,TN15-ETRS89-to-MML07-IRF.gsb,Network Rail.
   4258,9384,TN15-ETRS89-to-AbInvA96_2020-IRF.gsb,Transport Scotland.
   4258,9453,TN15-ETRS89-to-GBK19-IRF.gsb,Network Rail.
   4258,9739,TN15-ETRS89-to-EOS21-IRF.gsb,Network Rail.
   4258,9758,TN15-ETRS89-to-ECML14_NB-IRF.gsb,Network Rail.
   4258,9763,TN15-ETRS89-to-EWR2-IRF.gsb,Network Rail.
   4258,9866,TN15-ETRS89-to-MRH21-IRF.gsb,Network Rail.
   4258,9871,TN15-ETRS89-to-MOLDOR11-IRF.gsb,Network Rail.
   4258,9939,TN15-ETRS89-to-EBBWV14-IRF.gsb,Network Rail.
   4258,9964,TN15-ETRS89-to-HULLEE13-IRF.gsb,Network Rail.
   4258,9969,TN15-ETRS89-to-SCM22-IRF.gsb,Network Rail.
   4258,9974,TN15-ETRS89-to-FNL22-IRF.gsb,Network Rail.
   4258,10158,s34j_2022.gsb,"The Danish Agency for Data Supply and Infrastructure, https://sdfi.dk"
   4258,10175,TN15-ETRS89-to-DoPw22-IRF.gsb,"Network Rail, UK."
   4258,10185,TN15-ETRS89-to-ShAb07-IRF.gsb,"Network Rail, UK."
   4258,10191,TN15-ETRS89-to-CNH22-IRF.gsb,Network Rail.
   4258,10196,TN15-ETRS89-to-CWS13-IRF.gsb,Network Rail.
   4258,10204,TN15-ETRS89-to-DIBA15-IRF.gsb,Network Rail.
   4258,10209,TN15-ETRS89-to-GWPBS22-IRF.gsb,Network Rail.
   4258,10214,TN15-ETRS89-to-GWWAB22-IRF.gsb,Network Rail.
   4258,10219,TN15-ETRS89-to-GWWWA22-IRF.gsb,Network Rail.
   4258,10224,TN15-ETRS89-to-MALS09-IRF.gsb,Network Rail.
   4258,10229,TN15-ETRS89-to-OxWo08-IRF.gsb,Network Rail.
   4258,10237,TN15-ETRS89-to-SYC20-IRF.gsb,Network Rail.
   4258,10249,s34s_2022.gsb,"The Danish Agency for Data Supply and Infrastructure, https://sdfi.dk"
   4258,10252,s45b_2022.gsb,"The Danish Agency for Data Supply and Infrastructure, https://sdfi.dk"
   4258,10256,gs_2022.gsb,"The Danish Agency for Data Supply and Infrastructure, https://sdfi.dk"
   4258,10260,gsb_2022.gsb,"The Danish Agency for Data Supply and Infrastructure, https://sdfi.dk"
   4258,10265,kk_2022.gsb,"The Danish Agency for Data Supply and Infrastructure, https://sdfi.dk"
   4258,10268,os_2022.gsb,"The Danish Agency for Data Supply and Infrastructure, https://sdfi.dk"
   4258,10272,TN15-ETRS89-to-SMITB20-IRF.gsb,"Network Rail, UK."
   4258,10277,TN15-ETRS89-to-RBEPP12-IRF.gsb,"Network Rail, UK."
   4258,10468,TN15-ETRS89-to-COV23-IRF.gsb,Coventry City Council.
   4258,10623,TN15-ETRS89-to-ECML14-IRF.gsb,Network Rail.
   4258,10628,TN15-ETRS89-to-WC05-IRF.gsb,Network Rail.
   4258,20033,TN15-ETRS89-to-MWC18-IRF.gsb,Network Rail.
   4265,4230,"35160622_47161840_R40_E50.gsb","Istituto Geografico Militare (IGM), www.igmi.org"
   4265,4670,"35160622_47161840_R40_F89.gsb","Istituto Geografico Militare (IGM), www.igmi.org"
   4265,6706,"35160622_47161840_R40_F00.gsb","Istituto Geografico Militare (IGM), www.igmi.org"
   4267,4269,NA27NA83.GSB,Geodetic Service of Quebec.
   4267,4269,NTv2_0.gsb,"NR Canada, https://www.nrcan.gc.ca/"
   4267,4269,SK27-83.gsb,Saskatchewan Information Services Corporation (ISC) via ISO Geodetic Register.
   4267,4326,NA27SCRS.GSB,OGP
   4267,4326,NTv2_0.gsb,OGP
   4267,4326,SK27-98.gsb,OGP
   4267,8237,BC_27_98.GSB,"GeoBC, Government of British Columbia, via ISO Geodetic Register."
   4267,8237,NB2783v2.gsb,"""Generation of a NAD27-NAD83(CSRS) NTv2-type Grid Shift File for New Brunswick"", Marcelo C. Santos and Carlos A. Garcia, Department of Geodesy and Geomatics Engineering, University of New Brunswick, October, 2011 via Service New Brunswick."
   4267,8237,QUE27-98.gsb,Geodetic Service of Quebec. Contact alain.bernard@mrn.gouv.qc.ca
   4267,8237,SK27-98.gsb,Dir Geodetic Surveys; SaskGeomatics Div.; Saskatchewan Property Management Company.
   4267,8240,CRD27_00.GSB,"GeoBC, Government of British Columbia, via ISO Geodetic Register."
   4267,8240,NVI27_05.GSB,"GeoBC, Government of British Columbia, via ISO Geodetic Register."
   4267,8240,ON27CSv1.GSB,Land Information Ontario via ISO Geodetic Register.
   4267,8240,TO27CSv1.GSB,"Natural Resources Canada (NRCan), https://natural-resources.canada.ca"
   4267,8246,BC_27_05.GSB,"GeoBC, Government of British Columbia, via ISO Geodetic Register."
   4269,4326,AB_CSRS.DAC,OGP
   4269,4326,NA83SCRS.GSB,OGP
   4269,4326,SK83-98.gsb,OGP
   4269,8237,BC_93_98.GSB,"GeoBC, Government of British Columbia, via ISO Geodetic Register."
   4269,8237,NAD83-98.gsb,Geodetic Service of Quebec. Contact alain.bernard@mrn.gouv.qc.ca
   4269,8237,SK83-98.gsb,Dir Geodetic Surveys; SaskGeomatics Div.; Saskatchewan Property Management Company.
   4269,8240,CRD93_00.GSB,"GeoBC, Government of British Columbia, via ISO Geodetic Register."
   4269,8240,NVI93_05.GSB,"GeoBC, Government of British Columbia, via ISO Geodetic Register."
   4269,8240,ON83CSv1.GSB,Land Information Ontario via ISO Geodetic Register.
   4269,8246,AB_CSRS.DAC,Geodetic Control Section; Land and Forest Svc; Alberta Environment; http://www3.gov.ab.ca/env/land/dos/
   4269,8246,BC_93_05.GSB,"GeoBC, Government of British Columbia, via ISO Geodetic Register."
   4269,8252,NLCSRSV4A.GSB,"Canadian Geodetic Survey, Natural Resources Canada (NrCan)."
   4269,8255,ABCSRSV7.GSB,"NR Canada, https://webapp.geod.nrcan.gc.ca/geod/data-donnees/transformations.php"
   4272,4167,nzgd2kgrid0005.gsb,Land Information New Zealand: LINZS25000 Standard for New Zealand Geodetic Datum 2000; 16 November 2007.
   4272,4326,nzgd2kgrid0005.gsb,OGP
   4274,4258,D73_ETRS89_geo.gsb,Instituto Geografico Portugues; http://www.igeo.pt
   4277,4258,OSTN02_NTv2.gsb,"Ordnance Survey of Great Britain, http://www.gps.gov.uk"
   4277,4258,OSTN15_NTv2_OSGBtoETRS.gsb,Ordnance Survey of Great Britain.
   4277,4326,OSTN02_NTv2.gsb,OGP
   4277,4326,OSTN15_NTv2_OSGBtoETRS.gsb,EPSG
   4283,4326,GDA94_GDA2020_conformal_and_distortion.gsb,ANZLIC Intergovernmental Committee on Surveying and Mapping (ICSM) (http://www.icsm.gov.au).
   4283,7844,GDA94_GDA2020_conformal.gsb,GDA2020 Technical Manual and ICSM Datum Technical Fact Sheet TN1 (http://www.icsm.gov.au).
   4283,7844,GDA94_GDA2020_conformal_and_distortion.gsb,GDA2020 Technical Manual and ICSM Datum Technical Fact Sheet TN1 (http://www.icsm.gov.au).
   4283,7844,GDA94_GDA2020_conformal_christmas_island.gsb,GDA2020 Technical Manual (http://www.icsm.gov.au).
   4283,7844,GDA94_GDA2020_conformal_cocos_island.gsb,GDA2020 Technical Manual (http://www.icsm.gov.au)
   4289,4258,rdtrans2008.gsb,"Kadaster and Rijkswaterstaat CIV, working together under the name RDNAP."
   4289,4258,rdtrans2018.gsb,"NSGI: Netherlands partnership of Kadaster, Rijkswaterstaat and Hydrographic Service, http://www.nsgi.nl/."
   4300,4258,tm75_etrs89.gsb,ESRI Ireland.
   4300,4326,tm75_etrs89.gsb,OGP
   4301,4612,tky2jgd.gsb,ESRI
   4301,6668,tky2jgd.gsb,OGP
   4312,4258,AT_GIS_GRID.gsb,Federal Office of Metrology and Surveying (BEV); http://www.bev.gv.at
   4312,4258,AT_GIS_GRID_2021_09_28.gsb,Federal Office of Metrology and Surveying (BEV); http://www.bev.gv.at
   4313,4258,bd72lb72_etrs89lb08.gsb,IGN Brussels www.ngi.be
   4314,4258,BETA2007.gsb,BKG via EuroGeographics https://crs.bkg.bund.de/ or https://www.crs-geo.eu/
   4314,4258,BWTA2017.gsb,Landesamt für Geoinformation und Landentwicklung - Baden-Württemberg (LGL).
   4314,4258,HeTa2010.gsb,Hessisches Landesamt für Bodenmanagement und Geoinformation (HLBG).
   4314,4258,SeTa2016.gsb,"Landesamt für Vermessung, Geoinformation und Landentwicklung - Saarland (LVGL)."
   4314,4326,BETA2007.gsb,OGP
   4326,4275,rgf93_ntf.gsb,OGP
   4326,7844,GDA94_GDA2020_conformal_and_distortion.gsb,ANZLIC Intergovernmental Committee on Surveying and Mapping (ICSM) (http://www.icsm.gov.au).
   4608,4269,May76v20.gsb,Geodetic Survey of Canada  http://www.geod.nrcan.gc.ca/
   4608,4326,May76v20.gsb,OGP
   4608,8240,ON76CSv1.GSB,Land Information Ontario via ISO Geodetic Register.
   4609,4269,CQ77NA83.GSB,Geodetic Service of Quebec.
   4609,4326,CQ77NA83.GSB,OGP
   4609,8237,CGQ77-98.gsb,Geodetic Service of Quebec. Contact alain.bernard@mrn.gouv.qc.ca
   4612,4326,touhokutaiheiyouoki2011.gsb,IOGP
   4612,6668,touhokutaiheiyouoki2011.gsb,ESRI
   4618,4326,SAD69_003.gsb,OGP
   4618,4674,SAD69_003.gsb,IBGE.
   4659,8086,ISN93_ISN2016.gsb,"National Land Survey of Iceland, www.lmi.is."
   4670,6706,"35160622_47161840_F89_F00.gsb","Istituto Geografico Militare (IGM), www.igmi.org"
   4674,8987,SIRGAS2000-to-SIRGAS-CONSIR17P01.gsb,"SIRGAS Working Group I, ""Relación y modelos de transformación entre las soluciones SIRGAS95, SIRGAS2000 y SIR17P01. Resultados obtenidos"", presented at SIMPOSIO SIRGAS 2021 Perú, 29 Nov-01 Dec 2021."
   4745,4258,NTv2_SN.gsb,Saxony State Spatial Data and Land Survey Corporation (GeoSN).
   4745,4326,BETA2007.gsb,OGP
   4746,4258,de_tlbg_thuringen_NTv2gridTH.gsb,Thüringer Landesamt für Bodenmanagement und Geoinformation (TLBG).
   4746,4326,BETA2007.gsb,OGP
   4749,4644,RGNC1991_NEA74Noumea.gsb,ESRI
   4749,4662,RGNC1991_IGN72GrandeTerre.gsb,ESRI
   5324,8086,ISN2004_ISN2016.gsb,"National Land Survey of Iceland, www.lmi.is."
   5524,4326,CA61_003.gsb,OGP
   5524,4674,CA61_003.gsb,IBGE.
   5527,4326,SAD96_003.gsb,OGP
   5527,4674,SAD96_003.gsb,IBGE.
   8237,8240,CRD98_00.GSB,"GeoBC, Government of British Columbia, via ISO Geodetic Register."
   8237,8240,NVI98_05.GSB,"GeoBC, Government of British Columbia, via ISO Geodetic Register."
   8237,8246,BC_98_05.GSB,"GeoBC, Government of British Columbia, via ISO Geodetic Register."
   9299,4258,HS2TN15_NTv2.gsb,HS2 Limited.
   9777,4275,rgf93_ntf.gsb,ESRI
   9782,4275,rgf93_ntf.gsb,IOGP

.. The SQL statement::
   SELECT DISTINCT source_crs_code SOURCE_CRS, target_crs_code TARGET_CRS, val.param_value_file_ref GRID_FILE_NAME, information_source SOURCE_INFO
   FROM epsg_coordoperationparamvalue val, epsg_coordoperation op  
   WHERE val.coord_op_method_code = 9615 AND val.coord_op_code = op.coord_op_code AND op.deprecated = 0
   ORDER BY SOURCE_CRS, TARGET_CRS, GRID_FILE_NAME, SOURCE_INFO

NADCON
......

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
   8351,4156,UGKK-Svk,Slovakia_JTSK03_to_JTSK.LAS.las,Slovakia_JTSK03_to_JTSK.LAS.los
   8351,4156,UGKK-Svk,Slovakia_JTSK03_to_JTSK.LOS.las,Slovakia_JTSK03_to_JTSK.LOS.los


.. The SQL statement::
   SELECT DISTINCT source_crs_code SOURCE_CRS, target_crs_code TARGET_CRS, coord_tfm_version VERSION, REPLACE ( REPLACE (val.param_value_file_ref, '.las'), '.los') + '.las' GRID_FILE_NAME_1, REPLACE ( REPLACE (val.param_value_file_ref, '.las'), '.los') + '.los' GRID_FILE_NAME_2
   FROM epsg_coordoperationparamvalue val, epsg_coordoperation op  
   WHERE val.coord_op_method_code = 9613 AND val.coord_op_code = op.coord_op_code AND op.deprecated = 0 AND information_source != 'OGP'
   ORDER BY SOURCE_CRS, TARGET_CRS, GRID_FILE_NAME_1, GRID_FILE_NAME_2, VERSION

Define a custom Coordinate Operation
------------------------------------

Custom Coordinate Operations are defined in :file:`epsg_operations.properties` file. This file has to be placed into the :file:`user_projections` directory, inside your data directory (create it if it doesn't exist).

Each line in :file:`epsg_operations.properties` will describe a coordinate operation consisting of a `source CRS`, a `target CRS`, and a math transform with its parameter values. Use the following syntax::

  <source crs code>,<target crs code>=<WKT math transform>

Math transform is described in `Well-Known Text <http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html>`_ syntax. Parameter names and value ranges are described in the `EPSG Geodetic Parameter Registry <http://www.epsg-registry.org/>`_.

.. note::
   Use the `Reprojection Console`_ to learn from example and to test your custom definitions.

Examples
````````

Custom NTv2 file::

  4230,4258=PARAM_MT["NTv2", \
    PARAMETER["Latitude and longitude difference file", "100800401.gsb"]]

Geocentric transformation, preceded by an ellipsoid to geocentric conversion, and back geocentric to ellipsoid. The results is a concatenation of three math transforms::

  4230,4258=CONCAT_MT[ \
    PARAM_MT["Ellipsoid_To_Geocentric", \
      PARAMETER["dim", 2], \
      PARAMETER["semi_major", 6378388.0], \
      PARAMETER["semi_minor", 6356911.9461279465]], \
    PARAM_MT["Position Vector transformation (geog2D domain)", \
      PARAMETER["dx", -116.641], \
      PARAMETER["dy", -56.931], \
      PARAMETER["dz", -110.559], \
      PARAMETER["ex", 0.8925078166311858], \
      PARAMETER["ey", 0.9207660950870382], \
      PARAMETER["ez", -0.9166407989620964], \
      PARAMETER["ppm", -3.5200000000346066]], \
    PARAM_MT["Geocentric_To_Ellipsoid", \
      PARAMETER["dim", 2], \
      PARAMETER["semi_major", 6378137.0], \
      PARAMETER["semi_minor", 6356752.314140356]]]

You can make use of existing grid shift files such as this explicit transformation from NAD27 to WGS84 made up of a NADCON transform from NAD27 to NAD83 followed by a Molodenski transform converting from the GRS80 Ellipsoid (used by NAD83) to the WGS84 Ellipsoid::

    4267,4326=CONCAT_MT[ \
      PARAM_MT["NADCON", \
        PARAMETER["Latitude difference file", "conus.las"], \
        PARAMETER["Longitude difference file", "conus.los"]], \
      PARAM_MT["Molodenski", \
        PARAMETER["dim", 2], \
        PARAMETER["dx", 0.0], \
        PARAMETER["dy", 0.0], \
        PARAMETER["dz", 0.0], \
        PARAMETER["src_semi_major", 6378137.0], \
        PARAMETER["src_semi_minor", 6356752.314140356], \
        PARAMETER["tgt_semi_major", 6378137.0], \
        PARAMETER["tgt_semi_minor", 6356752.314245179]]]

Affine 2D transform operating directly in projected coordinates::

  23031,25831=PARAM_MT["Affine", \
    PARAMETER["num_row", 3], \
    PARAMETER["num_col", 3], \
    PARAMETER["elt_0_0", 1.0000015503712145], \
    PARAMETER["elt_0_1", 0.00000758753979846734], \
    PARAMETER["elt_0_2", -129.549], \
    PARAMETER["elt_1_0", -0.00000758753979846734], \
    PARAMETER["elt_1_1", 1.0000015503712145], \
    PARAMETER["elt_1_2", -208.185]]
    
Each operation can be described in a single line, or can be split in several lines for readability, adding a backslash "\\" at the end of each line, as in the former examples.
