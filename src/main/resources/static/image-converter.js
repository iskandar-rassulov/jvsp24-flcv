document.getElementById("image-form").addEventListener("submit", async (e) => {
    e.preventDefault();

    console.log("Image conversion form submitted.");

    const fileInput = document.getElementById("image-input");
    const formatSelect = document.getElementById("output-format");
    const file = fileInput.files[0];
    const format = formatSelect.value;

    console.log("Selected file:", file);
    console.log("Target format:", format);

    if (!file) {
        alert("Please upload a file.");
        console.error("No file provided for image conversion.");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("format", format);

    try {
        console.log("Sending request to server...");

        const response = await fetch("http://localhost:8080/api/convert", {
            method: "POST",
            body: formData,
        });

        if (response.ok) {
            console.log("Server response OK. Preparing file download...");

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `converted.${format}`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);

            console.log("File downloaded successfully.");
        } else {
            console.error("Server responded with an error. Status:", response.status);
            alert("Error converting the image. Server responded with an error.");
        }
    } catch (error) {
        console.error("Error during the request:", error);
        alert("An error occurred while converting the image.");
    }
});

document.getElementById("image-input").addEventListener("change", (e) => {
    const file = e.target.files[0];
    const preview = document.getElementById("image-preview");
    const noPreviewText = document.getElementById("no-preview");

    console.log("Selected file for preview:", file);

    if (file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            preview.src = e.target.result;
            preview.hidden = false;
            noPreviewText.hidden = true;

            console.log("Preview updated successfully.");
        };
        reader.readAsDataURL(file);
    } else {
        preview.hidden = true;
        noPreviewText.hidden = false;

        console.warn("No file selected for preview.");
    }
});
