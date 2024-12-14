document.getElementById("image-form").addEventListener("submit", async (e) => {
    e.preventDefault();

    const fileInput = document.getElementById("image-input");
    const formatSelect = document.getElementById("output-format");
    const file = fileInput.files[0];
    const format = formatSelect.value;

    if (!file) {
        alert("Please upload a file.");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("format", format);

    try {
        const response = await fetch("http://localhost:8080/api/convert", {
            method: "POST",
            body: formData,
        });

        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `converted.${format}`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
        } else {
            alert("Error converting the image.");
        }
    } catch (error) {
        console.error("Error:", error);
        alert("An error occurred while converting the image.");
    }
});

// Preview logic
document.getElementById("image-input").addEventListener("change", (e) => {
    const file = e.target.files[0];
    const preview = document.getElementById("image-preview");
    const noPreviewText = document.getElementById("no-preview");

    if (file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            preview.src = e.target.result;
            preview.hidden = false;
            noPreviewText.hidden = true;
        };
        reader.readAsDataURL(file);
    } else {
        preview.hidden = true;
        noPreviewText.hidden = false;
    }
});
