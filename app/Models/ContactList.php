<?php

namespace App\Models;

use App\Traits\HasUuid;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\BelongsToMany;

class ContactList extends Model {
    use HasFactory, HasUuid;

    protected $fillable = [
        'user_id',
        'name',
        'description',
        'total_contacts',
    ];

    protected $casts = [
        'total_contacts' => 'integer',
    ];

    public function user(): BelongsTo {
        return $this->belongsTo(User::class);
    }

    public function contacts(): BelongsToMany {
        return $this->belongsToMany(Contact::class, 'contact_list_member')
            ->withTimestamps();
    }
}
