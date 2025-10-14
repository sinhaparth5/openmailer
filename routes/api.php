<?php

use App\Http\Controllers\Api\CampaignController;
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
});
