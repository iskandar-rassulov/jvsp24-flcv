document.getElementById("video-form").addEventListener("submit", async (e) => {
    e.preventDefault();

    console.log("Video conversion form submitted.");

    const fileInput = document.getElementById("video-input");
    const formatSelect = document.getElementById("video-format");
    const file = fileInput.files[0];
    const format = formatSelect.value;

    console.log("Selected video file:", file);
    console.log("Target video format:", format);

    if (!file) {
        alert("Please upload a video file.");
        console.error("No file provided for video conversion.");
        return;
    }

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

document.getElementById("video-input").addEventListener("change", (e) => {
    const file = e.target.files[0];
    const videoPreview = document.getElementById("video-preview");
    const noPreviewText = document.getElementById("video-no-preview");

    console.log("Selected file for video preview:", file);

    if (file) {
        const url = URL.createObjectURL(file);
        videoPreview.src = url;
        videoPreview.hidden = false;
        noPreviewText.hidden = true;

        console.log("Video preview updated successfully.");
    } else {
        videoPreview.hidden = true;
        noPreviewText.hidden = false;
        videoPreview.src = "";

        console.warn("No file selected for video preview.");
    }
});
