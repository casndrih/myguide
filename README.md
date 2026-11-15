# My Guide — Papua New Guinea tourism and safe-travel app

My Guide is an Android application and web-managed tourism guide for Papua New Guinea. It helps visitors explore destinations by category, read travel/news updates, view maps and weather, save favourites, and receive Firebase Cloud Messaging notifications.

The repository contains the Android client, the existing mobile REST API, and a Laravel 12 administration panel. The Laravel panel replaces the old AngularJS/PHP administration interface; the legacy mobile API is retained for deployed Android clients.

## Repository layout

| Path | Purpose |
| --- | --- |
| `app/` | Android application source (Java) |
| `gradle/`, `gradlew*` | Gradle wrapper |
| `MyGuideWebAdminLaravel/` | Laravel 12 admin application |
| `MyGuideWebAdmin/app/services/` | Existing PHP REST API used by Android |
| `MyGuideWebAdmin/uploads/` | Legacy uploaded media retained for compatibility |
| `.github/workflows/` | Android and Laravel CI workflows |

## Android application

- Application ID: `com.casper.myguide`
- Current version: `1.1.0` (version code 2)
- Minimum Android: API 21
- Compile SDK: 36
- Target SDK: 35
- Java: 17
- Android Gradle Plugin: 8.13.2
- Gradle wrapper: 8.13

### Build locally

Install JDK 17 and Android SDK platform/build tools 36. Then run:

```bash
./gradlew --version
./gradlew clean assembleDebug
./gradlew assembleRelease
```

The release build enables R8 code shrinking/obfuscation and resource shrinking. Core-library desugaring is enabled for older Android versions. The unsigned release APK is written to `app/build/outputs/apk/release/`.

### Release signing

Do not commit signing keys or passwords. For a signed release, set these environment variables in the build environment:

- `MYGUIDE_STORE_FILE`
- `MYGUIDE_STORE_PASSWORD`
- `MYGUIDE_KEY_ALIAS`
- `MYGUIDE_KEY_PASSWORD`

Without all four variables, Gradle uses the generated debug keystore so the CI APK can be installed for testing. A debug-signed APK is not a production/Play Store release. The old hard-coded test keystore configuration has been removed.

The Firebase Android configuration remains in `app/google-services.json`. Restrict its API keys in Google Cloud Console and use Firebase project restrictions appropriate to the production package and signing certificate.

## Laravel administration panel

The Laravel application is in `MyGuideWebAdminLaravel/`.

### Requirements

- PHP 8.2 or newer
- Composer 2
- MySQL/MariaDB with PDO MySQL
- PHP extensions: fileinfo and mbstring

### Local setup

```bash
cd MyGuideWebAdminLaravel
composer install
cp .env.example .env
php artisan key:generate
```

Edit `.env` with the existing My Guide database connection and the correct `APP_URL`, then run:

```bash
php artisan optimize:clear
php artisan route:list
php artisan serve
```

Open `/login`. Sign in using an existing administrator account from the legacy `users` table. Existing MD5 password hashes are verified once and upgraded to Laravel's configured password hash after a successful login.

### Admin features

- Overview dashboard and content counts
- Destination search, create, edit, delete, coordinate and category assignment
- Primary and additional destination image uploads
- Category management
- News and travel-update management with banner uploads
- Read-only registered-device list (registration tokens are not displayed)

Uploads are written to `public/uploads/place` and `public/uploads/news`, preserving the image URL paths used by the Android client. Ensure the web-server user can write to these directories and configure the web server document root to the Laravel `public/` directory.

### Existing database compatibility

The panel intentionally uses the existing My Guide database tables rather than running destructive migrations against a live installation. It expects the legacy tables and columns used by the mobile API, including:

- `users` (`id`, `name`, `username`, `email`, `password`)
- `place` (`place_id`, `name`, `image`, `address`, `fee`, `status`, `phone`, `website`, `description`, `lat`, `lng`, `last_update`)
- `category` (`cat_id`, `name`, `icon`)
- `place_category` (`place_id`, `cat_id`)
- `images` (`place_id`, `name`)
- `news_info` (`id`, `title`, `brief_content`, `full_content`, `image`, `last_update`)
- `gcm` (`id`, `device`, `email`, `version`, `regid`, `date_create`)

Back up the production database and test the panel against a staging copy before switching production traffic. No schema migration is run automatically.

### Mobile API compatibility and deployment

Keep the existing `MyGuideWebAdmin/app/services/` REST API available at the URL currently configured in `app/src/main/java/com/casper/myguide/data/Constant.java`. Deploy the Laravel application's `public/` contents as the web document root (or configure an equivalent virtual host), and preserve the existing API endpoint and upload URL paths. Update the Android `Constant.WEB_URL` only when the production domain changes.

The previous FCM sender used the retired legacy HTTP API. The Laravel admin currently provides device visibility but does not send push messages; production push sending should be wired to Firebase Cloud Messaging HTTP v1 using a server-side service-account credential stored outside the repository.

## CI

GitHub Actions validates the Laravel application and builds the Android release APK. The Android workflow uploads the generated APK as a workflow artifact. The release build is unsigned unless repository Actions secrets are configured and mapped to the signing environment variables above.

## License and contributors

See [LICENSE](LICENSE). My Guide was originally developed by Casper N'drih and Isaac Silas for the 2021 APEC App Challenge.
