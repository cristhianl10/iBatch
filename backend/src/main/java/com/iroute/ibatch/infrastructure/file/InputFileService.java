package com.iroute.ibatch.infrastructure.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.iroute.ibatch.config.FileStorageProperties;
import com.iroute.ibatch.domain.model.InputFileMetadata;
import com.iroute.ibatch.dto.response.AvailableFileResponse;

@Service
public class InputFileService {

    private static final Pattern TRANSACTIONS_FILE_PATTERN =
            Pattern.compile("^transactions_\\d{8}\\.csv$", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");

    private final FileStorageProperties fileStorageProperties;

    public InputFileService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    public List<AvailableFileResponse> findAvailableCsvFiles() {
        return findAvailableCsvFiles(List.of());
    }

    public List<AvailableFileResponse> findAvailableCsvFiles(Collection<String> excludedFileNames) {
        var inputDir = resolveInputDir();

        if (!Files.isDirectory(inputDir)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(inputDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(this::isCsvFile)
                    .filter(file -> TRANSACTIONS_FILE_PATTERN.matcher(file.getFileName().toString()).matches())
                    .filter(file -> !excludedFileNames.contains(file.getFileName().toString()))
                    .map(this::toResponse)
                    .sorted(Comparator.comparing(AvailableFileResponse::fileName))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el directorio de entrada", exception);
        }
    }

    public InputFileMetadata validateFileForProcessing(String fileName) {
        if (!TRANSACTIONS_FILE_PATTERN.matcher(fileName).matches()) {
            throw new IllegalArgumentException("El archivo debe cumplir el formato transactions_DDMMYYYY.csv");
        }

        var inputDir = resolveInputDir();
        var filePath = inputDir.resolve(fileName).normalize();

        if (!filePath.startsWith(inputDir)) {
            throw new IllegalArgumentException("El nombre del archivo no es valido");
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("El archivo no existe en el directorio configurado");
        }

        try {
            if (Files.size(filePath) > fileStorageProperties.maxSizeBytes()) {
                throw new IllegalArgumentException("El archivo excede el tamano maximo permitido");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo consultar el tamano del archivo", exception);
        }

        return new InputFileMetadata(
                fileName,
                filePath.toString(),
                extractFileDate(fileName));
    }

    public AvailableFileResponse saveUploadedCsv(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw new IllegalArgumentException("Seleccione un archivo CSV con contenido");
        }

        var originalName = upload.getOriginalFilename();
        if (originalName == null || !originalName.equals(Path.of(originalName).getFileName().toString())) {
            throw new IllegalArgumentException("El nombre del archivo no es valido");
        }
        if (!TRANSACTIONS_FILE_PATTERN.matcher(originalName).matches()) {
            throw new IllegalArgumentException("El archivo debe cumplir el formato transactions_DDMMYYYY.csv");
        }
        extractFileDate(originalName);
        if (upload.getSize() > fileStorageProperties.maxSizeBytes()) {
            throw new IllegalArgumentException("El archivo excede el tamano maximo permitido");
        }

        validateUploadedHeader(upload);
        var inputDir = resolveInputDir();
        var target = inputDir.resolve(originalName).normalize();
        if (!target.startsWith(inputDir)) {
            throw new IllegalArgumentException("El nombre del archivo no es valido");
        }

        try {
            Files.createDirectories(inputDir);
            if (Files.exists(target)) {
                throw new IllegalArgumentException("Ya existe un archivo con el mismo nombre");
            }
            var temporary = Files.createTempFile(inputDir, ".upload-", ".tmp");
            try {
                upload.transferTo(temporary);
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, target);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return toResponse(target);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo guardar el archivo en el directorio configurado", exception);
        }
    }

    private void validateUploadedHeader(MultipartFile upload) {
        try (var reader = new BufferedReader(new InputStreamReader(upload.getInputStream(), StandardCharsets.UTF_8))) {
            var header = reader.readLine();
            if (header == null) {
                throw new IllegalArgumentException("El archivo CSV esta vacio");
            }
            header = header.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
            if (!header.matches("cuenta\\s*,\\s*monto\\s*,\\s*fecha(?:\\s*,.*)?")) {
                throw new IllegalArgumentException("El CSV debe incluir los encabezados cuenta,monto,fecha");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("No se pudo leer el archivo CSV", exception);
        }
    }

    private Path resolveInputDir() {
        return fileStorageProperties.inputDir().toAbsolutePath().normalize();
    }

    private boolean isCsvFile(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    private LocalDate extractFileDate(String fileName) {
        try {
            var datePart = fileName.substring("transactions_".length(), "transactions_".length() + 8);

            return LocalDate.parse(datePart, FILE_DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("La fecha del archivo no es valida");
        }
    }

    private AvailableFileResponse toResponse(Path file) {
        try {
            var fileName = file.getFileName().toString();
            var lastModifiedAt = OffsetDateTime.ofInstant(
                    Files.getLastModifiedTime(file).toInstant(),
                    ZoneId.systemDefault());

            return new AvailableFileResponse(
                    fileName,
                    Files.size(file),
                    lastModifiedAt,
                    TRANSACTIONS_FILE_PATTERN.matcher(fileName).matches());
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer la informacion del archivo " + file, exception);
        }
    }
}
