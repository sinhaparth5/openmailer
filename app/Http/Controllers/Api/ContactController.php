<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Contact;
use App\Models\ContactList;
use App\Services\ContactImportService;
use Illuminate\Http\Request;

class ContactController extends Controller {
    protected ContactImportService $importService;

    public function __construct(ContactImportService $importService)
    {
        $this->importService = $importService;
    }

    // Contact Lists
    public function indexLists(Request $request)
    {
        $lists = ContactList::where('user_id', $request->user()->id)
            ->withCount('contacts')
            ->latest()
            ->paginate(20);

        return response()->json($lists);
    }

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

    public function showList(Request $request, ContactList $contactList)
    {
        if ($contactList->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        return response()->json($contactList->load('contacts'));
    }

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

    public function destroyList(Request $request, ContactList $contactList)
    {
        if ($contactList->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $contactList->delete();

        return response()->json(['message' => 'Contact list deleted successfully']);
    }

    // Contacts
    public function index(Request $request)
    {
        $contacts = Contact::where('user_id', $request->user()->id)
            ->with('contactLists')
            ->latest()
            ->paginate(50);

        return response()->json($contacts);
    }

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

        // Add to lists
        if (isset($validated['contact_list_ids'])) {
            $contact->contactLists()->sync($validated['contact_list_ids']);
        }

        return response()->json([
            'message' => 'Contact created successfully',
            'contact' => $contact->load('contactLists')
        ], 201);
    }

    public function show(Request $request, Contact $contact)
    {
        if ($contact->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        return response()->json($contact->load(['contactLists', 'emailEvents']));
    }

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
        ]);

        $contact->update($validated);

        return response()->json([
            'message' => 'Contact updated successfully',
            'contact' => $contact
        ]);
    }

    public function destroy(Request $request, Contact $contact)
    {
        if ($contact->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $contact->delete();

        return response()->json(['message' => 'Contact deleted successfully']);
    }

    // Import contacts
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
