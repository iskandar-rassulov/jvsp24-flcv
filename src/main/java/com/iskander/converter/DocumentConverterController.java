package com.iskander.converter;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/document")
public class DocumentConverterController {

    private static final Logger LOGGER = Logger.getLogger(DocumentConverterController.class.getName());

    @CrossOrigin(origins = "*")
    @PostMapping("/convert")
    public ResponseEntity<InputStreamResource> convertDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("format") String format) {
        File inputFile = null;
        File outputDir = null;

        try {
            // Проверка поддерживаемых форматов
            if (!isSupportedFormat(format)) {
                LOGGER.log(Level.WARNING, "Unsupported target document format: {0}", format);
                return ResponseEntity.badRequest().body(null);
            }

            // Ограничение на размер файла (50 MB)
            if (file.getSize() > 50 * 1024 * 1024) {
                LOGGER.log(Level.WARNING, "File size exceeds the limit of 50 MB");
                return ResponseEntity.badRequest().body(null);
            }

            inputFile = File.createTempFile("input_document", getExtension(file.getOriginalFilename()));
            file.transferTo(inputFile);

            outputDir = Files.createTempDirectory("document_conversion_").toFile();

            String convertParam = resolveConvertParam(format);
            if (convertParam == null) {
                LOGGER.log(Level.WARNING, "No conversion parameter found for format: {0}", format);
                return ResponseEntity.badRequest().body(null);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "soffice", "--headless", "--convert-to", convertParam,
                    "--outdir", outputDir.getAbsolutePath(),
                    inputFile.getAbsolutePath()
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            long startTime = System.currentTimeMillis();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.log(Level.INFO, line);
                }
            }

            int exitCode = process.waitFor();
            long endTime = System.currentTimeMillis();
            LOGGER.log(Level.INFO, "Document conversion completed in {0} ms", (endTime - startTime));

            if (exitCode != 0) {
                LOGGER.log(Level.SEVERE, "LibreOffice conversion failed with exit code {0}", exitCode);
                return ResponseEntity.status(500).body(null);
            }

            File convertedFile = findConvertedFile(outputDir, format);
            if (convertedFile == null || !convertedFile.exists()) {
                LOGGER.log(Level.SEVERE, "Converted file not found. Possibly this conversion is not supported directly.");
                return ResponseEntity.status(500).body(null);
            }

            byte[] fileBytes = Files.readAllBytes(convertedFile.toPath());
            InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(fileBytes));

            String mimeType = resolveMimeType(format);

            String originalFilename = file.getOriginalFilename();
            String baseName = removeExtension(originalFilename);
            String newFilename = baseName + "-converted." + format.toLowerCase();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + newFilename + "\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileBytes.length)
                    .body(resource);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during document conversion: {0}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        } finally {
            if (inputFile != null && inputFile.exists()) inputFile.delete();
            if (outputDir != null && outputDir.exists()) {
                for (File f : outputDir.listFiles()) {
                    f.delete();
                }
                outputDir.delete();
            }
        }
    }

    private boolean isSupportedFormat(String format) {
        return format.equalsIgnoreCase("pdf") ||
                format.equalsIgnoreCase("docx") ||
                format.equalsIgnoreCase("odt");
    }

    private String resolveMimeType(String format) {
        switch (format.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "odt":
                return "application/vnd.oasis.opendocument.text";
            default:
                return "application/octet-stream";
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".tmp";
        int lastIndex = filename.lastIndexOf(".");
        if (lastIndex == -1) {
            return ".tmp";
        }
        return filename.substring(lastIndex);
    }

    private String removeExtension(String filename) {
        if (filename == null) return "converted-file";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return filename;
        }
        return filename.substring(0, lastDot);
    }

    private String resolveConvertParam(String format) {
        // Попытка без фильтров для DOCX и ODT:
        switch (format.toLowerCase()) {
            case "pdf":
                return "pdf:writer_pdf_Export";
            case "docx":
                return "docx"; // Без фильтра
            case "odt":
                return "odt";  // Без фильтра
            default:
                return null;
        }
    }

    private File findConvertedFile(File outputDir, String format) {
        File[] files = outputDir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.getName().toLowerCase().endsWith("." + format.toLowerCase())) {
                return f;
            }
        }
        return null;
    }
}
