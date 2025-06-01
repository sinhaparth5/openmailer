<?php

namespace App\Livewire;

use Livewire\Component;
use App\Models\ContactList;
use Illuminate\Support\Facades\Auth;

class ContactListSelector extends Component
{
    public $selectedLists = [];
    public $search = '';
    public $multiple = true;
    public $placeholder = 'Select contact lists...';

    public function mount($selectedLists = [], $multiple = true, $placeholder = null) {
        $this->selectedLists = is_array($selectedLists) ? $selectedLists : [$selectedLists];
        $this->multiple = $multiple;
        if ($placeholder) {
            $this->placeholder = $placeholder;
        }
    }

    public function selectList($listId) {
        if ($this->multiple) {
            if (in_array($listId, $this->selectedLists)) {
                $this->selectedLists = array_filter($this->selectedLists, fn($id) => $id != $listId);
            } else {
                $this->selectedLists[] = $listId;
            }
        } else {
            $this->selectedLists = [$listId];
        }

        $this->dispatch('listsSelected', $this->selectedLists);
    }

    public function clearSelection() {
        $this->selectedLists = [];
        $this->dispatch('listsSelection', $this->selectedLists);
    }

    public function render()
    {
        $lists = ContactList::query()
                ->where('user_id', Auth::id())
                ->active()
                ->when($this->search, function ($query) {
                    $query->where('name', 'like', '%' . $this->search . '%');
                })
                ->orderBy('name')
                ->get();
        return view('livewire.contact-list-selector', [
            'lists' => $lists,
        ]);
    }
}
