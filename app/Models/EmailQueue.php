<?php

namespace App\Models;

use App\Traits\HasUuid;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class EmailQueue extends Model
{
    use HasFactory, HasUuid;

    protected $table = 'email_queue';

    protected $fillable = [
        'campaign_id',
        'contact_id',
        'email',
        'status',
        'tracking_id',
        'attempts',
        'error_message',
        'sent_at',
    ];

    protected $casts = [
        'attempts' => 'integer',
        'send_at' => 'datetime',
    ];

    public function campaign(): BelongsTo
    {
        return $this->belongsTo(Campaign::class);
    }

    public function contact(): BelongsTo
    {
        return $this->belongsTo(Contact::class);
    }
}
