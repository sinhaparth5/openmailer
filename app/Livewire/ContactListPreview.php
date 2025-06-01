<?php

namespace App\Livewire;

use Livewire\Component;
use App\Models\ContactList;
use App\Models\Contact;

class ContactListPreview extends Component
{
    public $listId;
    public $showModal = false;
    
    protected $listeners = [
        'showListPreview' => 'show',
    ];

    public function show($listId)
    {
        $this->listId = $listId;
        $this->showModal = true;
    }

    public function close()
    {
        $this->showModal = false;
        $this->listId = null;
    }

    public function render()
    {
        $list = null;
        $contacts = collect();
        $recentActivities = collect();

        if ($this->listId) {
            $list = ContactList::with(['user'])->find($this->listId);

            if ($list) {
                $contacts = $list->subscribedContacts()
                            ->latest()
                            ->limit(10)
                            ->get();

                // Get recent activities for this list's contacts
                $contactIds = $list->contacts()->pluck('contacts.id');
                $recentActivities = \App\Models\ContactActivity::whereIn('contact_id', $contactIds)
                                    ->with(['contact'])
                                    ->latest()
                                    ->limit(5)
                                    ->get();
            }
        }
        return view('livewire.contact-list-preview', [
            'list' => $list,
            'contact' => $contacts,
            'recentActivities' -> $recentActivities,
        ]);
    }
}
