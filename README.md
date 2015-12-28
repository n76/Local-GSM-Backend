GSM LocationProvider backend for µg UnifiedNlp
==============================================

The µg Project at https://github.com/microg has a unified network location provider that accepts backends for the actual location lookup. This project implements a cell-tower based lookup for your current location.

This "GSM Location Backend" works without network connectivity and will
never post your data anywhere.

The database needed to lookup cell tower locations resides on the phone. A facility in the setting menu allows you to create a database using data from OpenCellID and/or Mozilla Location Services CSV files.

Alternatively, the on-phone database can be generated from scripts at https://github.com/n76/lacells-creator Those scripts gather tower information from OpenCellId http://opencellid.org and Mozilla Location Services https://location.services.mozilla.com

You can improve the quality of the data used by this location provider by contributing to either or both of those projects.

This software is licensed as "Apache License, Version 2.0" unless noted
otherwise.

This software is derived from LocalGSMLocationProvider at https://github.com/rtreffer/LocalGSMLocationProvider but differs as follows:

1. This software uses a sqlite database rather than the custom built one in LocalGSMLocationProvider.
2. This software does not ship with a built in database. Databases can be generated with the scripts at https://github.com/n76/lacells-creator or using the built in database generation facility.
3. The database used by this software includes an estimated range of coverage for each cell tower. That information is used to provide a location estimate weighted by coverage area and to make an estimate of position accuracy. (The original LocalGSMLocationProvider assumes 800 meter range for all towers.)

Requirements on phone
=====================
1. This is a plug in for µg UnifiedNlp which can be installed from f-droid.

Setup on phone
==============
1. Install a cell tower database. Either use the settings menu to select the data sources and mobile country codes (mcc) that you desire or use the scripts at https://github.com/n76/lacells-creator to build and install a database.
2. In the NLP Controller app (interface for µg UnifiedNlp) select the "GSM Location Backend".

Notes on generating the database
================================
The on-phone database can be created using this settings in the app or via scripts located at https://github.com/n76/lacells-creator in either case the following applies:

1. OpenCellID requires an API key to download its CSV file and limits downloads to one per day. Information on getting an API key can be found at http://wiki.opencellid.org/wiki/How_to_join
2. Mozilla publishes new CSV files once per day.

Both of these are large files and take time to transfer and process: The current OpenCellID gzip 152MB data file contains over 6.7 million records and Mozilla’s gzip 48MB data file contains over 2.2 million records.

At least on a Google Galaxy Nexus (Maguro), download and creation of the database is much slower than using the scripts at https://github.com/n76/lacells-creator though with a decent data connection it can be done in a reasonable amount of time directly on the phone.

Filtering
=========
You are very likely to want to filter the data going into the on-phone database so that it only contains the Mobile Country Codes (MCCs) in your area of interest.

In addition to filtering by Mobile Country Code (MCCs), the on phone database generator can filter by Mobile Network Code (MNCs). This can further decrease the size of the on phone database but may decrease accuracy of the location result.

Mobile Country Codes (MCC) and Mobile Network Codes (MNC) can be found at http://en.wikipedia.org/wiki/Mobile_country_code

Update History
==============
|Version|Date|Comment|
|:-------|:----:|:-------|
0.8.0|27June2015|Serbian translation (thanks to Mladen Pejaković) and conversion to use Android Studio and Gradle for building.
0.9.0|29June2015|Add ability to acquire OpenCellID API key from within app (thanks to agilob)
0.9.1|30June2015|Improve acquisition of OpenCellID API key.
0.9.2|2Aug2015|Fix bug where on download where towers were not being inserted into database.
0.9.3|3Aug2015|Update Serbian translation.
0.9.4|13Aug2015|Slight revision to clean up logic, might help on https://github.com/n76/Local-GSM-Backend/issues/31 however the largest change is moving many text strings into string resources so that internationalization is better.
1.0.0|14Aug2015|Update Serbian translation.
1.0.1|8Oct2015|Restore compability with API 17
1.0.2|22Nov2015|Thanks to @hogbush Lots of code cleanup with better handling of UI elements.
1.0.3|22Nov2015|Thanks to @pejakm Updated Serbian translation.
1.0.4|24Nov2015|Thanks to @hogbush Support Marshmallow’s runtime permissions.
1.0.5|20Dec2015|Detect and better handle SQLite detected errors
1.0.6|21Dec2015|Revise required API to allow install on Gingerbread
1.0.7|23Dec2015|Revise target API to allow install on Gingerbread through Marshmallow
1.1.0|25Dec2015|Thanks to @UnknownUntilNow Improve database download, add German translation
1.2.0|27Dec2015|Thanks to @UnknownUntilNow Improve database download, new UI allows selection of countries by name for many countries.

