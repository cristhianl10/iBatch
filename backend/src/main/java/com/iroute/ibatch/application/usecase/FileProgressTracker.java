package com.iroute.ibatch.application.usecase;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.iroute.ibatch.domain.model.FileProgress;

@Component
public class FileProgressTracker {

    private final Map<Long, FileProgress> activeProgressMap = new ConcurrentHashMap<>();

    public void startProgress(Long fileId, String fileName) {
        activeProgressMap.put(fileId, new FileProgress(
                fileId,
                fileName,
                0,
                0,
                0,
                0.0,
                "PROCESANDO",
                false,
                false
        ));
    }

    public void updateProgress(Long fileId, String fileName, int processedCount, int rejectedCount, int totalRecords) {
        int currentTotal = processedCount + rejectedCount;
        double percentage = totalRecords > 0 
                ? Math.min(100.0, Math.round(((double) currentTotal / totalRecords) * 1000.0) / 10.0) 
                : 0.0;

        activeProgressMap.put(fileId, new FileProgress(
                fileId,
                fileName,
                processedCount,
                rejectedCount,
                totalRecords,
                percentage,
                "PROCESANDO",
                false,
                false
        ));
    }

    public void finishProgress(Long fileId, String fileName, int processedCount, int rejectedCount, int totalRecords, boolean hasRejections) {
        String status = hasRejections ? "PROCESADO_CON_RECHAZOS" : "PROCESADO";
        activeProgressMap.put(fileId, new FileProgress(
                fileId,
                fileName,
                processedCount,
                rejectedCount,
                totalRecords,
                100.0,
                status,
                true,
                false
        ));
    }

    public void errorProgress(Long fileId, String fileName) {
        activeProgressMap.put(fileId, new FileProgress(
                fileId,
                fileName,
                0,
                0,
                0,
                0.0,
                "ERROR",
                true,
                true
        ));
    }

    public Optional<FileProgress> getProgress(Long fileId) {
        return Optional.ofNullable(activeProgressMap.get(fileId));
    }

    public void removeProgress(Long fileId) {
        activeProgressMap.remove(fileId);
    }
}
