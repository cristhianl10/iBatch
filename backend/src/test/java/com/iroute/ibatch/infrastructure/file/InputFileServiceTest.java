package com.iroute.ibatch.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.iroute.ibatch.config.FileStorageProperties;
import org.springframework.mock.web.MockMultipartFile;

class InputFileServiceTest {

    @TempDir
    private Path inputDir;

    @Test
    void shouldListOnlyFilesWithExpectedFormat() throws Exception {
        Files.writeString(inputDir.resolve("transactions_30072026.csv"), "cuenta,monto,fecha");
        Files.writeString(inputDir.resolve("transactions_wrong.csv"), "cuenta,monto,fecha");
        Files.writeString(inputDir.resolve("notes.txt"), "ignore");

        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));

        var files = service.findAvailableCsvFiles();

        assertThat(files).hasSize(1);
        assertThat(files)
                .extracting("fileName")
                .containsExactly("transactions_30072026.csv");
        assertThat(files.get(0).expectedFormat()).isTrue();
    }

    @Test
    void shouldReturnEmptyListWhenInputDirectoryDoesNotExist() {
        var service = new InputFileService(new FileStorageProperties(inputDir.resolve("missing"), 52_428_800L));

        var files = service.findAvailableCsvFiles();

        assertThat(files).isEmpty();
    }

    @Test
    void shouldValidateExistingFileForProcessing() throws Exception {
        Files.writeString(inputDir.resolve("transactions_30072026.csv"), "cuenta,monto,fecha");
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));

        var response = service.validateFileForProcessing("transactions_30072026.csv");

        assertThat(response.fileName()).isEqualTo("transactions_30072026.csv");
        assertThat(response.originalPath()).endsWith("transactions_30072026.csv");
        assertThat(response.fileDate()).isEqualTo(LocalDate.parse("2026-07-30"));
    }

    @Test
    void shouldRejectFileWithUnexpectedFormat() {
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));

        assertThatThrownBy(() -> service.validateFileForProcessing("transactions 30072026.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo debe cumplir el formato transactions_DDMMYYYY.csv");
    }

    @Test
    void shouldRejectMissingFile() {
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));

        assertThatThrownBy(() -> service.validateFileForProcessing("transactions_30072026.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo no existe en el directorio configurado");
    }

    @Test
    void shouldStoreValidUploadedCsv() {
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));
        var upload = new MockMultipartFile(
                "file",
                "transactions_13082026.csv",
                "text/csv",
                "cuenta,monto,fecha\n2000000000,10.50,13/08/2026".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        var response = service.saveUploadedCsv(upload);

        assertThat(response.fileName()).isEqualTo("transactions_13082026.csv");
        assertThat(inputDir.resolve("transactions_13082026.csv")).exists();
    }

    @Test
    void shouldRejectUploadWithInvalidHeaders() {
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));
        var upload = new MockMultipartFile(
                "file",
                "transactions_13082026.csv",
                "text/csv",
                "account,value,date\n2000000000,10.50,13/08/2026".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.saveUploadedCsv(upload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El CSV debe incluir los encabezados cuenta,monto,fecha");
    }

    @Test
    void shouldRejectDuplicateUploadedFileName() throws Exception {
        Files.writeString(inputDir.resolve("transactions_13082026.csv"), "cuenta,monto,fecha");
        var service = new InputFileService(new FileStorageProperties(inputDir, 52_428_800L));
        var upload = new MockMultipartFile(
                "file",
                "transactions_13082026.csv",
                "text/csv",
                "cuenta,monto,fecha\n2000000000,10.50,13/08/2026".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.saveUploadedCsv(upload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un archivo con el mismo nombre");
    }
}
