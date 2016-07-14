GSM LocationProvider backend for µg UnifiedNlp
==============================================

The µg Project at https://github.com/microg has a unified network location provider that accepts backends for the actual location lookup. This project implements a cell-tower based lookup for your current location.

This "GSM Location Backend" works without network connectivity and will
never post your data anywhere.

The database needed to lookup cell tower locations resides on the phone. A facility in the setting menu allows you to create a database using data from OpenCellID and/or Mozilla Location Services CSV files.

Alternatively, the on-phone database can be generated from scripts at https://github.com/n76/lacells-creator Those scripts gather tower information from OpenCellId http://opencellid.org and Mozilla Location Services https://location.services.mozilla.com

You can improve the quality of the data used by this location provider by contributing to either or both of those projects.

See http://opencellid.org for information about the OpenCellID project and see https://location.services.mozilla.com for information about the Mozilla Location Services initiative.

This software is licensed as "Apache License, Version 2.0" unless noted
otherwise.

This software was forked from LocalGSMLocationProvider at https://github.com/rtreffer/LocalGSMLocationProvider with the following initial changes:

1. This software uses a sqlite database rather than the custom built one in LocalGSMLocationProvider.
2. This software does not ship with a built in database. Databases can be generated with the scripts at https://github.com/n76/lacells-creator or using the built in database generation facility.
3. The database used by this software includes an estimated range of coverage for each cell tower. That information is used to provide a location estimate weighted by coverage area and to make an estimate of position accuracy. (The original LocalGSMLocationProvider assumes 800 meter range for all towers.)

The code has evolved significantly since the initial fork.

[![Get it on F-Droid](get_it_on_f-droid.png?raw=true)](https://f-droid.org/repository/browse/?fdid=org.fitchfamily.android.gsmlocation)

Requirements on phone
=====================
1. This is a plug in for µg UnifiedNlp which can be installed from f-droid.

Setup on phone
==============
1. Install a cell tower database. Either use the settings menu to select the data sources and mobile country codes (mcc) that you desire or use the scripts at https://github.com/n76/lacells-creator to build and install a database.
2. In the NLP Controller app (interface for µg UnifiedNlp) select the "GSM Location Backend".
3. Optionally, and highly discouraged, you can set the directory path used by the backend for database operations. The only reason for this option is to make some scripted operations involving off line generated databases easier. If you are using on phone generated data you should leave this setting alone.

Notes on generating the database
================================
The on-phone database can be created using this settings in the app or via scripts located at https://github.com/n76/lacells-creator in either case the following applies:

1. OpenCellID requires an API key to download its CSV file and limits downloads to one per day. Information on getting an API key can be found at http://wiki.opencellid.org/wiki/How_to_join
2. Mozilla publishes new CSV files once per day.

At least on a Google Galaxy Nexus (Maguro), download and creation of the database is much slower than using the scripts at https://github.com/n76/lacells-creator though with a good data connection it can be done in a reasonable amount of time directly on the phone.

If the database is generated using off phone scripts, then:

1. In the advanced settings area you may wish to change the location of the directory that the backend uses to store databases to one that you find easy to download to.
2. The database you push to the phone should be named lacells.db.new The backend will detect a new database file, cleanly close the old file, rename the old file to lacells.db.bak, rename the new file to lacells.db and open it. If a failure occurs, the backend will revert to the lacells.db.bak version of the database.

Filtering
=========
You are very likely to want to filter the data going into the on-phone database so that it only contains the Mobile Country Codes (MCCs) in your area of interest. As of version 1.2 of this plug-in, it is now possible to select the Mobile Country Codes for a country by country name. The advanced setup still allows for individual codes to be selected.

In addition to filtering by Mobile Country Code (MCCs), the on phone database generator can filter by Mobile Network Code (MNCs). This can further decrease the size of the on phone database but may decrease accuracy of the location result.

Mobile Country Codes (MCC) and Mobile Network Codes (MNC) can be found at http://en.wikipedia.org/wiki/Mobile_country_code

Pre-Filtered Data
=================
Downloading from Mozilla and/or OpenCellID will get you the absolutely latest data but at the cost of time and a large data usage on the phone.

@wvengen has created a repository of per-MCC data for many but not all MCC codes at https://github.com/wvengen/lacells which is updated periodically. Starting with v1.3, this backend is capable of loading cell information from that data set which should be faster.

Permisssions
============
|Permission|Use|
|:----------|:---|
ACCESS_COARSE_LOCATION|Allows backend to determine which cell towers your phone detects.
INTERNET|Allows database creator to pull cell tower data from OpenCellID, Mozilla, etc.
WAKE_LOCK|Allows database generation service to run without being put to sleep.
WRITE_EXTERNAL_STORAGE|Allows database to reside on external storage.

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
1.2.1|28Dec2015|Correct report on processing time per record.
1.3.0|28Dec2015|Thanks to @UnknownUntilNow Add import of cell tower data from per MCC extracts provided by @wvengen
1.3.1|29Dec2015|Fix bug on downloading small files from lacells.
1.3.2|29Dec2015|Improve download of lacells MCC extract files.
1.3.3|29Dec2015|Move database to reduce permissions needed.
1.3.4|2Jan2016|Update Serbian translation
1.3.5|14Jan2016|Remove all access and permissions required for access to old database storage location. If upgrading from a version prior to 1.3.3 you will need to re-generate your database and you should manually remove the old database from the /sdcard/.nogapps/ directory.
1.3.6|23Mar2016|Support location aging in newest version of UnifiedNlp
1.3.7|30Mar2016|Revert changes from 1.3.4 to 1.3.5 as some users are reporting file permission issues. Fix crash introduced in v1.3.6.
1.4.0|4May2016|Support Marshmallow (Android 6.x) permissions.
1.4.1|6May2016|Remove confusion about mobile country code 505 Australia and Norfolk Island in favor of Australia.
1.4.2|19May2016|Only report new samples, not old ones with old times, to reduce debug logging in UnifiedNLP.
1.4.3|22May2016|When asked for update, report current position (based on most recent changes reported by TelephonyManager) with current time.
1.4.4|23May2016|Allow use of user defined database directory location. Fix negative progress indicator for large downloads.
1.4.6|14July2016|Trigger scan for mobile/cell towers when location update requested by UnifiedNLP

