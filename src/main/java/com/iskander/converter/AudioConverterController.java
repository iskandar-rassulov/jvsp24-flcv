package com.iskander.converter;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.sound.sampled.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/audio")
public class AudioConverterController {

    private static final Logger LOGGER = Logger.getLogger(AudioConverterController.class.getName());

    @PostMapping("/convert")
    public ResponseEntity<InputStreamResource> convertAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "format", required = false, defaultValue = "mp3") String format) {
        LOGGER.log(Level.INFO, "Received request to convert audio file to format: {0}", format);

        if (!format.equalsIgnoreCase("mp3") && !format.equalsIgnoreCase("wav")) {
            LOGGER.log(Level.WARNING, "Unsupported target format: {0}", format);
            String errorMessage = "Invalid format. Supported formats are: MP3, WAV.";
            LOGGER.log(Level.WARNING, errorMessage);
            return ResponseEntity.badRequest().body(null);
        }

        File inputFile = null;
        File outputFile = null;

        try {
            // Save uploaded file to a temporary location
            inputFile = File.createTempFile("audio_input", getExtension(file.getOriginalFilename()));
            try {
                file.transferTo(inputFile);
                LOGGER.log(Level.INFO, "Uploaded file saved to temporary location: {0}", inputFile.getAbsolutePath());
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Failed to transfer uploaded file to temporary location: {0}", ex.getMessage());
                throw ex;
            }

            // Determine output format and convert
            outputFile = File.createTempFile("audio_output", "." + format);
            if (format.equalsIgnoreCase("wav")) {
                convertMp3ToWav(inputFile, outputFile);
            } else {
                convertWavToMp3(inputFile, outputFile);
            }

            // Prepare response
            InputStreamResource resource = new InputStreamResource(new FileInputStream(outputFile));
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted." + format);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            LOGGER.log(Level.INFO, "Audio conversion successful. Output file: {0}", outputFile.getAbsolutePath());

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(outputFile.length())
                    .body(resource);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during audio conversion: {0}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        } finally {
            if (inputFile != null && inputFile.exists()) {
                inputFile.delete();
                LOGGER.log(Level.INFO, "Temporary input file deleted: {0}", inputFile.getAbsolutePath());
            }
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete();
                LOGGER.log(Level.INFO, "Temporary output file deleted: {0}", outputFile.getAbsolutePath());
            }
        }
    }

    private void convertMp3ToWav(File inputFile, File outputFile) throws Exception {
        LOGGER.log(Level.INFO, "Converting MP3 to WAV: {0} -> {1}", new Object[]{inputFile.getAbsolutePath(), outputFile.getAbsolutePath()});

        try (FileInputStream mp3Stream = new FileInputStream(inputFile);
             FileOutputStream wavStream = new FileOutputStream(outputFile)) {
            Bitstream bitstream = new Bitstream(mp3Stream);
            Decoder decoder = new Decoder();
            AudioFormat audioFormat = null;
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

            while (true) {
                Header header = bitstream.readFrame();
                if (header == null) break; // End of file

                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);

                if (audioFormat == null) {
                    audioFormat = new AudioFormat(
                            decoder.getOutputFrequency(),
                            16, // Sample size in bits
                            decoder.getOutputChannels(),
                            true, // Signed
                            false // Little-endian
                    );
                }

                // Convert short[] to byte[] and write to output stream
                short[] buffer = output.getBuffer();
                for (int i = 0; i < output.getBufferLength(); i++) {
                    byteOutputStream.write(buffer[i] & 0xFF);
                    byteOutputStream.write((buffer[i] >> 8) & 0xFF);
                }

                bitstream.closeFrame();
            }

            try (AudioInputStream audioInputStream = new AudioInputStream(
                    new ByteArrayInputStream(byteOutputStream.toByteArray()),
                    audioFormat,
                    byteOutputStream.size() / audioFormat.getFrameSize())) {

                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to convert MP3 to WAV: {0}", e.getMessage());
            throw e;
        }
    }

    private void convertWavToMp3(File inputFile, File outputFile) throws Exception {
        LOGGER.log(Level.INFO, "Converting WAV to MP3: {0} -> {1}", new Object[]{inputFile.getAbsolutePath(), outputFile.getAbsolutePath()});

        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i", inputFile.getAbsolutePath(),
                "-codec:a", "libmp3lame",
                outputFile.getAbsolutePath()
        );

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.log(Level.INFO, line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg process failed with exit code: " + exitCode);
        }
    }

    private String getExtension(String filename) {
        int lastIndex = filename.lastIndexOf(".");
        if (lastIndex == -1) {
            return ""; // No extension found
        }
        return filename.substring(lastIndex);
    }

    @GetMapping("/convert")
    public ResponseEntity<String> getAudioConversionInfo() {
        return ResponseEntity.ok("This endpoint supports POST requests for audio conversion.");
    }

}
