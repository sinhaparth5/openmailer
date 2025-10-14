<?php

use App\Http\Controllers\Api\TrackingController;
use Illuminate\Support\Facades\Route;
use Inertia\Inertia;

Route::get('/', function () {
    return Inertia::render('welcome');
})->name('home');

Route::middleware(['auth', 'verified'])->group(function () {
    Route::get('dashboard', function () {
        return Inertia::render('dashboard');
    })->name('dashboard');
});

// Tracking routes (public, no auth)
Route::get('/track/open/{id}', [TrackingController::class, 'trackOpen'])->name('track.open');
Route::get('/track/click/{code}', [TrackingController::class, 'trackClick'])->name('track.click');

// Unsubscribe routes
Route::get('/unsubscribe/{token}', [TrackingController::class, 'unsubscribe'])->name('unsubscribe');
Route::post('/unsubscribe/{token}', [TrackingController::class, 'unsubscribe']);

require __DIR__.'/settings.php';
require __DIR__.'/auth.php';
