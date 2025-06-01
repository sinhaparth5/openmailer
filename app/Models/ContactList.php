<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\BelongsToMany;
use Illuminate\Database\Eloquent\Relations\HasMany;

class ContactList extends Model
{
    //
    use HasFactory, HasUuids;
    protected $fillable = [
        'user_id',
        'name',
        'description',
        'segmentation_rules',
        'type',
        'is_active',
        'contacts_count',
        'last_cleaned_at',
    ];

    protected $casts = [
        'segmentation_rules' => 'array',
        'is_active' => 'boolean',
        'contacts_count' => 'integer',
        'last_cleaned_at' => 'datetime',
    ];

    protected $keyType = 'string';
    public $incrementing = false;

    // Relationship
    public function user(): BelongsTo {
        return $this->belongsTo(User::class);
    }

    public function contacts(): BelongsToMany {
        return $this->belongsToMany(Contact::class)
                    ->withPivot([
                        'subscription_status',
                        'subscribed_at',
                        'unsubscribed_at',
                        'subscription_source',
                        'subscription_metadata'
                    ])
                    ->withTimestamps();
    }

    public function subscribedContacts(): BelongsToMany {
        return $this->contacts()->wherePivot('subscription_status', 'subscribed');
    }

    public function unsubcribedContacts(): BelongsToMany {
        return $this->contacts()->wherePivot('subscription_status', 'unsubscribed');
    }

    public function contactImports(): HasMany {
        return $this->hasMany(ContactImport::class);
    }

    public function scopeActive($query) {
        return $query->where('is_active', true);
    }

    public function scopeStatic($query) {
        return $query->where('type', 'static');
    }

    public function scopeDynamic($query) {
        return $query->where('type', 'dynamic');
    }

    // Accessors & Mutators
    public function getSubscribedCountAttribute(): int {
        return $this->subscribedContacts()->count();
    }

    public function getUnsubscribedCountAttribute(): int {
        return $this->unsubcribedContacts()->count();
    }

    // Methods
    public function updateContactsCount(): void {
        $this->update([
            'contacts_count' => $this->subscribedContacts()->count()
        ]);
    }

    public function addContacts(Contact $contact, array $pivotData = []): void {
        $defaultPivotData = [
            'subscription_status' => 'subscribed',
            'subscribed_at' => now(),
            'subscription_source' => 'manual',            
        ];

        $this->contacts()->attach($contact->id, array_merge($defaultPivotData, $pivotData));
        $this->updateContactsCount();
    }

    public function removeContact(Contact $contact): void {
        $this->contacts()->detach($contact->id);
        $this->updateContactsCount();
    }
}
