# EPSG Geodetic Parameter Dataset Terms of Use

1. In this document the following definitions of terms apply:
   
   “Registry” means the EPSG Geodetic Parameter Registry;“EPSG Dataset” means EPSG Geodetic Parameter Dataset;“IOGP” means the International Association of Oil and Gas Producers, incorporated in England as a company limited by guarantee (number 1832064);“EPSG Facilities” means the Registry, the EPSG Dataset (published through the Registry or through a downloadable MS-Access file or through a set of SQL scripts that enable a user to create an Oracle, MySQL, PostgreSQL or other database and populate that database with the EPSG Dataset) and associated documentation consisting of the Release Notes and Guidance Notes 7.1 and 7.2
   
   “the data” means the geodetic parameter data and associated metadata, contained in the EPSG Dataset; it also refers to any subset of data from the EPSG Dataset.
   
2. The EPSG Facilities are published by IOGP at no charge. Distribution for profit is forbidden.
   
3. The EPSG Facilities are owned by IOGP. They are compiled by the Geodetic Subcommittee of the IOGP from publicly available and member-supplied information.

4. In order to use the EPSG Facilities, you must agree to these Terms of Use. You may not use the EPSG Facilities or any of them in whole or in part unless you agree to these Terms of Use.

5. You can accept these Terms of Use by clicking the command button ‘Accept Terms’ upon registering as a new user. You will also be required to accept any revised Terms of Use prior to using or downloading any EPSG Facilities. You understand and agree that any use of the EPSG Facilities or any of them, even if obtained without clicking acceptance, will be acceptance of these Terms of Use.

6. The data may be used, copied and distributed subject to the following conditions:
   
   1. Whilst every effort has been made to ensure the accuracy of the information contained in the EPSG Facilities, neither the IOGP nor any of its members past present or future warrants their accuracy or will, regardless of its or their negligence, assume liability for any foreseeable or unforeseeable use made thereof, which liability is hereby excluded. Consequently, such use is at your own risk. You are obliged to inform anyone to whom you provide the EPSG Facilities of these Terms of Use.

   2. DATA AND INFORMATION PROVIDED IN THE EPSG FACILITIES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE.

   3. The data may be included in any commercial package provided that any commerciality is based on value added by the provider and not on a value ascribed to the EPSG Dataset which is made available at no charge.

   4. Ownership of the EPSG Dataset by IOGP must be acknowledged in any publication or transmission (by whatever means) thereof (including permitted modifications).

   5. Subsets of information may be extracted from the dataset. Users are advised that coordinate reference system and coordinate transformation descriptions are incomplete unless all elements detailed as essential in IOGP Surveying and Positioning Guidance Note 7-1 Annex A are included.

   6. Essential elements should preferably be reproduced as described in the dataset. Modification of parameter values is permitted as described in the table below to allow change to the content of the information provided that numeric equivalence is achieved. Numeric equivalence refers to the results of geodetic calculations in which the parameters are used, for example (i) conversion of ellipsoid defining parameters, or (ii) conversion of parameters between one and two standard parallel projection methods, or (iii) conversion of parameters between 7-parameter geocentric transformation methods.

   7. No data that has been modified other than as permitted in these Terms of Use shall be attributed to the EPSG Dataset.

## Table 1: permitted modifications of data

| AS GIVEN IN EPSG DATASET || PERMITTED CHANGE FOR VENDORS/USERS TO ADOPT |
|-||-|
| *Change of ellipsoid defining parameters.* |||
| 1a | Ellipsoid parameters a and b. | a and 1/f ; a and f; a and e; a and e2. |
| 1b | Ellipsoid parameters a and 1/f. | a and b; a and f; a and e; a and e2. |
| *Change of projection method* |||
| 2a | Lambert Conic Conformal (1 SP) method with projection parameters φO and kO. | Lambert Conic Conformal (2 SP) method with projection parameters φ1 and φ2. |
| 2b | Lambert Conic Conformal (2 SP) method with projection parametersφ1 and φ2. | Lambert Conic Conformal (1 SP) method with projection parameters φO and kO. |
| 3a | Mercator (variant A) method with projection parameters φO and kO. | Mercator (variant B) method with projection parameter φ1. |
| 3b | Mercator (variant B) method with projection parameter φ1. | Mercator (variant A) method with projection parameters φO and kO. |
| 4a | Hotine Oblique Mercator (variant A) method with projection parameters FE and FN. | Hotine Oblique Mercator (variant B) method with projection parameters EC and NC. |
| 4b | Hotine Oblique Mercator (variant B) method with projection parameters EC and NC. | Hotine Oblique Mercator (variant A) method with projection parameters FE and FN. |
| 5a | Polar Stereographic (Variant A) method with projection parameters φO and kO. | Polar Stereographic (Variant B) method with projection parameter φF. |
| 5b | Polar Stereographic (Variant B) method with projection parameter φF. | Polar Stereographic (Variant A) method with projection parameters φO and kO. |
| 5c | Polar Stereographic (Variant A) method with projection parameters φO, kO, FE and FN. | Polar Stereographic (Variant C) method with projection parameters φF, EF and NF. |
| 5d | Polar Stereographic (Variant C) method with projection parameters φF, EF and NF. | Polar Stereographic (Variant A) method with projection parameters φO, kO, FE and FN. |
| 5e | Polar Stereographic (Variant B) method with projection parameter FE and FN. | Polar Stereographic (Variant C) method with projection parameters EF and NF. |
| 5f | Polar Stereographic (Variant C) method with projection parameters EF and NF. | Polar Stereographic (Variant B) method with projection parameter FE and FN. |
| Change of transformation method |  |  |
| 6a | Position Vector 7-parameter transformation method parameters RX RY and RZ. | Coordinate Frame transformation method with signs of position vector parameters RX RY and RZ reversed. |
| 6b | Coordinate Frame transformation method parameters RX RY and RZ. | Position Vector 7-parameter transformation method with signs of coordinate frame parameters RX RY and RZ reversed. |
| 7 | Concatenated transformation using geocentric methods (Geocentric translations, Position Vector 7-parameter transformation, Coordinate Frame rotation). | Equivalent single geocentric transformation in which for each parameter the parameter values of the component steps have been summed. |
| *Change of units* |||
| 8 | NTv2 method grid file filename. | NTv2 method grid file relative storage path with file name including removal (if necessary) of “special characters” [spaces, parentheses, etc] which are replaced by underscore characters. |
| 9 | Parameter value. | Convert unit to another, for example from microradian to arc-second, using conversion factors obtained from the EPSG dataset Unit table. |

<cite>source: [https://epsg.org/terms-of-use.html](https://epsg.org/terms-of-use.html)</cite>