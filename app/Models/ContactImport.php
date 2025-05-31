<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ContactImport extends Model
{
    use HasFactory;

    protected $fillable = [
        'user_id',
        'contact_list_id',
        'filename',
        'original_filename',
        'file_path',
        'status',
        'total_rows',
        'processed_rows',
        'successful_imports',
        'failed_imports',
        'duplicate_contacts',
        'field_mapping',
        'import_options',
        'errors',
        'error_message',
        'started_at',
        'completed_at',
    ];

    protected $casts = [
        'field_mapping' => 'array',
        'import_options' => 'array',
        'errors' => 'array',
        'total_rows' => 'integer',
        'processed_rows' => 'integer',
        'successful_imports' => 'integer',
        'failed_imports' => 'integer',
        'duplicate_contacts' => 'integer',
        'started_at' => 'datetime',
        'completed_at' => 'datetime',
    ];

    // Relationship
    public function user(): BelongsTo {
        return $this->belongsTo(User::class);
    }

    public function contactList(): BelongsTo {
        return $this->belongsTo(ContactList::class);
    }

    // Scope
    public function scopePending($query) {
        return $query->where('status', 'pending');
    }

    public function scopeProcessing($query) {
        return $query->where('status', 'processing');
    }

    public function scopeCompleted($query) {
        return $query->where('status', 'completed');
    }

    public function scopeFailed($query) {
        return $query->where('status', 'failed');
    }

    public function getProgressPercentageAttribute(): float {
        if ($this->total_rows === 0) {
            return 0;
        }
        return round(($this->processed_rows / $this->total_rows) * 100, 2);
    }

    public function getSuccessRateAttribute(): float {
        if ($this->processed_rows === 0 ) {
            return 0;
        }
        return round(($this->successful_imports / $this->processed_rows) * 100, 2);
    }

    public function getDurationAttribute() {
        if (!$this->started_at || !$this->completed_at) {
            return null;
        }

        $duration = $this->completed_at->diffInSeconds($this->started_at);

        if ($duration < 60) {
            return $duration . ' seconds';
        } elseif ($duration < 3600) {
            return round($duration / 60, 1) . ' minutes';
        } else {
            return round($duration / 3600, 1) . ' hours';
        }
    }

    // Methods
    public function markAsProcessing(): void {
        $this->update([
            'status' => 'processing',
            'started_at' => now(),
        ]);
    }

    public function markAsFailed(string $errorMessage): void {
        $this->update([
            'status' => 'failed',
            'error_message' => $errorMessage,
            'completed_at' => now(),
        ]);
    }

    public function updateProgress(int $processed, int $successful = 0, int $failed = 0, int $duplicates = 0): void {
        $this->update([
            'processed_rows' => $processed,
            'successful_imports' => $this->successful_imports + $successful,
            'failed_imports' => $this->failed_imports + $failed,
            'duplicate_contacts' => $this->duplicate_contacts + $duplicates,
        ]);
    }

    public function addError(string $row, string $error): void {
        $errors = $this->errors ?? [];
        $errors[] = [
            'row' => $row,
            'error' => $error,
            'timestamp' => now()->toISOString(),
        ];
        $this->update(['errors' => $errors]);
    }
}
