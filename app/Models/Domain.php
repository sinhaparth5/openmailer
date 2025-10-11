<?php

namespace App\Models;

use App\Traits\HasUuid;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Domain extends Model {
    use HasFactory, HasUuid;

    protected $fillable = [
        'user_id',
        'domain',
        'verification_token',
        'is_verified',
        'dkim_selector',
        'dkim_private_key',
        'dkim_public_key',
        'spf_record',
        'dmarc_record',
        'status',
    ];

    protected $casts = [
        'is_verified' => 'boolean',
    ];

    public function user(): BelongsTo {
        return $this->belongsTo(User::class);
    }

    public function campaigns(): HasMany {
        return $this->hasMany(Campaign::class);
    }

    public function smtpSettings(): HasMany {
        return $this->hasMany(SmtpSetting::class);
    }
}
