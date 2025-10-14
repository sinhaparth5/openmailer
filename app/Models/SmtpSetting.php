<?php

namespace App\Models;

use App\Traits\HasUuid;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class SmtpSetting extends Model {
    use HasFactory, HasUuid;

    protected $fillable = [
        'user_id',
        'domain_id',
        'host',
        'port',
        'encryption',
        'username',
        'password',
        'is_default',
        'is_active',
    ];

    protected $casts = [
        'port' => 'integer',
        'is_default' => 'boolean',
        'is_active' => 'boolean',
    ];

    protected $hidden = [
        'password',
    ];

    public function user(): BelongsTo {
        return $this->belongsTo(User::class);
    }

    public function domain(): BelongsTo {
        return $this->belongsTo(Domain::class);
    }
}
