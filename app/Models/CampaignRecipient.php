<?php

namespace App\Models;

use App\Traits\HasUuid;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class CampaignRecipient extends Model {
    use HasFactory, HasUuid;

    protected $fillable = [
        'campaign_id',
        'contact_id',
        'contact_list_id',
    ];

    public function campaign(): BelongsTo {
        return $this->belongsTo(Campaign::class);
    }

    public function contact(): BelongsTo {
        return $this->belongsTo(Contact::class);
    }

    public function contactList(): BelongsTo {
        return $this->belongsTo(ContactList::class);
    }
}
