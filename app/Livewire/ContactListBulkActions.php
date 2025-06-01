<?php

namespace App\Livewire;

use Livewire\Component;
use App\Models\ContactList;
use Illuminate\Support\Facades\Auth;

class ContactListBulkActions extends Component
{
    public $selectedLists = [];
    public $bulkAction = '';
    public $showConfirmModal = false;
    
    protected $listeners = [
        'listsSelected' => 'updateSelectedLists',
    ];

    public function updateSelectedLists($lists)
    {
        $this->selectedLists = $lists;
    }

    public function executeBulkAction()
    {
        if (empty($this->selectedLists) || empty($this->bulkAction)) {
            return;
        }

        switch ($this->bulkAction) {
            case 'activate':
                $this->activateLists();
                break;
            case 'deactivate':
                $this->deactivateLists();
                break;
            case 'delete':
                $this->showConfirmModal = true;
                return;
        }

        $this->resetBulkAction();
    }

    public function confirmBulkDelete()
    {
        $this->deleteLists();
        $this->showConfirmModal = false;
        $this->resetBulkAction();
    }

    private function activateLists()
    {
        ContactList::whereIn('id', $this->selectedLists)
            ->where('user_id', Auth::id())
            ->update(['is_active' => true]);
        
        session()->flash('message', count($this->selectedLists) . ' lists activated successfully!');
    }

    private function deactivateLists()
    {
        ContactList::whereIn('id', $this->selectedLists)
            ->where('user_id', Auth::id())
            ->update(['is_active' => false]);
        
        session()->flash('message', count($this->selectedLists) . ' lists deactivated successfully!');
    }

    private function deleteLists()
    {
        ContactList::whereIn('id', $this->selectedLists)
            ->where('user_id', Auth::id())
            ->delete();
        
        session()->flash('message', count($this->selectedLists) . ' lists deleted successfully!');
    }

    private function resetBulkAction()
    {
        $this->selectedLists = [];
        $this->bulkAction = '';
        $this->dispatch('refreshLists');
    }
    
    public function render()
    {
        return view('livewire.contact-list-bulk-actions');
    }
}
