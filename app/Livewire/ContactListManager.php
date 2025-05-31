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
    public $type = 'all';
    public $status = 'all';
    public $sortBy = 'created_at';
    public $sortDirection = 'desc';

    // Modal states
    public $showCreateModal = false;
    public $showEditModal = false;
    public $showDeleteModal = false;

    // Form Properties
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

    protected $listeners = [
        'refreshLists' => '$refresh',
        'listDeleted' => 'handleListDeleted',
    ];

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
    }

    public function createList()
    {
        $this->resetForm();
        $this->showCreateModal = true;
    }

    public function editList($listId)
    {
        $list = ContactList::findOrFail($listId);
        
        $this->selectedListId = $list->id;
        $this->name = $list->name;
        $this->description = $list->description;
        $this->listType = $list->type;
        $this->isActive = $list->is_active;
        
        $this->showEditModal = true;
    }

    public function deleteList($listId)
    {
        $this->selectedListId = $listId;
        $this->showDeleteModal = true;
    }

    public function save()
    {
        $this->validate();

        ContactList::create([
            'user_id' => Auth::id(),
            'name' => $this->name,
            'description' => $this->description,
            'type' => $this->listType,
            'is_active' => $this->isActive,
        ]);

        $this->resetForm();
        $this->showCreateModal = false;
        
        session()->flash('message', 'Contact list created successfully!');
    }

    public function update()
    {
        $this->validate();

        $list = ContactList::findOrFail($this->selectedListId);
        $list->update([
            'name' => $this->name,
            'description' => $this->description,
            'type' => $this->listType,
            'is_active' => $this->isActive,
        ]);

        $this->resetForm();
        $this->showEditModal = false;
        
        session()->flash('message', 'Contact list updated successfully!');
    }

    public function confirmDelete()
    {
        $list = ContactList::findOrFail($this->selectedListId);
        $list->delete();
        
        $this->showDeleteModal = false;
        $this->selectedListId = null;
        
        session()->flash('message', 'Contact list deleted successfully!');
    }

    public function toggleStatus($listId)
    {
        $list = ContactList::findOrFail($listId);
        $list->update(['is_active' => !$list->is_active]);
        
        session()->flash('message', 'List status updated successfully!');
    }

    public function resetForm()
    {
        $this->selectedListId = null;
        $this->name = '';
        $this->description = '';
        $this->listType = 'static';
        $this->isActive = true;
        $this->resetErrorBag();
    }


    public function render(){
        $query = ContactList::query()
                    ->where('user_id', Auth::id())
                    ->when($this->search, function($query) {
                        $query->where('name', 'like', '%' . $this->search . '%')
                                ->orWhere('description', 'like', '%' . $this->search . '%');
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
    }
}
