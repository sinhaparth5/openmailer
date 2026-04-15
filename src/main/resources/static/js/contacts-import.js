document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("contactImportForm");
    if (!form) return;

    const fileInput = document.getElementById("file");
    const fileSummary = document.getElementById("fileSummary");
    const listInput = document.getElementById("listId");
    const skipDuplicatesInput = document.getElementById("skipDuplicates");
    const validateButton = document.getElementById("validateImportButton");
    const startButton = document.getElementById("startImportButton");
    const statusPanel = document.getElementById("importStatus");
    const importMessage = document.getElementById("importMessage");
    const jobStatus = document.getElementById("jobStatus");
    const jobRows = document.getElementById("jobRows");
    const jobImported = document.getElementById("jobImported");
    const jobErrors = document.getElementById("jobErrors");
    const jobErrorList = document.getElementById("jobErrorList");
    const validationRows = document.getElementById("validationRows");
    const validationValidRows = document.getElementById("validationValidRows");
    const validationInvalidRows = document.getElementById("validationInvalidRows");
    const validationMessage = document.getElementById("validationMessage");

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    let pollTimer = null;
    let validationPassed = false;

    fileInput.addEventListener("change", async () => {
        validationPassed = false;
        if (fileInput.files && fileInput.files[0]) {
            const file = fileInput.files[0];
            fileSummary.textContent = `${file.name} selected (${formatBytes(file.size)}).`;
            resetValidationSummary("File selected. Run validation before starting the import.");
        } else {
            fileSummary.textContent = "Only CSV is supported. The file must include an email column.";
            resetValidationSummary("Choose a file to validate it before starting the import.");
        }
    });

    validateButton?.addEventListener("click", async () => {
        if (!fileInput.files || fileInput.files.length === 0) {
            window.showToast?.("error", "No file selected", "Choose a CSV file before validating.");
            return;
        }

        await runValidation();
    });

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        if (!fileInput.files || fileInput.files.length === 0) {
            window.showToast?.("error", "No file selected", "Choose a CSV file before starting the import.");
            return;
        }

        if (!validationPassed) {
            await runValidation();
            if (!validationPassed) {
                return;
            }
        }

        const formData = new FormData();
        formData.append("file", fileInput.files[0]);
        formData.append("skipDuplicates", String(skipDuplicatesInput.checked));
        if (listInput?.value) {
            formData.append("listId", listInput.value);
        }

        const headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        try {
            setSubmittingState(true);
            const response = await fetch("/api/v1/contacts/import", {
                method: "POST",
                headers,
                body: formData
            });
            const payload = await response.json();

            if (!response.ok || !payload.success) {
                throw new Error(payload.error?.message || payload.message || "Import could not be started.");
            }

            const jobId = payload.data?.jobId;
            statusPanel.classList.remove("hidden");
            importMessage.textContent = payload.message || "Import started.";
            jobStatus.textContent = "PROCESSING";
            jobRows.textContent = validationRows.textContent === "-" ? "0" : validationRows.textContent;
            jobImported.textContent = "0";
            jobErrors.textContent = "0";
            jobErrorList.classList.add("hidden");
            jobErrorList.textContent = "";

            if (jobId) {
                if (pollTimer) clearInterval(pollTimer);
                pollTimer = setInterval(() => pollImportStatus(jobId), 2000);
                await pollImportStatus(jobId);
            }
        } catch (error) {
            window.showToast?.("error", "Import failed", error.message);
        } finally {
            setSubmittingState(false);
        }
    });

    async function pollImportStatus(jobId) {
        try {
            const response = await fetch(`/api/v1/contacts/import/${jobId}`);
            const payload = await response.json();
            if (!response.ok || !payload.success) {
                throw new Error(payload.error?.message || "Could not fetch import status.");
            }

            const job = payload.data;
            jobStatus.textContent = job.status || "UNKNOWN";
            jobRows.textContent = String(job.totalRows ?? 0);
            jobImported.textContent = String(job.importedCount ?? 0);
            jobErrors.textContent = String(job.errorCount ?? 0);

            if (Array.isArray(job.errors) && job.errors.length > 0) {
                jobErrorList.textContent = job.errors.join("\n");
                jobErrorList.classList.remove("hidden");
            }

            if (job.status === "COMPLETED" || job.status === "FAILED") {
                clearInterval(pollTimer);
                pollTimer = null;
                window.showToast?.(
                    job.status === "COMPLETED" ? "success" : "error",
                    job.status === "COMPLETED" ? "Import complete" : "Import failed",
                    job.status === "COMPLETED"
                        ? `${job.importedCount ?? 0} contacts imported.`
                        : "Check the import status panel for details."
                );
            }
        } catch (error) {
            clearInterval(pollTimer);
            pollTimer = null;
            window.showToast?.("error", "Import status failed", error.message);
        }
    }

    async function runValidation() {
        const formData = new FormData();
        formData.append("file", fileInput.files[0]);

        const headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        try {
            validateButton.disabled = true;
            validateButton.textContent = "Validating...";

            const response = await fetch("/api/v1/contacts/import/validate", {
                method: "POST",
                headers,
                body: formData
            });
            const payload = await response.json();

            if (!response.ok || !payload.success) {
                throw new Error(payload.error?.message || payload.message || "CSV validation failed.");
            }

            const result = payload.data;
            validationRows.textContent = String(result.totalRows ?? 0);
            validationValidRows.textContent = String(result.validRows ?? 0);
            validationInvalidRows.textContent = String(result.invalidEmails ?? 0);
            validationPassed = Boolean(result.valid);
            validationMessage.textContent = validationPassed
                ? `Validation passed. Duplicate rows in file: ${result.duplicatesInFile ?? 0}.`
                : (result.error || "Validation failed.");
            validationMessage.className = `mt-3 text-xs ${validationPassed ? "text-emerald-700" : "text-rose-700"}`;

            if (!validationPassed) {
                window.showToast?.("error", "Validation failed", validationMessage.textContent);
            }
        } catch (error) {
            validationPassed = false;
            resetValidationSummary(error.message, true);
            window.showToast?.("error", "Validation failed", error.message);
        } finally {
            validateButton.disabled = false;
            validateButton.textContent = "Validate CSV";
        }
    }

    function resetValidationSummary(message, isError = false) {
        validationRows.textContent = "-";
        validationValidRows.textContent = "-";
        validationInvalidRows.textContent = "-";
        validationMessage.textContent = message;
        validationMessage.className = `mt-3 text-xs ${isError ? "text-rose-700" : "text-slate-500"}`;
    }

    function setSubmittingState(isSubmitting) {
        startButton.disabled = isSubmitting;
        startButton.textContent = isSubmitting ? "Starting..." : "Start Import";
        validateButton.disabled = isSubmitting;
    }

    function formatBytes(bytes) {
        if (!bytes) return "0 B";
        const units = ["B", "KB", "MB", "GB"];
        let value = bytes;
        let unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex += 1;
        }
        return `${value.toFixed(value >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
    }
});
