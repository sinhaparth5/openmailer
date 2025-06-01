<?php

namespace App\Livewire;

use Livewire\Component;
use Livewire\WithPagination;
use App\Models\ContactList;
use App\Models\Contact;
use Illuminate\Support\Facades\Auth;

class ContactListManager extends Component
{
    use WithPagination;

    public $search = '';
    public $type = 'all'; // all, static, dynamic
    public $status = 'all'; // all, active, inactive
    public $sortBy = 'created_at';
    public $sortDirection = 'desc';
    
    // Modal states
    public $showCreateModal = false;
    public $showEditModal = false;
    public $showDeleteModal = false;
    
    // Form properties
    public $selectedListId = null;
    public $name = '';
    public $description = '';
    public $listType = 'static';
    public $isActive = true;
    
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
        'listType.required' => 'Please select a list type.',
        'listType.in' => 'Invalid list type selected.',
    ];

    protected $listeners = [
        'refreshLists' => '$refresh',
        'listDeleted' => 'handleListDeleted',
    ];

    public function mount()
    {
        // Initialize any default values if needed
    }

    public function updatingSearch()
    {
        $this->resetPage();
    }

    public function updatingType()
    {
        $this->resetPage();
    }

    public function updatingStatus()
    {
        $this->resetPage();
    }

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

    public function createList()
    {
        $this->resetForm();
        $this->showCreateModal = true;
    }

    public function editList($listId)
    {
        try {
            $list = ContactList::where('user_id', Auth::id())->findOrFail($listId);
            
            $this->selectedListId = $list->id;
            $this->name = $list->name;
            $this->description = $list->description ?? '';
            $this->listType = $list->type;
            $this->isActive = $list->is_active;
            
            $this->showEditModal = true;
        } catch (\Exception $e) {
            session()->flash('error', 'List not found or access denied.');
        }
    }

    public function deleteList($listId)
    {
        try {
            $list = ContactList::where('user_id', Auth::id())->findOrFail($listId);
            $this->selectedListId = $list->id;
            $this->showDeleteModal = true;
        } catch (\Exception $e) {
            session()->flash('error', 'List not found or access denied.');
        }
    }

    public function save()
    {
        $this->validate();

        try {
            ContactList::create([
                'user_id' => Auth::id(),
                'name' => trim($this->name),
                'description' => trim($this->description) ?: null,
                'type' => $this->listType,
                'is_active' => $this->isActive,
            ]);

            $this->resetForm();
            $this->showCreateModal = false;
            
            session()->flash('message', 'Contact list created successfully!');
        } catch (\Exception $e) {
            session()->flash('error', 'Failed to create contact list. Please try again.');
        }
    }

    public function update()
    {
        $this->validate();

        try {
            $list = ContactList::where('user_id', Auth::id())->findOrFail($this->selectedListId);
            $list->update([
                'name' => trim($this->name),
                'description' => trim($this->description) ?: null,
                'type' => $this->listType,
                'is_active' => $this->isActive,
            ]);

            $this->resetForm();
            $this->showEditModal = false;
            
            session()->flash('message', 'Contact list updated successfully!');
        } catch (\Exception $e) {
            session()->flash('error', 'Failed to update contact list. Please try again.');
        }
    }

    public function confirmDelete()
    {
        try {
            $list = ContactList::where('user_id', Auth::id())->findOrFail($this->selectedListId);
            
            // Check if list has contacts
            $contactsCount = $list->contacts()->count();
            if ($contactsCount > 0) {
                // Detach all contacts before deleting
                $list->contacts()->detach();
            }
            
            $list->delete();
            
            $this->showDeleteModal = false;
            $this->selectedListId = null;
            
            session()->flash('message', 'Contact list deleted successfully!');
        } catch (\Exception $e) {
            session()->flash('error', 'Failed to delete contact list. Please try again.');
        }
    }

    public function toggleStatus($listId)
    {
        try {
            $list = ContactList::where('user_id', Auth::id())->findOrFail($listId);
            $list->update(['is_active' => !$list->is_active]);
            
            $status = $list->is_active ? 'activated' : 'deactivated';
            session()->flash('message', "List {$status} successfully!");
        } catch (\Exception $e) {
            session()->flash('error', 'Failed to update list status. Please try again.');
        }
    }

    public function resetForm()
    {
        $this->selectedListId = null;
        $this->name = '';
        $this->description = '';
        $this->listType = 'static';
        $this->isActive = true;
        $this->showCreateModal = false;
        $this->showEditModal = false;
        $this->resetErrorBag();
        $this->resetValidation();
    }

    public function handleListDeleted()
    {
        $this->resetForm();
        $this->dispatch('refreshLists');
    }

    public function render()
    {
        try {
            $query = ContactList::query()
                ->where('user_id', Auth::id())
                ->when($this->search, function ($query) {
                    $query->where(function($q) {
                        $q->where('name', 'like', '%' . $this->search . '%')
                          ->orWhere('description', 'like', '%' . $this->search . '%');
                    });
                })
                ->when($this->type !== 'all', function ($query) {
                    $query->where('type', $this->type);
                })
                ->when($this->status !== 'all', function ($query) {
                    $query->where('is_active', $this->status === 'active');
                })
                ->orderBy($this->sortBy, $this->sortDirection);

            $lists = $query->paginate(10);

            return view('livewire.contact-list-manager', [
                'lists' => $lists,
            ]);
        } catch (\Exception $e) {
            // Log the error for debugging
            logger()->error('ContactListManager render error: ' . $e->getMessage());
            
            // Return empty result set on error
            return view('livewire.contact-list-manager', [
                'lists' => collect()->paginate(10),
            ]);
        }
    }
}
