document.addEventListener("DOMContentLoaded", () => {
    const forms = document.querySelectorAll('form[data-enhanced-post="true"]');
    if (forms.length === 0) {
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");

    forms.forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const submitButton = form.querySelector('button[type="submit"]');
            const originalLabel = submitButton?.textContent;

            if (submitButton) {
                submitButton.disabled = true;
                submitButton.textContent = "Saving...";
            }

            try {
                const headers = {};
                if (csrfToken && csrfHeader) {
                    headers[csrfHeader] = csrfToken;
                }

                const response = await fetch(form.action, {
                    method: (form.method || "POST").toUpperCase(),
                    headers,
                    body: new FormData(form),
                    credentials: "same-origin",
                    redirect: "follow"
                });

                if (!response.ok) {
                    const text = await response.text();
                    throw new Error(text || `Request failed with status ${response.status}`);
                }

                if (response.redirected && response.url) {
                    window.location.href = response.url;
                    return;
                }

                window.location.reload();
            } catch (error) {
                const message = error.message?.includes("<!DOCTYPE")
                    ? "The form could not be submitted."
                    : error.message;
                if (window.showToast) {
                    window.showToast("error", "Save failed", message);
                } else {
                    window.alert(message);
                }
            } finally {
                if (submitButton) {
                    submitButton.disabled = false;
                    submitButton.textContent = originalLabel;
                }
            }
        });
    });
});
