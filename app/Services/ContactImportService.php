<?php

namespace App\Services;

use App\Models\Contact;
use App\Models\ContactList;
use Illuminate\Support\Facades\Validator;
use League\Csv\Reader;

class ContactImportService {
    public function importFromCSV(int $userId, ContactList $contactList, string $filePath): array {
        $csv = Reader::createFromPath($filePath, 'r');
        $csv->setHeaderOffset(0);

        $records = $csv->getRecords();

        $imported = 0;
        $skipped = 0;
        $errors = [];

        foreach ($records as $offset => $record) {
            try {
                // Validate email
                $validator = Validator::make($record, [
                    'email' => 'required|email',
                ]);

                if ($validator->fails()) {
                    $skipped++;
                    $errors[] = "Row {$offset}: Invalid email - " . ($record['email'] ?? 'missing');
                    continue;
                }

                $contact = Contact::updateOrCreate(
                    [
                        'user_id' => $userId,
                        'email' => strtolower(trim($record['email'])),
                    ],
                    [
                        'first_name' => $record['first_name'] ?? null,
                        'last_name' => $record['last_name'] ?? null,
                        'status' => 'subscribed',
                        'subscribed_at' => now(),
                        'custom_fields' => $this->extractCustomFields($record),
                    ]
                );

                // Add to contact list
                $contactList->contacts()->syncWithoutDetaching([$contact->id]);
                $imported++;
            } catch (\Exception $e) {
                $skipped++;
                $errors[] = "Row {$offset}: " . $e->getMessage();
            }
        }

        $contactList->update([
            'total_contacts' => $contactList->contacts()->count()
        ]);

        return [
            'imported' => $imported,
            'skipped' => $skipped,
            'errors' => $errors,
        ];
    }

    public function importFromArray(int $userId, ContactList $contactList, array $contacts): array {
        $imported = 0;
        $skipped = 0;
        $errors = [];

        foreach ($contacts as $index => $contactData) {
            try {
                $validator = Validator::make($contactData, [
                    'email' => 'required|email',
                ]);

                if ($validator->fails()) {
                    $skipped++;
                    $errors[] = "Contact {$index}: Invalid data";
                    continue;
                }

                $contact = Contact::updateOrCreate([
                    'user_id' => $userId,
                    'email' => strtolower(trim($contactData['email'])),
                ],
                    [
                        'first_name' => $contactData['first_name'] ?? null,
                        'last_name' => $contactData['last_name'] ?? null,
                        'status' => 'subscribed',
                        'subscribed_at' => now(),
                        'custom_fields' => $contactData['custom_fields'] ?? null,
                    ]
                );

                $contactList->contacts()->syncWithoutDetaching([$contact->id]);
                $imported++;
            } catch (\Exception $e) {
                $skipped++;
                $errors[] = "Contact {$index}: " . $e->getMessage();
            }
        }

        $contactList->update([
            'total_contacts' => $contactList->contacts()->count()
        ]);

        return [
            'imported' => $imported,
            'skipped' => $skipped,
            'errors' => $errors,
        ];
    }

    private function extractCustomFields(array $record): ?array {
        $standardFields = ['email', 'first_name', 'last_name'];
        $customFields = array_diff_key($record, array_flip($standardFields));

        return !empty($customFields) ? $customFields : null;
    }
}
