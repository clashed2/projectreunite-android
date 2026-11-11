PROJECT REUNITE - LIVE DATABASE/API SETUP

The Android app is local, but Login/Register/Reports use the online PHP API.

1. Upload these four PHP files to your ByetHost public_html folder:
   api_login.php
   api_register.php
   api_reports.php
   db.php

2. Open ApiClient.java and replace:
   https://YOUR-BYETHOST-DOMAIN/
   with the exact public HTTPS URL of the folder containing the PHP files.

   Example:
   https://your-subdomain.byethost32.com/

3. The app calls:
   /api_login.php
   /api_register.php
   /api_reports.php

4. The database connection in db.php already points to the remote MySQL server.

5. Do NOT put localhost in ApiClient.java.

IMPORTANT:
The SQL schema has a report image column named `photo / image`. api_reports.php was adjusted to use that exact existing column name.

The current report schema does not have dedicated columns for item name/description/report type, so the app sends the item information to the API but the existing reports table only permanently stores the report ID, filer, location, and other fields defined by the schema. Report IDs are prefixed LOST- or FOUND- so the type is retained.
