<?php

namespace App\Models;

use App\Traits\HasUuid;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class EmailEvent extends Model {
    use HasFactory, HasUuid;

    protected $fillable = [
        'campaign_id',
        'contact_id',
        'tracking_id',
        'event_type',
        'link_url',
        'user_agent',
        'ip_address'
    ];

    public function campaign(): BelongsTo {
        return $this->belongsTo(Campaign::class);
    }

    public function contact(): BelongsTo {
        return $this->belongsTo(Contact::class);
    }
}
