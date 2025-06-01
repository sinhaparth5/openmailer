<?php

namespace App\Livewire;

use Livewire\Component;
use Livewire\WithPagination;
use App\Models\ContactList;
use App\Models\Contact;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Str;

class ContactListManager extends Component
{
    use WithPagination;

    // Form properties
    public $name = '';
    public $description = '';
    public $listType = 'static';
    public $isActive = true;
    
    // Modal and editing state
    public $showModal = false;
    public $editingListId = null;
    public $showDeleteModal = false;
    public $deletingListId = null;
    
    // Search and filters
    public $search = '';
    public $typeFilter = 'all';
    public $statusFilter = 'all';
    public $sortBy = 'created_at';
    public $sortDirection = 'desc';

    protected $rules = [
        'name' => 'required|min:3|max:255',
        'description' => 'nullable|max:1000',
        'listType' => 'required|in:static,dynamic',
        'isActive' => 'boolean',
    ];

    protected $messages = [
        'name.required' => 'List name is required.',
        'name.min' => 'List name must be at least 3 characters.',
        'name.max' => 'List name cannot exceed 255 characters.',
        'description.max' => 'Description cannot exceed 1000 characters.',
    ];

    // Reset pagination when search changes
    public function updatingSearch()
    {
        $this->resetPage();
    }

    public function updatingTypeFilter()
    {
        $this->resetPage();
    }

    public function updatingStatusFilter()
    {
        $this->resetPage();
    }

    // Modal management
    public function createList()
    {
        $this->resetForm();
        $this->showModal = true;
    }

    public function editList($listId)
    {
        $list = ContactList::findOrFail($listId);
        
        $this->editingListId = $list->id;
        $this->name = $list->name;
        $this->description = $list->description ?? '';
        $this->listType = $list->type;
        $this->isActive = $list->is_active;
        
        $this->showModal = true;
    }

    public function deleteList($listId)
    {
        $this->deletingListId = $listId;
        $this->showDeleteModal = true;
    }

    // Form actions
    public function save()
    {
        $this->validate();

        try {
            if ($this->editingListId) {
                // Update existing list
                $list = ContactList::findOrFail($this->editingListId);
                $list->update([
                    'name' => trim($this->name),
                    'description' => trim($this->description) ?: null,
                    'type' => $this->listType,
                    'is_active' => $this->isActive,
                ]);
                
                session()->flash('message', 'Contact list updated successfully!');
            } else {
                // Create new list
                ContactList::create([
                    'id' => Str::uuid()->toString(),
                    'user_id' => Auth::id() ?? 1, // Fallback for testing
                    'name' => trim($this->name),
                    'description' => trim($this->description) ?: null,
                    'type' => $this->listType,
                    'is_active' => $this->isActive,
                ]);
                
                session()->flash('message', 'Contact list created successfully!');
            }

            $this->resetForm();
            $this->showModal = false;
            
        } catch (\Exception $e) {
            session()->flash('error', 'An error occurred. Please try again.');
        }
    }

    public function confirmDelete()
    {
        try {
            $list = ContactList::findOrFail($this->deletingListId);
            $list->delete();
            
            $this->showDeleteModal = false;
            $this->deletingListId = null;
            
            session()->flash('message', 'Contact list deleted successfully!');
            
        } catch (\Exception $e) {
            session()->flash('error', 'An error occurred while deleting the list.');
        }
    }

    public function toggleStatus($listId)
    {
        try {
            $list = ContactList::findOrFail($listId);
            $list->update(['is_active' => !$list->is_active]);
            
            $status = $list->is_active ? 'activated' : 'deactivated';
            session()->flash('message', "List {$status} successfully!");
            
        } catch (\Exception $e) {
            session()->flash('error', 'An error occurred while updating the list status.');
        }
    }

    // Sorting
    public function sortBy($field)
    {
        if ($this->sortBy === $field) {
            $this->sortDirection = $this->sortDirection === 'asc' ? 'desc' : 'asc';
        } else {
            $this->sortDirection = 'asc';
        }
        $this->sortBy = $field;
        $this->resetPage();
    }

    // Utility methods
    public function resetForm()
    {
        $this->editingListId = null;
        $this->name = '';
        $this->description = '';
        $this->listType = 'static';
        $this->isActive = true;
        $this->showModal = false;
        $this->resetErrorBag();
        $this->resetValidation();
    }

    public function closeModal()
    {
        $this->resetForm();
    }

    public function closeDeleteModal()
    {
        $this->showDeleteModal = false;
        $this->deletingListId = null;
    }

    public function render()
    {
        $query = ContactList::query()
            ->when($this->search, function ($query) {
                $query->where(function($q) {
                    $q->where('name', 'like', '%' . $this->search . '%')
                      ->orWhere('description', 'like', '%' . $this->search . '%');
                });
            })
            ->when($this->typeFilter !== 'all', function ($query) {
                $query->where('type', $this->typeFilter);
            })
            ->when($this->statusFilter !== 'all', function ($query) {
                $query->where('is_active', $this->statusFilter === 'active');
            })
            ->orderBy($this->sortBy, $this->sortDirection);

        $lists = $query->paginate(10);

        $stats = [
            'total_lists' => ContactList::count(),
            'active_lists' => ContactList::where('is_active', true)->count(),
            'total_contacts' => \App\Models\Contact::count(),
            'subscribed_contacts' => \App\Models\Contact::where('status', 'subscribed')->count(),
        ];

        return view('livewire.contact-list-manager', [
            'lists' => $lists,
            'stats' => $stats,
        ]);
    }
}
