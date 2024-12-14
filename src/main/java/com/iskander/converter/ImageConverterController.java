package com.iskander.converter;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

@RestController
@RequestMapping("/api")
public class ImageConverterController {

    @PostMapping("/convert")
    public ResponseEntity<InputStreamResource> convertImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("format") String format) {
        try {
            // Проверяем, поддерживается ли целевой формат
            if (!ImageIO.getImageWritersByFormatName(format).hasNext()) {
                System.out.println("Unsupported format: " + format);
                return ResponseEntity.badRequest().body(null);
            }

            // Читаем исходный файл
            File inputFile = File.createTempFile("input", getExtension(file.getOriginalFilename()));
            file.transferTo(inputFile);
            BufferedImage inputImage = ImageIO.read(inputFile);
            if (inputImage == null) {
                System.out.println("Unsupported input file format.");
                return ResponseEntity.badRequest().body(null);
            }

            // Создаём временный файл для результата
            File outputFile = File.createTempFile("output", "." + format);
            ImageIO.write(inputImage, format, outputFile);

            // Отправляем файл клиенту
            InputStreamResource resource = new InputStreamResource(new FileInputStream(outputFile));
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted." + format);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            // Удаляем временные файлы
            inputFile.delete();
            outputFile.delete();

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(outputFile.length())
                    .body(resource);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    private String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf("."));
    }
}
