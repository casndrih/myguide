<?php

use App\Http\Controllers\AdminController;
use Illuminate\Support\Facades\Route;

Route::redirect('/', '/admin');
Route::redirect('/home', '/admin');

Route::middleware('guest')->group(function (): void {
    Route::get('/login', [AdminController::class, 'login'])->name('login');
    Route::post('/login', [AdminController::class, 'authenticate'])
        ->middleware('throttle:5,1')
        ->name('login.store');
});

Route::post('/logout', [AdminController::class, 'logout'])
    ->middleware('auth')
    ->name('logout');

Route::prefix('admin')->name('admin.')->middleware('auth')->group(function (): void {
    Route::get('/', [AdminController::class, 'dashboard'])->name('dashboard');

    Route::get('/places', [AdminController::class, 'places'])->name('places.index');
    Route::get('/places/create', [AdminController::class, 'placeCreate'])->name('places.create');
    Route::post('/places', [AdminController::class, 'placeStore'])->name('places.store');
    Route::get('/places/{id}/edit', [AdminController::class, 'placeEdit'])->whereNumber('id')->name('places.edit');
    Route::put('/places/{id}', [AdminController::class, 'updatePlace'])->whereNumber('id')->name('places.update');
    Route::delete('/places/{id}', [AdminController::class, 'placeDelete'])->whereNumber('id')->name('places.destroy');

    Route::get('/categories', [AdminController::class, 'categories'])->name('categories.index');
    Route::post('/categories', [AdminController::class, 'categoryStore'])->name('categories.store');
    Route::put('/categories/{id}', [AdminController::class, 'categoryUpdate'])->whereNumber('id')->name('categories.update');
    Route::delete('/categories/{id}', [AdminController::class, 'categoryDelete'])->whereNumber('id')->name('categories.destroy');

    Route::get('/news', [AdminController::class, 'news'])->name('news.index');
    Route::get('/news/create', [AdminController::class, 'newsCreate'])->name('news.create');
    Route::post('/news', [AdminController::class, 'newsStore'])->name('news.store');
    Route::get('/news/{id}/edit', [AdminController::class, 'newsEdit'])->whereNumber('id')->name('news.edit');
    Route::put('/news/{id}', [AdminController::class, 'newsUpdate'])->whereNumber('id')->name('news.update');
    Route::delete('/news/{id}', [AdminController::class, 'newsDelete'])->whereNumber('id')->name('news.destroy');

    Route::get('/devices', [AdminController::class, 'devices'])->name('devices.index');
});
