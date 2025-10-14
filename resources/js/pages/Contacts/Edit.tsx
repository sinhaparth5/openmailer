import { Head, Link, useForm } from '@inertiajs/react';
import { useState, FormEvent } from 'react';
import contactRoutes from '@/routes/contacts';

interface ContactList {
    id: string;
    name: string;
    total_contacts?: number;
}

interface Contact {
    id: string;
    email: string;
    first_name: string | null;
    last_name: string | null;
    status: 'subscribed' | 'unsubscribed' | 'bounced' | 'complained';
    custom_fields: Record<string, string> | null;
    contact_lists?: ContactList[];
}

interface FormData {
    email: string;
    first_name: string;
    last_name: string;
    status: Contact['status'];
    contact_list_ids: string[];
    custom_fields: Record<string, string>;
}

interface Props {
    auth: {
        user: any;
    };
    contact: Contact;
    contactLists: ContactList[];
}

export default function ContactsEdit({ auth, contact, contactLists }: Props) {
    const { data, setData, put, processing, errors } = useForm<FormData>({
        email: contact.email || '',
        first_name: contact.first_name || '',
        last_name: contact.last_name || '',
        status: contact.status || 'subscribed',
        contact_list_ids: contact.contact_lists?.map(list => list.id) || [],
        custom_fields: contact.custom_fields || {},
    });

    const [customFieldKey, setCustomFieldKey] = useState<string>('');
    const [customFieldValue, setCustomFieldValue] = useState<string>('');

    const handleSubmit = (e: FormEvent<HTMLFormElement>): void => {
        e.preventDefault();
        put(contactRoutes.update.url(contact.id));
    };

    const toggleList = (listId: string): void => {
        if (data.contact_list_ids.includes(listId)) {
            setData('contact_list_ids', data.contact_list_ids.filter(id => id !== listId));
        } else {
            setData('contact_list_ids', [...data.contact_list_ids, listId]);
        }
    };

    const addCustomField = (): void => {
        if (customFieldKey.trim() && customFieldValue.trim()) {
            setData('custom_fields', {
                ...data.custom_fields,
                [customFieldKey]: customFieldValue
            });
            setCustomFieldKey('');
            setCustomFieldValue('');
        }
    };

    const removeCustomField = (key: string): void => {
        const newFields = { ...data.custom_fields };
        delete newFields[key];
        setData('custom_fields', newFields);
    };

    return (
        <>
            <Head title="Edit Contact" />
            
            <div className="py-12">
                <div className="max-w-3xl mx-auto sm:px-6 lg:px-8">
                    {/* Header */}
                    <div className="mb-6 flex justify-between items-center">
                        <h1 className="text-3xl font-bold text-gray-900">Edit Contact</h1>
                        <Link
                            href={contactRoutes.index.url()}
                            className="text-gray-600 hover:text-gray-900 flex items-center"
                        >
                            <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                            </svg>
                            Back to Contacts
                        </Link>
                    </div>

                    {/* Form Card */}
                    <div className="bg-white rounded-lg shadow-sm p-6">
                        <form onSubmit={handleSubmit} className="space-y-6">
                            {/* Email */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">
                                    Email Address <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="email"
                                    value={data.email}
                                    onChange={(e) => setData('email', e.target.value)}
                                    className={`w-full border ${errors.email ? 'border-red-300' : 'border-gray-300'} rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent`}
                                    placeholder="john@example.com"
                                    required
                                />
                                {errors.email && (
                                    <p className="mt-1 text-sm text-red-600">{errors.email}</p>
                                )}
                            </div>

                            {/* Name Fields */}
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                        First Name
                                    </label>
                                    <input
                                        type="text"
                                        value={data.first_name}
                                        onChange={(e) => setData('first_name', e.target.value)}
                                        className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                        placeholder="John"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                        Last Name
                                    </label>
                                    <input
                                        type="text"
                                        value={data.last_name}
                                        onChange={(e) => setData('last_name', e.target.value)}
                                        className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                        placeholder="Doe"
                                    />
                                </div>
                            </div>

                            {/* Status */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">
                                    Status <span className="text-red-500">*</span>
                                </label>
                                <select
                                    value={data.status}
                                    onChange={(e) => setData('status', e.target.value as Contact['status'])}
                                    className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    required
                                >
                                    <option value="subscribed">Subscribed</option>
                                    <option value="unsubscribed">Unsubscribed</option>
                                    <option value="bounced">Bounced</option>
                                    <option value="complained">Complained</option>
                                </select>
                                {errors.status && (
                                    <p className="mt-1 text-sm text-red-600">{errors.status}</p>
                                )}
                            </div>

                            {/* Contact Lists */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">
                                    Contact Lists
                                </label>
                                <div className="border border-gray-300 rounded-lg p-4 max-h-48 overflow-y-auto bg-gray-50">
                                    {contactLists.length === 0 ? (
                                        <p className="text-sm text-gray-500">No contact lists available.</p>
                                    ) : (
                                        <div className="space-y-2">
                                            {contactLists.map((list) => (
                                                <label key={list.id} className="flex items-center p-2 hover:bg-white rounded cursor-pointer transition">
                                                    <input
                                                        type="checkbox"
                                                        checked={data.contact_list_ids.includes(list.id)}
                                                        onChange={() => toggleList(list.id)}
                                                        className="h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                                                    />
                                                    <span className="ml-3 text-sm text-gray-700">
                                                        {list.name}
                                                        <span className="text-gray-400 ml-2">
                                                            ({list.total_contacts || 0} contacts)
                                                        </span>
                                                    </span>
                                                </label>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* Custom Fields */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">
                                    Custom Fields
                                </label>
                                
                                {/* Display existing custom fields */}
                                {Object.keys(data.custom_fields).length > 0 && (
                                    <div className="mb-3 space-y-2">
                                        {Object.entries(data.custom_fields).map(([key, value]) => (
                                            <div key={key} className="flex items-center gap-2 bg-blue-50 p-3 rounded-lg">
                                                <div className="flex-1">
                                                    <span className="text-sm font-medium text-gray-700">{key}:</span>
                                                    <span className="text-sm text-gray-600 ml-2">{value}</span>
                                                </div>
                                                <button
                                                    type="button"
                                                    onClick={() => removeCustomField(key)}
                                                    className="text-red-600 hover:text-red-800 text-sm font-medium"
                                                >
                                                    Remove
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                )}

                                {/* Add new custom field */}
                                <div className="flex gap-2">
                                    <input
                                        type="text"
                                        value={customFieldKey}
                                        onChange={(e) => setCustomFieldKey(e.target.value)}
                                        className="flex-1 border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                        placeholder="Field name (e.g., company)"
                                    />
                                    <input
                                        type="text"
                                        value={customFieldValue}
                                        onChange={(e) => setCustomFieldValue(e.target.value)}
                                        className="flex-1 border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                        placeholder="Field value"
                                    />
                                    <button
                                        type="button"
                                        onClick={addCustomField}
                                        className="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition"
                                    >
                                        Add
                                    </button>
                                </div>
                            </div>

                            {/* Form Actions */}
                            <div className="flex items-center justify-end gap-3 pt-4 border-t">
                                <Link
                                    href={contactRoutes.index.url()}
                                    className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition"
                                >
                                    Cancel
                                </Link>
                                <button
                                    type="submit"
                                    disabled={processing}
                                    className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
                                >
                                    {processing ? (
                                        <span className="flex items-center">
                                            <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                            </svg>
                                            Updating...
                                        </span>
                                    ) : (
                                        'Update Contact'
                                    )}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </>
    );
}