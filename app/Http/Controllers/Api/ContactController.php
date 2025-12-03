<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Contact;
use App\Models\ContactList;
use App\Services\ContactImportService;
use Illuminate\Http\Request;
use Inertia\Inertia;

class ContactController extends Controller {
    protected ContactImportService $importService;

    public function __construct(ContactImportService $importService)
    {
        $this->importService = $importService;
    }

    // ===================== INERTIA METHODS (WEB) ===================

    /**
     * Display contacts list page (Inertia)
     */
    public function indexWeb(Request $request): \Inertia\Response
    {
        $contacts = Contact::where('user_id', $request->user()->id)
            ->with('contactLists:id,name')
            ->when($request->search, function ($query, $search) {
                $query->where(function ($q) use ($search) {
                    $q->where('email', 'like', "%{$search}%")
                        ->orWhere('first_name', 'like', "%{$search}%")
                        ->orWhere('last_name', 'like', "%{$search}%");
                });
            })
            ->when($request->status, function ($query, $status) {
                $query->where('status', $status);
            })
            ->latest()
            ->paginate(20)
            ->withQueryString();

        return Inertia::render('Contacts/Index', [
            'contacts' => $contacts,
            'filters' => $request->only(['search', 'status']),
        ]);
    }

    /**
     * Show create contact form (Inertia)
     */
    public function createWeb(): \Inertia\Response
    {
        $contactLists = ContactList::where('user_id', auth()->id())
            ->select('id', 'name', 'total_contacts')
            ->get();

        return Inertia::render('Contacts/Create', [
            'contactLists' => $contactLists,
        ]);
    }

    /**
     * Store new contact (Inertia)
     */
    public function storeWeb(Request $request)
    {
        $validated = $request->validate([
            'email' => 'required|email|unique:contacts,email,NULL,id,user_id,' . $request->user()->id,
            'first_name' => 'nullable|string|max:100',
            'last_name' => 'nullable|string|max:100',
            'custom_fields' => 'nullable|array',
            'contact_list_ids' => 'nullable|array',
            'contact_list_ids.*' => 'exists:contact_lists,id',
        ]);

        $contact = Contact::create([
            'user_id' => $request->user()->id,
            'email' => strtolower($validated['email']),
            'first_name' => $validated['first_name'] ?? null,
            'last_name' => $validated['last_name'] ?? null,
            'custom_fields' => $validated['custom_fields'] ?? null,
            'status' => 'subscribed',
            'subscribed_at' => now(),
        ]);

        // Attach to contact lists and update counts
        if (!empty($validated['contact_list_ids'])) {
            $contact->contactLists()->sync($validated['contact_list_ids']);

            foreach ($validated['contact_list_ids'] as $listId) {
                $list = ContactList::find($listId);
                if ($list) {
                    $list->update(['total_contacts' => $list->contacts()->count()]);
                }
            }
        }

        return redirect()->route('contacts.index')
            ->with('success', 'Contact created successfully!');
    }

    /**
     * Show edit contact form (Inertia)
     */
    public function editWeb(Contact $contact): \Inertia\Response
    {
        if ($contact->user_id !== auth()->id()) {
            abort(403, 'Unauthorized');
        }

        $contactLists = ContactList::where('user_id', auth()->id())
            ->select('id', 'name', 'total_contacts')
            ->get();

        return Inertia::render('Contacts/Edit', [
            'contact' => $contact->load('contactLists:id,name'),
            'contactLists' => $contactLists,
        ]);
    }

    /**
     * Update contact (Inertia)
     */
    public function updateWeb(Request $request, Contact $contact)
    {
        if ($contact->user_id !== $request->user()->id) {
            abort(403, 'Unauthorized');
        }

        $validated = $request->validate([
            'email' => 'required|email|unique:contacts,email,' . $contact->id . ',id,user_id,' . $request->user()->id,
            'first_name' => 'nullable|string|max:100',
            'last_name' => 'nullable|string|max:100',
            'custom_fields' => 'nullable|array',
            'status' => 'required|in:subscribed,unsubscribed,bounced,complained',
            'contact_list_ids' => 'nullable|array',
            'contact_list_ids.*' => 'exists:contact_lists,id',
        ]);

        $contact->update([
            'email' => strtolower($validated['email']),
            'first_name' => $validated['first_name'] ?? null,
            'last_name' => $validated['last_name'] ?? null,
            'custom_fields' => $validated['custom_fields'] ?? null,
            'status' => $validated['status'],
        ]);

        // Update contact lists
        if (isset($validated['contact_list_ids'])) {
            $contact->contactLists()->sync($validated['contact_list_ids']);
        }

        return redirect()->route('contacts.index')
            ->with('success', 'Contact updated successfully!');
    }

    /**
     * Delete contact (Inertia)
     */
    public function destroyWeb(Contact $contact)
    {
        if ($contact->user_id !== auth()->id()) {
            abort(403, 'Unauthorized');
        }

        $contact->delete();

        return redirect()->route('contacts.index')
            ->with('success', 'Contact deleted successfully!');
    }

    // ===================== API METHODS (JSON) ===================

    /**
     * Get contacts list (API)
     */
    public function index(Request $request)
    {
        $contacts = Contact::where('user_id', $request->user()->id)
            ->with('contactLists')
            ->when($request->search, function ($query, $search) {
                $query->where(function ($q) use ($search) {
                    $q->where('email', 'like', "%{$search}%")
                        ->orWhere('first_name', 'like', "%{$search}%")
                        ->orWhere('last_name', 'like', "%{$search}%");
                });
            })
            ->latest()
            ->paginate(50);

        return response()->json($contacts);
    }

    /**
     * Create contact (API)
     */
    public function store(Request $request)
    {
        $validated = $request->validate([
            'email' => 'required|email|unique:contacts,email,NULL,id,user_id,' . $request->user()->id,
            'first_name' => 'nullable|string|max:100',
            'last_name' => 'nullable|string|max:100',
            'custom_fields' => 'nullable|array',
            'contact_list_ids' => 'nullable|array',
            'contact_list_ids.*' => 'exists:contact_lists,id',
        ]);

        $contact = Contact::create([
            'user_id' => $request->user()->id,
            'email' => strtolower($validated['email']),
            'first_name' => $validated['first_name'] ?? null,
            'last_name' => $validated['last_name'] ?? null,
            'custom_fields' => $validated['custom_fields'] ?? null,
            'status' => 'subscribed',
            'subscribed_at' => now(),
        ]);

        if (isset($validated['contact_list_ids'])) {
            $contact->contactLists()->sync($validated['contact_list_ids']);
        }

        return response()->json([
            'message' => 'Contact created successfully',
            'contact' => $contact->load('contactLists')
        ], 201);
    }

    /**
     * Get single contact (API)
     */
    public function show(Request $request, Contact $contact)
    {
        if ($contact->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        return response()->json($contact->load(['contactLists', 'emailEvents']));
    }

    /**
     * Update contact (API)
     */
    public function update(Request $request, Contact $contact)
    {
        if ($contact->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $validated = $request->validate([
            'email' => 'sometimes|email',
            'first_name' => 'nullable|string|max:100',
            'last_name' => 'nullable|string|max:100',
            'custom_fields' => 'nullable|array',
            'status' => 'sometimes|in:subscribed,unsubscribed,bounced,complained',
            'contact_list_ids' => 'nullable|array',
            'contact_list_ids.*' => 'exists:contact_lists,id',
        ]);

        $contact->update($validated);

        if (isset($validated['contact_list_ids'])) {
            $contact->contactLists()->sync($validated['contact_list_ids']);
        }

        return response()->json([
            'message' => 'Contact updated successfully',
            'contact' => $contact->fresh()->load('contactLists')
        ]);
    }

    /**
     * Delete contact (API)
     */
    public function destroy(Request $request, Contact $contact)
    {
        if ($contact->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $contact->delete();

        return response()->json(['message' => 'Contact deleted successfully']);
    }

    // ===================== CONTACT LISTS (API) ===================

    /**
     * Get contact lists (API)
     */
    public function indexLists(Request $request)
    {
        $lists = ContactList::where('user_id', $request->user()->id)
            ->withCount('contacts')
            ->latest()
            ->paginate(20);

        return response()->json($lists);
    }

    /**
     * Create contact list (API)
     */
    public function storeList(Request $request)
    {
        $validated = $request->validate([
            'name' => 'required|string|max:255',
            'description' => 'nullable|string',
        ]);

        $list = ContactList::create([
            'user_id' => $request->user()->id,
            'name' => $validated['name'],
            'description' => $validated['description'] ?? null,
        ]);

        return response()->json([
            'message' => 'Contact list created successfully',
            'list' => $list
        ], 201);
    }

    /**
     * Get single contact list (API)
     */
    public function showList(Request $request, ContactList $contactList)
    {
        if ($contactList->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        return response()->json($contactList->load('contacts'));
    }

    /**
     * Update contact list (API)
     */
    public function updateList(Request $request, ContactList $contactList)
    {
        if ($contactList->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $validated = $request->validate([
            'name' => 'sometimes|string|max:255',
            'description' => 'nullable|string',
        ]);

        $contactList->update($validated);

        return response()->json([
            'message' => 'Contact list updated successfully',
            'list' => $contactList
        ]);
    }

    /**
     * Delete contact list (API)
     */
    public function destroyList(Request $request, ContactList $contactList)
    {
        if ($contactList->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $contactList->delete();

        return response()->json(['message' => 'Contact list deleted successfully']);
    }

    // ===================== IMPORT (API) ===================

    /**
     * Import contacts from CSV (API)
     */
    public function import(Request $request, ContactList $contactList)
    {
        if ($contactList->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $validated = $request->validate([
            'file' => 'required|file|mimes:csv,txt|max:10240',
        ]);

        $filePath = $validated['file']->getRealPath();

        $result = $this->importService->importFromCSV(
            $request->user()->id,
            $contactList,
            $filePath
        );

        return response()->json([
            'message' => 'Import completed',
            'result' => $result
        ]);
    }
}
