# My Guide Admin (Laravel 12)

Laravel administration panel for the My Guide Papua New Guinea Android application.

## Requirements

- PHP 8.2+
- Composer 2
- MySQL/MariaDB and PDO MySQL
- fileinfo and mbstring PHP extensions

## Install

```bash
composer install
cp .env.example .env
php artisan key:generate
```

Configure the existing My Guide database in `.env`, set `SESSION_SECURE_COOKIE=true` on the production HTTPS host (keep it false for local `php artisan serve`), then run `php artisan optimize:clear` and `php artisan route:list`.

The application intentionally does not run schema migrations against the legacy production database. It uses the existing `users`, `place`, `category`, `place_category`, `images`, `news_info`, and `gcm` tables. Test against a database backup before deployment.

## Features

- Session-based administrator login with rate-limited authentication
- Legacy MD5 password verification and one-time upgrade to Laravel password hashing
- Dashboard and content counts
- Destination CRUD, category assignment, primary and gallery image uploads
- Category CRUD
- News CRUD and banner uploads
- Registered-device listing without exposing FCM registration tokens

Uploaded media is written to `public/uploads/place` and `public/uploads/news`. The web-server user must have write access to these directories.

Keep the legacy REST API in `MyGuideWebAdmin/app/services/` online until all deployed Android clients have migrated to a replacement API. Push notification sending is not included; integrate FCM HTTP v1 with a server-side service-account secret before enabling notification administration.


## Deployment layout

Keep the existing mobile API served from `MyGuideWebAdmin/app/services/` at the URL configured in the Android application's `Constant.WEB_URL`. Deploy this Laravel application's `public/` directory as a separate administration virtual host or subdomain. Both hosts must point to the same existing My Guide MySQL database.

The Laravel `public/uploads/place` and `public/uploads/news` directories must share storage with the legacy API host's public `/uploads/place` and `/uploads/news` paths (for example, with a server-side symlink or shared mounted volume). This is required because the Android client resolves image URLs against its legacy API base URL. Do not change the Android API base URL to the Laravel admin host unless the legacy API routes are also mapped there.
