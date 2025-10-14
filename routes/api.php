<?php

use App\Http\Controllers\Api\CampaignController;
use App\Http\Controllers\Api\ContactController;
use App\Http\Controllers\Api\DomainController;
use App\Http\Controllers\Api\TemplateController;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

Route::get('/user', function (Request $request) {
    return $request->user();
})->middleware('auth:sanctum');

Route::middleware('auth:sanctum')->group(function () {
   Route::prefix('campaigns')->group(function () {
       Route::get('/', [CampaignController::class, 'index']);
       Route::post('/', [CampaignController::class, 'store']);
       Route::get('/{campaign}', [CampaignController::class, 'show']);
       Route::put('/{campaign}', [CampaignController::class, 'update']);
       Route::delete('/{campaign}', [CampaignController::class, 'destroy']);
       Route::post('/{campaign}/send', [CampaignController::class, 'send']);
       Route::post('/{campaign}/pause', [CampaignController::class, 'pause']);
       Route::post('/{campaign}/resume', [CampaignController::class, 'resume']);
       Route::get('/{campaign}/stats', [CampaignController::class, 'stats']);
   });

    // Contact Lists
    Route::prefix('contact-lists')->group(function () {
        Route::get('/', [ContactController::class, 'indexLists']);
        Route::post('/', [ContactController::class, 'storeList']);
        Route::get('/{contactList}', [ContactController::class, 'showList']);
        Route::put('/{contactList}', [ContactController::class, 'updateList']);
        Route::delete('/{contactList}', [ContactController::class, 'destroyList']);
        Route::post('/{contactList}/import', [ContactController::class, 'import']);
    });

    // Contacts
    Route::prefix('contacts')->group(function () {
        Route::get('/', [ContactController::class, 'index']);
        Route::post('/', [ContactController::class, 'store']);
        Route::get('/{contact}', [ContactController::class, 'show']);
        Route::put('/{contact}', [ContactController::class, 'update']);
        Route::delete('/{contact}', [ContactController::class, 'destroy']);
    });

    // Domains
    Route::prefix('domains')->group(function () {
        Route::get('/', [DomainController::class, 'index']);
        Route::post('/', [DomainController::class, 'store']);
        Route::get('/{domain}', [DomainController::class, 'show']);
        Route::delete('/{domain}', [DomainController::class, 'destroy']);
        Route::post('/{domain}/verify', [DomainController::class, 'verify']);
        Route::get('/{domain}/dns-instructions', [DomainController::class, 'dnsInstructions']);
    });

    // Email Templates
    Route::prefix('templates')->group(function () {
        Route::get('/', [TemplateController::class, 'index']);
        Route::post('/', [TemplateController::class, 'store']);
        Route::get('/{template}', [TemplateController::class, 'show']);
        Route::put('/{template}', [TemplateController::class, 'update']);
        Route::delete('/{template}', [TemplateController::class, 'destroy']);
        Route::post('/{template}/duplicate', [TemplateController::class, 'duplicate']);
        Route::post('/{template}/toggle-favorite', [TemplateController::class, 'toggleFavorite']);
    });
});
