<?php

namespace App\Livewire;

use Livewire\Component;
use App\Models\ContactList;
use App\Models\Contact;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\DB;

class ContactListStats extends Component
{
    public $refreshInterval = 30000; // 30 seconds

    protected $listeners = [
        'refreshStats' => '$refresh',
    ];

    public function placeholder()
    {
        return view('livewire.placeholders.stats-loading');
    }

    public function render()
    {
        $stats = [
            'total_lists' => ContactList::where('user_id', Auth::id())->count(),
            'active_lists' => ContactList::where('user_id', Auth::id())->active()->count(),
            'total_contacts' => Contact::where('user_id', Auth::id())->count(),
            'subscribed_contacts' => Contact::where('user_id', Auth::id())->subscribed()->count(),
            'recent_lists' => ContactList::where('user_id', Auth::id())
                ->where('created_at', '>=', now()->subDays(7))
                ->count(),
        ];

        // Calculate growth
        $previousWeekContacts = Contact::where('user_id', Auth::id())
            ->where('created_at', '<', now()->subDays(7))
            ->count();
        
        $stats['contact_growth'] = $previousWeekContacts > 0 
            ? round((($stats['total_contacts'] - $previousWeekContacts) / $previousWeekContacts) * 100, 1)
            : 0;

        // Top performing lists
        $topLists = ContactList::where('user_id', Auth::id())
            ->withCount(['subscribedContacts'])
            ->orderBy('subscribed_contacts_count', 'desc')
            ->limit(5)
            ->get();

        return view('livewire.contact-list-stats', [
            'stats' => $stats,
            'topLists' => $topLists,
        ]);
    }
}
