<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ContactCustomField extends Model
{
    use HasFactory;

    protected $fillable = [
        'user_id',
        'name',
        'label',
        'type',
        'options',
        'default_value',
        'is_required',
        'is_active',
        'sort_order',
        'description',
        'validation_rules',
    ];

    protected $casts = [
        'options' => 'array',
        'validation_rules' => 'array',
        'is_required' => 'boolean',
        'is_active' => 'boolean',
        'sort_order' => 'integer',
    ];

    // Relationships
    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    // Scopes
    public function scopeActive($query)
    {
        return $query->where('is_active', true);
    }

    public function scopeRequired($query)
    {
        return $query->where('is_required', true);
    }

    public function scopeOfType($query, string $type)
    {
        return $query->where('type', $type);
    }

    public function scopeOrdered($query)
    {
        return $query->orderBy('sort_order');
    }

    // Accessors
    public function getFormattedOptionsAttribute(): string
    {
        if (!$this->options || !in_array($this->type, ['select', 'multiselect'])) {
            return '';
        }

        return implode(', ', $this->options);
    }

    public function getValidationRulesStringAttribute(): string
    {
        if (!$this->validation_rules) {
            return '';
        }

        return implode('|', $this->validation_rules);
    }

    // Methods
    public function getValidationRules(): array
    {
        $rules = [];

        if ($this->is_required) {
            $rules[] = 'required';
        }

        switch ($this->type) {
            case 'number':
                $rules[] = 'numeric';
                break;
            case 'date':
                $rules[] = 'date';
                break;
            case 'boolean':
                $rules[] = 'boolean';
                break;
            case 'select':
                if ($this->options) {
                    $rules[] = 'in:' . implode(',', $this->options);
                }
                break;
            case 'multiselect':
                $rules[] = 'array';
                if ($this->options) {
                    $rules[] = 'in:' . implode(',', $this->options);
                }
                break;
        }

        // Add custom validation rules
        if ($this->validation_rules) {
            $rules = array_merge($rules, $this->validation_rules);
        }

        return $rules;
    }

    public function validateValue($value): bool
    {
        $validator = validator(['value' => $value], ['value' => $this->getValidationRules()]);
        return !$validator->fails();
    }
}
