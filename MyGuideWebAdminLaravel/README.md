# My Guide Laravel Administration Portal

Laravel 12 replacement for the legacy AngularJS/PHP web administration panel. The Android application and its legacy mobile API remain in the parent repository.

## Stack and current foundation
- Laravel 12 / PHP 8.2+
- Existing My Guide MySQL database (no destructive schema migration)
- Session-based administrator authentication with throttling
- Legacy MD5 administrator passwords are upgraded to Laravel password hashes after a successful sign-in
- Dashboard metrics and destination listing/update routes

## Installation
1. Install the Laravel 12 project dependencies with Composer from this directory.
2. Copy .env.example to .env and configure the existing My Guide MySQL database.
3. Run php artisan key:generate.
4. Point the web server document root to this project's public directory.
5. Use SESSION_DRIVER=file unless a Laravel sessions table has been created.
6. Confirm the legacy database tables users, place, category, news_info and gcm exist.

The legacy AngularJS/PHP panel is retained unchanged during the migration. The existing Android API remains in place until a compatibility-tested Laravel API replacement is ready. This is an incremental migration foundation, not yet a complete replacement: category, image upload, news editing, settings, push notification and full mobile API compatibility are still to be migrated and tested. Do not deploy over production before testing against a database copy.
