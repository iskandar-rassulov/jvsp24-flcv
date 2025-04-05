document.getElementById("audio-form").addEventListener("submit", async (e) => {
    e.preventDefault();

    console.log("Audio conversion form submitted.");

    const fileInput = document.getElementById("audio-input");
    const formatSelect = document.getElementById("audio-format");
    const file = fileInput.files[0];
    const format = formatSelect.value;

    console.log("Selected file:", file);
    console.log("Target format:", format);

    // Проверка наличия файла
    if (!file) {
        alert("Please upload a file.");
        console.error("No file provided for audio conversion.");
        return;
    }

    // Проверка размера файла (50 MB лимит)
    if (file.size > 50 * 1024 * 1024) { // 50 MB
        alert("File size exceeds the limit of 50 MB.");
        console.error("File size exceeds the limit: ", file.size);
        return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("format", format);

    try {
        console.log("Sending request to server...");

        const response = await fetch("http://localhost:8080/api/audio/convert", {
            method: "POST",
            body: formData,
        });

        if (response.ok) {
            console.log("Server response OK. Preparing file download...");

            const blob = await response.blob();
            const contentDisposition = response.headers.get('Content-Disposition');
            let fileName = `converted.${format}`; // Default name if header is not present

            if (contentDisposition) {
                const fileNameMatch = contentDisposition.match(/filename="(.+)"/);
                if (fileNameMatch && fileNameMatch.length === 2) {
                    fileName = fileNameMatch[1];
                    console.log("Filename extracted from header:", fileName);
                } else {
                    console.warn("Filename not found in Content-Disposition header.");
                }
            } else {
                console.warn("Content-Disposition header is missing.");
            }

            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = fileName;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);

            console.log("File downloaded successfully.");
        } else {
            console.error("Server responded with an error. Status:", response.status);
            alert("Error converting the audio. Server responded with an error.");
        }
    } catch (error) {
        console.error("Error during the request:", error);
        alert("An error occurred while converting the audio.");
    }
});
