# My Guide mobile API (legacy compatibility)

This folder contains only the legacy PHP REST API and existing uploaded media retained for compatibility with released Android clients. It is **not** an administration panel. The AngularJS/PHP administration UI has been removed; use the Laravel application in `../MyGuideWebAdminLaravel/` for all administration.

## Deployment

Deploy this directory as the document root of the existing mobile API host. Preserve these public paths:

- `/app/services/api.php` — Android REST endpoint
- `/uploads/place/` — destination images
- `/uploads/news/` — news images

The Android client currently uses `WEB_URL` in `app/src/main/java/com/casper/myguide/data/Constant.java`. Keep that value pointed at the legacy API host unless the API is deliberately migrated and the app is updated in the same release.

The API's database connection constants are in `app/services/api.php`. Configure credentials securely on the server; never commit production database credentials. Back up the existing API and database before deployment. This compatibility API is legacy PHP and should be treated as a transition component, not a replacement for the Laravel admin.
