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
@RequestMapping("/api")
public class ImageConverterController {

    private static final Logger LOGGER = Logger.getLogger(ImageConverterController.class.getName());

    @CrossOrigin(origins = "*") // Если необходимо раздавать запросы с других доменов
    @PostMapping("/convert")
    public ResponseEntity<InputStreamResource> convertImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("format") String format) {
        File inputFile = null;
        File outputFile = null;

        try {
            // Проверка поддерживаемых форматов
            if (!isSupportedFormat(format)) {
                LOGGER.log(Level.WARNING, "Unsupported target format: {0}", format);
                return ResponseEntity.badRequest().body(null);
            }

            // Ограничение на размер файла (50 MB)
            if (file.getSize() > 50 * 1024 * 1024) {
                LOGGER.log(Level.WARNING, "File size exceeds the limit of 50 MB");
                return ResponseEntity.badRequest().body(null);
            }

            // Сохранение исходного файла во временную директорию
            inputFile = File.createTempFile("input", ".tmp");
            file.transferTo(inputFile);

            // Создание выходного файла
            outputFile = File.createTempFile("output", "." + format);

            // Вызов FFmpeg для конвертации изображения
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg", "-y",      // -y для перезаписи без запроса
                    "-i", inputFile.getAbsolutePath(),
                    "-vf", "scale=1920:-1",
                    outputFile.getAbsolutePath()
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Логирование вывода FFmpeg
            long startTime = System.currentTimeMillis();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.log(Level.INFO, line);
                }
            }

            int exitCode = process.waitFor();
            long endTime = System.currentTimeMillis();
            LOGGER.log(Level.INFO, "FFmpeg process completed in {0} ms", (endTime - startTime));

            if (exitCode != 0) {
                LOGGER.log(Level.SEVERE, "FFmpeg process failed with exit code {0}", exitCode);
                return ResponseEntity.status(500).body(null);
            }

            // Читаем выходной файл в память, чтобы после можно было его удалить
            byte[] fileBytes = Files.readAllBytes(outputFile.toPath());
            InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(fileBytes));

            // Определяем MIME-тип результата
            String mimeType = resolveMimeType(format);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted." + format);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileBytes.length)
                    .body(resource);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during image conversion: {0}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        } finally {
            // Удаляем временные файлы
            if (inputFile != null && inputFile.exists()) inputFile.delete();
            if (outputFile != null && outputFile.exists()) outputFile.delete();
        }
    }

    private boolean isSupportedFormat(String format) {
        return format.equalsIgnoreCase("jpg") ||
                format.equalsIgnoreCase("jpeg") ||
                format.equalsIgnoreCase("png") ||
                format.equalsIgnoreCase("bmp") ||
                format.equalsIgnoreCase("tiff") ||
                format.equalsIgnoreCase("webp");
    }

    private String resolveMimeType(String format) {
        switch (format.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "bmp":
                return "image/bmp";
            case "tiff":
                return "image/tiff";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }
}
