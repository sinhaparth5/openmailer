<?php

namespace App\Models;

use App\Traits\HasUuid;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class EmailLink extends Model
{
    use HasFactory, HasUuid;

    protected $fillable = [
        'campaign_id',
        'original_url',
        'short_code',
        'total_clicks',
        'unique_clicks',
    ];

    protected $casts = [
        'total_clicks' => 'integer',
        'unique_clicks' => 'integer',
    ];

    public function campaign(): BelongsTo
    {
        return $this->belongsTo(Campaign::class);
    }
}
