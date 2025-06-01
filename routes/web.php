<?php

use Illuminate\Support\Facades\Route;
use Livewire\Volt\Volt;

Route::get('/', function () {
    return view('welcome');
})->name('home');

Route::view('dashboard', 'dashboard')
    ->middleware(['auth', 'verified'])
    ->name('dashboard');

Route::middleware(['auth'])->group(function () {
    Route::redirect('settings', 'settings/profile');

    Volt::route('settings/profile', 'settings.profile')->name('settings.profile');
    Volt::route('settings/password', 'settings.password')->name('settings.password');
    Volt::route('settings/appearance', 'settings.appearance')->name('settings.appearance');
});

Route::middleware(['auth'])->group(function () {
    Route::get('/contact/lists', function () {
        return view('contacts.lists');
    })->name('contacts.lists');
});

Route::get('/debug-lists', function () {
    $lists = \App\Models\ContactList::all();
    return response()->json([
        'count' => $lists->count(),
        'lists' => $lists->toArray()
    ]);
});

require __DIR__.'/auth.php';
