# File Converter Web Application

A simple web-based file conversion tool powered by Spring Boot and FFmpeg/LibreOffice. This application allows you to convert images, audio, video, and documents to various formats right from your browser.

![converterscreen1](https://github.com/user-attachments/assets/211ab2de-264d-43bf-9ba2-785ad9c2faa0)

## Features

- **Image Conversion**:  
  Convert between JPG, PNG, BMP, TIFF and WEBP.  
  Preview your image before conversion.

- **Audio Conversion**:  
  Convert between MP3, WAV, AAC, FLAC and OGG.

- **Video Conversion**:  
  Convert between MP4, MKV, MOV, AVI and WEBM.  
  Preview your video before conversion.

- **Document Conversion**:  
  Convert between PDF, DOCX and ODT.  
  If you select a PDF file, you can preview it directly in the browser.

## Technologies Used

- **Backend**:  
  - Java, Spring Boot  
  - FFmpeg for image/video/audio conversions  
  - LibreOffice (via `soffice`) for document conversions

- **Frontend**:  
  - HTML, CSS, JavaScript (Vanilla)  
  - Simple and responsive UI with file previews where possible

## Prerequisites

- **Java**: Ensure Java (version 17 or higher) is installed.
- **FFmpeg**: Download and install [FFmpeg](https://ffmpeg.org/download.html) and ensure it’s accessible in your PATH.
- **LibreOffice**: Download and install [LibreOffice](https://www.libreoffice.org/) and ensure `soffice` is accessible from the command line.

