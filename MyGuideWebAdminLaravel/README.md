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

Configure the existing My Guide database in `.env`, then run `php artisan optimize:clear` and `php artisan route:list`.

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
