(function () {
  const sourceField = document.getElementById("htmlContent");
  const editor = document.getElementById("htmlEditor");
  const toolbar = document.getElementById("htmlToolbar");

  if (!sourceField || !editor || typeof Quill === "undefined") {
    return;
  }

  const quill = new Quill(editor, {
    theme: "snow",
    placeholder: "Start writing your email here. Use the toolbar for formatting and the merge field chips to personalize content.",
    modules: {
      toolbar: toolbar || true,
      history: {
        delay: 300,
        maxStack: 100,
        userOnly: true
      }
    }
  });

  let activeTarget = quill;

  const setEditorHtml = (html) => {
    quill.clipboard.dangerouslyPasteHTML(html || "");
  };

  const syncSourceFromEditor = () => {
    const html = quill.root.innerHTML.trim();
    sourceField.value = html === "<p><br></p>" ? "" : html;
  };

  setEditorHtml(sourceField.value);

  document.querySelectorAll(".js-token-target").forEach((element) => {
    element.addEventListener("focus", () => {
      activeTarget = element === editor ? quill : element;
    });
    element.addEventListener("click", () => {
      activeTarget = element === editor ? quill : element;
    });
  });

  quill.on("text-change", syncSourceFromEditor);
  quill.on("selection-change", (range) => {
    if (range) {
      activeTarget = quill;
    }
  });

  const insertTextAtCursor = (target, text) => {
    if (target === quill) {
      const range = quill.getSelection(true) || { index: quill.getLength(), length: 0 };
      quill.focus();
      quill.insertText(range.index, text, "user");
      quill.setSelection(range.index + text.length, 0, "user");
      syncSourceFromEditor();
      return;
    }

    if (target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement) {
      const start = target.selectionStart ?? target.value.length;
      const end = target.selectionEnd ?? target.value.length;
      target.setRangeText(text, start, end, "end");
      target.dispatchEvent(new Event("input", { bubbles: true }));
      target.focus();
      activeTarget = target;
      return;
    }

    const range = quill.getSelection(true) || { index: quill.getLength(), length: 0 };
    quill.focus();
    quill.insertText(range.index, text, "user");
    quill.setSelection(range.index + text.length, 0, "user");
    syncSourceFromEditor();
  };

  document.querySelectorAll("[data-token]").forEach((button) => {
    button.addEventListener("click", () => {
      insertTextAtCursor(activeTarget, button.dataset.token || "");
    });
  });

  const plainTextField = document.getElementById("plainTextContent");
  const copyToPlainButton = document.querySelector(".js-copy-to-plain");
  if (copyToPlainButton && plainTextField) {
    copyToPlainButton.addEventListener("click", () => {
      const text = quill.getText().replace(/\n{3,}/g, "\n\n").trim();
      plainTextField.value = text;
      plainTextField.dispatchEvent(new Event("input", { bubbles: true }));
      plainTextField.focus();
      activeTarget = plainTextField;
    });
  }

  const form = sourceField.closest("form");
  if (form) {
    form.addEventListener("submit", syncSourceFromEditor);
  }
})();
