// Alpine.js Component Functions
// This file contains reusable Alpine.js component functions

/**
 * Toast Manager Component
 * Manages toast notifications with auto-dismiss functionality
 */
function toastManager() {
    return {
        toasts: [],
        nextId: 1,

        addToast(type, title, message = '', duration = 5000) {
            const id = this.nextId++;
            const toast = { id, type, title, message, show: true };
            this.toasts.push(toast);

            if (duration > 0) {
                setTimeout(() => this.removeToast(id), duration);
            }
        },

        removeToast(id) {
            const index = this.toasts.findIndex(t => t.id === id);
            if (index > -1) {
                this.toasts[index].show = false;
                setTimeout(() => {
                    this.toasts.splice(index, 1);
                }, 300);
            }
        }
    };
}

/**
 * Table Selection Component
 * Manages table row selection with select all functionality
 */
function tableSelection() {
    return {
        selectedAll: false,
        selectedRows: [],

        toggleAll() {
            this.selectedAll = !this.selectedAll;
            const checkboxes = document.querySelectorAll('.row-checkbox');
            checkboxes.forEach(checkbox => {
                checkbox.checked = this.selectedAll;
            });
            this.updateSelection();
        },

        updateSelection() {
            const checkboxes = document.querySelectorAll('.row-checkbox');
            this.selectedAll = Array.from(checkboxes).every(cb => cb.checked);
            this.selectedRows = Array.from(checkboxes).filter(cb => cb.checked).map(cb => cb.value);
        }
    };
}

/**
 * Global helper function to show toast notifications
 * Can be called from anywhere: showToast('success', 'Title', 'Message')
 */
window.showToast = function(type, title, message = '', duration = 5000) {
    const event = new CustomEvent('show-toast', {
        detail: { type, title, message, duration }
    });
    window.dispatchEvent(event);
};

// Listen for toast events and dispatch to the toast container
window.addEventListener('show-toast', (event) => {
    const container = document.querySelector('[x-data*="toastManager"]');
    if (container && container.__x) {
        container.__x.$data.addToast(
            event.detail.type,
            event.detail.title,
            event.detail.message,
            event.detail.duration
        );
    }
});
