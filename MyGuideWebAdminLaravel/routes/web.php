<?php
use Illuminate\Support\Facades\Route;
use App\Http\Controllers\AdminController;
Route::redirect('/','/admin');
Route::middleware('guest')->group(function(){Route::get('/login',[AdminController::class,'login'])->name('login');Route::post('/login',[AdminController::class,'authenticate'])->middleware('throttle:5,1')->name('login.store');});
Route::post('/logout',[AdminController::class,'logout'])->middleware('auth')->name('logout');
Route::prefix('admin')->name('admin.')->middleware('auth')->group(function(){Route::get('/',[AdminController::class,'dashboard'])->name('dashboard');Route::get('/places',[AdminController::class,'places'])->name('places.index');Route::put('/places/{id}',[AdminController::class,'updatePlace'])->name('places.update');});
