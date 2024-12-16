document.getElementById("video-form").addEventListener("submit", async (e) => {
    e.preventDefault();

    console.log("Video conversion form submitted.");

    const fileInput = document.getElementById("video-input");
    const formatSelect = document.getElementById("video-format");
    const file = fileInput.files[0];
    const format = formatSelect.value;

    console.log("Selected video file:", file);
    console.log("Target video format:", format);

    // Проверка наличия файла
    if (!file) {
        alert("Please upload a video file.");
        console.error("No file provided for video conversion.");
        return;
    }

    // Проверка размера файла (200 MB лимит для примера)
    if (file.size > 200 * 1024 * 1024) {
        alert("File size exceeds the limit of 200 MB.");
        console.error("File size exceeds the limit: ", file.size);
        return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("format", format);

    try {
        console.log("Sending video conversion request to server...");

        const response = await fetch("http://localhost:8080/api/video/convert", {
            method: "POST",
            body: formData,
        });

        if (response.ok) {
            console.log("Server response OK. Preparing video file download...");

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `converted.${format}`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);

            console.log("Video file downloaded successfully.");
        } else {
            console.error("Server responded with an error. Status:", response.status);
            alert("Error converting the video. Server responded with an error.");
        }
    } catch (error) {
        console.error("Error during the video conversion request:", error);
        alert("An error occurred while converting the video.");
    }
});
