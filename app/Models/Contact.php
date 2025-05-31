<?php

namespace App\Models;

use Illuminate\Contracts\Database\Eloquent\Builder;
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
    public function scopeSubscribed($query): Builder {
        return $query->where('status', 'subscribed');
    }

    public function scopeUnsubscribed($query): Builder {
        return $query->where('status', 'unsubscribed');
    }

    public function scopeBounced($query): Builder {
        return $query->where('status', 'bounced');
    }

    public function scopeComplained($query): Builder {
        return $query->where('status', 'complained');
    }

    public function scopeVerified($query): Builder {
        return $query->where('email_verified', true);
    }

    public function scopeUnverified($query): Builder {
        return $query->where('email_verified', false);
    }

    public function scopedWithTag($query, string $tag): Builder {
        return $query->whereJsonContains('tags', $tag);
    }

    public function scopeSearch($query, string $search): Builder {
        return $query->where(function($q) use($search) {
            $q->where('email', 'like', "%($search)%")
            ->orwhere('first_name', 'like', "%($search)%")
            ->orWhere('last_name', 'like', "%($search)%")
            ->orWhere('company', 'like', "%($search)%");
        });
    }

    // Accessors & Mutators
    public function getFullNameAttribute(): string {
        return trim($this->first_name . ' ' . $this->last_name);
    }

    public function getDisplayNameAttribute(): string {
        return $this->full_name ?: $this->email;
    }

    public function getInitialsAttribute(): string {
        $names = explode(' ', $this->full_name);
        $initials = '';
        foreach ($names as $name) {
            $initials .= substr($name, 0, 1);
        }
        return strtoupper(substr($initials, 0, 2));
    }

    public function setEmailAttributes($value): void {
        $this->attributes['email'] = strtolower(trim($value));
    }

    // Methods
    public function subscribe(string $source = 'manual'): void {
        $this->update([
            'status' => 'subscribed',
            'subscribed_at' => now(),
            'unsubscribed_at' => null,
            'unsubscribe_reason' => null,
            'last_activity_at' => now(),
        ]);
        $this->logActivity('subscribed', 'Contact subscribed', ['source' => $source]);
    }

    public function markAsBounced(): void {
        $this->update([
            'status' => 'bounced',
            'last_activity_at' => now(),
        ]);
        $this->logActivity('bounced', 'Email bounced');
    }

    public function maskAsComplained(): void {
        $this->update([
            'status' => 'complained',
            'last_activity_at' => now(),
        ]);
        $this->logActivity('complained', 'Spam complaint received');
    }

    public function verify(): void {
        $this->update([
            'email_verified' => true,
            'email_verfied_at' => now(),
            'verification_token' => null,
            'last_activity_at' => now(),
        ]);
        $this->logActivity('email_verified', 'Email address verified');
    }

    public function addTag(string $tag): void {
        $tags = $this->tags ?? [];
        if (!in_array($tag, $tags)) {
            $tags[] = $tag;
            $this->update(['tags' => $tags]);
            $this->logActivity('tag_added', 'Tag added', ['tag' => $tag]);
        }
    }

    public function removeTag(string $tag): void
    {
        $tags = $this->tags ?? [];
        if (($key = array_search($tag, $tags)) !== false) {
            unset($tags[$key]);
            $this->update(['tags' => array_values($tags)]);
            $this->logActivity('tag_removed', 'Tag removed', ['tag' => $tag]);
        }
    }

    public function updateCustomField(string $field, $value): void
    {
        $customFields = $this->custom_fields ?? [];
        $oldValue = $customFields[$field] ?? null;
        $customFields[$field] = $value;
        
        $this->update(['custom_fields' => $customFields]);
        
        $this->logActivity('custom_field_updated', 'Custom field updated', [
            'field' => $field,
            'old_value' => $oldValue,
            'new_value' => $value
        ]);
    }

    public function logActivity(string $type, ?string $description = null, array $properties = []): ContactActivity {
        return $this->activities()->create([
            'user_id' => $this->user_id,
            'activity_type' => $type,
            'description' => $description,
            'source' => 'system',
            'ip_address' => request()->ip(),
            'user_agent' => request()->userAgent(),
        ]);
    }
}
