<?php
use Illuminate\Support\Facades\Route;
use App\Http\Controllers\AdminController;
Route::redirect('/','/admin');
Route::redirect('/home','/admin');
Route::middleware('guest')->group(function(){Route::get('/login',[AdminController::class,'login'])->name('login');Route::post('/login',[AdminController::class,'authenticate'])->middleware('throttle:5,1')->name('login.store');});
Route::post('/logout',[AdminController::class,'logout'])->middleware('auth')->name('logout');
Route::prefix('admin')->name('admin.')->middleware('auth')->group(function(){
 Route::get('/',[AdminController::class,'dashboard'])->name('dashboard');
 Route::get('/places',[AdminController::class,'places'])->name('places.index');
 Route::get('/places/create',[AdminController::class,'placeCreate'])->name('places.create');
 Route::post('/places',[AdminController::class,'placeStore'])->name('places.store');
 Route::get('/places/{id}/edit',[AdminController::class,'placeEdit'])->name('places.edit');
 Route::put('/places/{id}',[AdminController::class,'updatePlace'])->name('places.update');
 Route::delete('/places/{id}',[AdminController::class,'placeDelete'])->name('places.destroy');
 Route::get('/news',[AdminController::class,'news'])->name('news.index');
 Route::get('/news/create',[AdminController::class,'newsCreate'])->name('news.create');
 Route::post('/news',[AdminController::class,'newsStore'])->name('news.store');
 Route::get('/news/{id}/edit',[AdminController::class,'newsEdit'])->name('news.edit');
 Route::put('/news/{id}',[AdminController::class,'newsUpdate'])->name('news.update');
 Route::delete('/news/{id}',[AdminController::class,'newsDelete'])->name('news.destroy');
});
