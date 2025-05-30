<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\BelongsToMany;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Contact extends Model
{
    use HasFactory;

    protected $fillable = [
        'user_id',
        'email',
        'first_name',
        'last_name',
        'phone',
        'company',
        'job_title',
        'custom_fields',
        'tags',
        'status',
        'subscribed_at',
        'unsubscribed_at',
        'unsubscribe_reason',
        'source',
        'ip_address',
        'user_agent',
        'email_verified',
        'email_verified_at',
        'verification_token',
        'last_activity_at',
    ];

    protected $casts = [
        'custom_fields' => 'array',
        'tags' => 'array',
        'email_verified' => 'boolean',
        'subscribed_at' => 'datetime',
        'unsubscribed_at' => 'datetime',
        'email_verified_at' => 'datetime',
        'last_activity_at' => 'datetime',
    ];

    // RelationShips
    public function user(): BelongsTo {
        return $this->belongsTo(User::class);
    }

    public function contactList(): BelongsToMany {
        return $this->belongsToMany(ContactList::class)
                    ->withPivot([
                        'subscription_status',
                        'subscribed_at',
                        'unsubscribed_at',
                        'subscription_source',
                        'subscription_metadata'
                    ])
                    ->withTimestamps();
    }

    public function activities(): HasMany {
        return $this->hasMany(ContactActivity::class);
    }

    // Scopes
    
}
