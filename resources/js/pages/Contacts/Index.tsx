import { Head, Link, router } from '@inertiajs/react';
import { useState, FormEvent } from 'react';
import contactRoutes from '@/routes/contacts';

interface ContactList {
    id: string;
    name: string;
}

interface Contact {
    id: string;
    email: string;
    first_name: string | null;
    last_name: string | null;
    status: 'subscribed' | 'unsubscribed' | 'bounced' | 'complained';
    created_at: string;
    contact_lists?: ContactList[];
}

interface PaginationLink {
    url: string | null;
    label: string;
    active: boolean;
}

interface PaginatedContacts {
    data: Contact[];
    links: PaginationLink[];
    prev_page_url: string | null;
    next_page_url: string | null;
    from: number;
    to: number;
    total: number;
}

interface Filters {
    search?: string;
    status?: string;
}

interface Props {
    auth: {
        user: any;
    };
    contacts: PaginatedContacts;
    filters: Filters;
}

export default function ContactsIndex({ auth, contacts, filters }: Props) {
    const [search, setSearch] = useState<string>(filters.search || '');
    const [status, setStatus] = useState<string>(filters.status || '');

    const handleSearch = (e: FormEvent<HTMLFormElement>): void => {
        e.preventDefault();
        router.get(contactRoutes.index.url({ search, status }), {}, {
            preserveState: true,
            replace: true,
        });
    };

    const handleDelete = (id: string): void => {
        if (confirm('Are you sure you want to delete this contact?')) {
            router.delete(contactRoutes.destroy.url(id));
        }
    };

    const getStatusColor = (status: Contact['status']): string => {
        const colors: Record<Contact['status'], string> = {
            subscribed: 'bg-green-100 text-green-800',
            unsubscribed: 'bg-gray-100 text-gray-800',
            bounced: 'bg-red-100 text-red-800',
            complained: 'bg-yellow-100 text-yellow-800',
        };
        return colors[status] || 'bg-gray-100 text-gray-800';
    };

    return (
        <>
            <Head title="Contacts" />
            
            <div className="py-12">
                <div className="max-w-7xl mx-auto sm:px-6 lg:px-8">
                    {/* Header */}
                    <div className="mb-6 flex justify-between items-center">
                        <h1 className="text-3xl font-bold text-gray-900">Contacts</h1>
                        <Link
                            href={contactRoutes.create.url()}
                            className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
                        >
                            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                            </svg>
                            Add Contact
                        </Link>
                    </div>

                    {/* Search & Filter */}
                    <div className="bg-white rounded-lg shadow-sm p-4 mb-6">
                        <form onSubmit={handleSearch} className="flex gap-4">
                            <input
                                type="text"
                                placeholder="Search by email, first name, or last name..."
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                className="flex-1 border-gray-300 rounded-lg shadow-sm focus:border-blue-500 focus:ring focus:ring-blue-200"
                            />
                            <select
                                value={status}
                                onChange={(e) => setStatus(e.target.value)}
                                className="border-gray-300 rounded-lg shadow-sm focus:border-blue-500 focus:ring focus:ring-blue-200"
                            >
                                <option value="">All Status</option>
                                <option value="subscribed">Subscribed</option>
                                <option value="unsubscribed">Unsubscribed</option>
                                <option value="bounced">Bounced</option>
                                <option value="complained">Complained</option>
                            </select>
                            <button
                                type="submit"
                                className="px-6 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition"
                            >
                                Search
                            </button>
                            {(search || status) && (
                                <button
                                    type="button"
                                    onClick={() => {
                                        setSearch('');
                                        setStatus('');
                                        router.get(contactRoutes.index.url());
                                    }}
                                    className="px-4 py-2 text-gray-600 hover:text-gray-900"
                                >
                                    Clear
                                </button>
                            )}
                        </form>
                    </div>

                    {/* Table */}
                    <div className="bg-white rounded-lg shadow-sm overflow-hidden">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        Name
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        Email
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        Lists
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        Status
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        Created
                                    </th>
                                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        Actions
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {contacts.data.length === 0 ? (
                                    <tr>
                                        <td colSpan={6} className="px-6 py-12 text-center">
                                            <div className="text-gray-500">
                                                <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                                                </svg>
                                                <p className="mt-2 text-sm">No contacts found.</p>
                                                <Link href={contactRoutes.create.url()} className="mt-2 text-blue-600 hover:text-blue-800">
                                                    Add your first contact
                                                </Link>
                                            </div>
                                        </td>
                                    </tr>
                                ) : (
                                    contacts.data.map((contact) => (
                                        <tr key={contact.id} className="hover:bg-gray-50">
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                <div className="text-sm font-medium text-gray-900">
                                                    {contact.first_name || contact.last_name 
                                                        ? `${contact.first_name || ''} ${contact.last_name || ''}`.trim()
                                                        : <span className="text-gray-400">No name</span>
                                                    }
                                                </div>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                                                {contact.email}
                                            </td>
                                            <td className="px-6 py-4">
                                                <div className="flex flex-wrap gap-1">
                                                    {contact.contact_lists?.slice(0, 2).map((list) => (
                                                        <span
                                                            key={list.id}
                                                            className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800"
                                                        >
                                                            {list.name}
                                                        </span>
                                                    ))}
                                                    {contact.contact_lists && contact.contact_lists.length > 2 && (
                                                        <span className="text-xs text-gray-500">
                                                            +{contact.contact_lists.length - 2}
                                                        </span>
                                                    )}
                                                    {(!contact.contact_lists || contact.contact_lists.length === 0) && (
                                                        <span className="text-xs text-gray-400">No lists</span>
                                                    )}
                                                </div>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(contact.status)}`}>
                                                    {contact.status}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                {new Date(contact.created_at).toLocaleDateString()}
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                                <Link
                                                    href={contactRoutes.edit.url(contact.id)}
                                                    className="text-blue-600 hover:text-blue-900 mr-4"
                                                >
                                                    Edit
                                                </Link>
                                                <button
                                                    onClick={() => handleDelete(contact.id)}
                                                    className="text-red-600 hover:text-red-900"
                                                >
                                                    Delete
                                                </button>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>

                        {/* Pagination */}
                        {contacts.links.length > 3 && (
                            <div className="bg-gray-50 px-6 py-3 flex items-center justify-between border-t border-gray-200">
                                <div className="flex-1 flex justify-between sm:hidden">
                                    <Link
                                        href={contacts.prev_page_url || '#'}
                                        className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
                                    >
                                        Previous
                                    </Link>
                                    <Link
                                        href={contacts.next_page_url || '#'}
                                        className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
                                    >
                                        Next
                                    </Link>
                                </div>
                                <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                                    <div>
                                        <p className="text-sm text-gray-700">
                                            Showing <span className="font-medium">{contacts.from}</span> to <span className="font-medium">{contacts.to}</span> of{' '}
                                            <span className="font-medium">{contacts.total}</span> contacts
                                        </p>
                                    </div>
                                    <div>
                                        <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
                                            {contacts.links.map((link, index) => (
                                                <Link
                                                    key={index}
                                                    href={link.url || '#'}
                                                    className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                                                        link.active
                                                            ? 'z-10 bg-blue-50 border-blue-500 text-blue-600'
                                                            : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                                                    } ${!link.url && 'opacity-50 cursor-not-allowed'}`}
                                                    dangerouslySetInnerHTML={{ __html: link.label }}
                                                    preserveState
                                                />
                                            ))}
                                        </nav>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </>
    );
}