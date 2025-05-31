<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ContactActivity extends Model
{
    //
    use HasFactory;

    protected $fillable = [
        'contact_id',
        'user_id',
        'activity_type',
        'description',
        'properties',
        'old_values',
        'new_values',
        'source',
        'ip_address',
        'user_agent',
    ];

    protected $casts = [
        'properties' => 'array',
        'old_values' => 'array',
        'new_values' => 'array',
    ];

    // Relationships
    public function contact(): BelongsTo {
        return $this->belongsTo(Contact::class);
    }

    public function user(): BelongsTo {
        return $this->belongsTo(User::class);
    }

    // Scopes
    public function scopeOfType($query, string $type) {
        return $query->where('activity_type', $type);
    }

    public function scopeRecent($query, int $days = 30) {
        return $query->where('created_at', '>=', now()->subDays($days));
    }

    public function scopeFromSource($query, string $source) {
        return $query->where('source', $source);
    }

    // Accessors
    public function getFormattedPropertiesAttribute(): string {
        if (!$this->properties) {
            return '';
        }

        $formatted = [];
        foreach ($this->properties as $key => $value) {
            $formatted[] = ucfirst($key) . ': ' .(is_array($value) ? json_encode($value) : $value);
        }
        return implode(', ', $formatted);
    }

    public function getActivityIconAttribute(): string
    {
        return match($this->activity_type) {
            'subscribed' => '✅',
            'unsubscribed' => '❌',
            'bounced' => '⚠️',
            'complained' => '🚫',
            'email_verified' => '✉️',
            'updated' => '✏️',
            'imported' => '📥',
            'tag_added' => '🏷️',
            'tag_removed' => '🗑️',
            'custom_field_updated' => '📝',
            default => '📌',
        };
    }

    public function getActivityColorAttribute(): string {
        return match($this->activity_type) {
            'subscribed', 'email_verified' => 'green',
            'unsubscribed', 'bounced', 'complained' => 'red',
            'updated', 'custom_field_updated' => 'blue',
            'imported' => 'purple',
            'tag_added', 'tag_removed' => 'yellow',
            default => 'gray',
        };
    }
}
