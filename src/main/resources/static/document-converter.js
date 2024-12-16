document.getElementById("document-form").addEventListener("submit", async (e) => {
    e.preventDefault();

    console.log("Document conversion form submitted.");

    const fileInput = document.getElementById("document-input");
    const formatSelect = document.getElementById("document-format");
    const file = fileInput.files[0];
    const format = formatSelect.value;

    console.log("Selected document file:", file);
    console.log("Target document format:", format);

    if (!file) {
        alert("Please upload a document file.");
        console.error("No file provided for document conversion.");
        return;
    }

    if (file.size > 50 * 1024 * 1024) {
        alert("File size exceeds the limit of 50 MB.");
        console.error("File size exceeds limit:", file.size);
        return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("format", format);

    try {
        console.log("Sending document conversion request to server...");

        const response = await fetch("http://localhost:8080/api/document/convert", {
            method: "POST",
            body: formData,
        });

        if (response.ok) {
            console.log("Server response OK. Preparing document file download...");

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `converted.${format}`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);

            console.log("Document file downloaded successfully.");
        } else {
            console.error("Server responded with an error. Status:", response.status);
            alert("Error converting the document. Server responded with an error.");
        }
    } catch (error) {
        console.error("Error during the document conversion request:", error);
        alert("An error occurred while converting the document.");
    }
});

document.getElementById("document-input").addEventListener("change", (e) => {
    const file = e.target.files[0];
    const docPreview = document.getElementById("document-preview");
    const noPreviewText = document.getElementById("document-no-preview");

    console.log("Selected document file for preview:", file);

    if (file) {
        const extension = file.name.split('.').pop().toLowerCase();
        if (extension === 'pdf') {
            const url = URL.createObjectURL(file);
            docPreview.src = url;
            docPreview.hidden = false;
            noPreviewText.hidden = true;

            console.log("Document (PDF) preview updated successfully.");
        } else {
            // Для DOCX, ODT делаем просто "No preview available"
            docPreview.hidden = true;
            docPreview.src = "";
            noPreviewText.hidden = false;

            console.warn("No preview available for this document format (DOCX, ODT).");
        }
    } else {
        docPreview.hidden = true;
        docPreview.src = "";
        noPreviewText.hidden = false;

        console.warn("No file selected for document preview.");
    }
});
