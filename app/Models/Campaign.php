<?php

namespace App\Models;

use App\Traits\HasUuid;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\BelongsToMany;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Campaign extends Model
{
    use HasFactory, HasUuid;

    protected $fillable = [
        'user_id',
        'domain_id',
        'template_id',
        'name',
        'subject',
        'from_name',
        'from_email',
        'reply_to',
        'html_content',
        'status',
        'total_recipients',
        'total_sent',
        'total_delivered',
        'total_opens',
        'total_clicks',
        'total_bounces',
        'total_complaints',
        'scheduled_at',
        'started_at',
        'completed_at',
    ];

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function domain(): BelongsTo
    {
        return $this->belongsTo(Domain::class);
    }

    public function template(): BelongsTo
    {
        return $this->belongsTo(EmailTemplate::class, 'template_id');
    }

    public function contacts(): BelongsToMany
    {
        return $this->belongsToMany(Contact::class, 'campaign_recipients')
            ->withPivot('contact_list_id')
            ->withTimestamps();
    }

    public function emailQueues(): HasMany
    {
        return $this->hasMany(EmailQueue::class);
    }

    public function emailEvents(): HasMany
    {
        return $this->hasMany(EmailEvent::class);
    }

    public function emailLinks(): HasMany
    {
        return $this->hasMany(EmailLink::class);
    }
}
