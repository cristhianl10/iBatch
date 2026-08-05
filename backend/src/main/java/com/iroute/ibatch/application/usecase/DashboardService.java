package com.iroute.ibatch.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.iroute.ibatch.domain.model.ProcessingLogEntry;
import com.iroute.ibatch.dto.response.DashboardSummaryResponse;
import com.iroute.ibatch.dto.response.ProcessingLogResponse;
import com.iroute.ibatch.dto.response.RejectionReasonSummaryResponse;
import com.iroute.ibatch.infrastructure.persistence.repository.ProcessingLogRepository;
import com.iroute.ibatch.infrastructure.persistence.repository.TransactionRepository;

@Service
public class DashboardService {

    private static final int RECENT_FILES_LIMIT = 5;
    private static final int RECENT_EVENTS_LIMIT = 5;

    private final ProcessedFileService processedFileService;
    private final TransactionRepository transactionRepository;
    private final ProcessingLogRepository processingLogRepository;

    public DashboardService(
            ProcessedFileService processedFileService,
            TransactionRepository transactionRepository,
            ProcessingLogRepository processingLogRepository) {
        this.processedFileService = processedFileService;
        this.transactionRepository = transactionRepository;
        this.processingLogRepository = processingLogRepository;
    }

    public DashboardSummaryResponse findSummary() {
        var files = processedFileService.findAll();
        var processedTransactions = files.stream()
                .mapToInt(file -> file.processedTransactions())
                .sum();
        var rejectedTransactions = files.stream()
                .mapToInt(file -> file.rejectedTransactions())
                .sum();
        var totalTransactions = processedTransactions + rejectedTransactions;
        var rejectionRate = totalTransactions == 0
                ? 0D
                : (rejectedTransactions * 100D) / totalTransactions;

        return new DashboardSummaryResponse(
                files.size(),
                processedTransactions,
                rejectedTransactions,
                rejectionRate,
                transactionRepository.findRejectionReasonSummary().stream()
                        .map(reason -> new RejectionReasonSummaryResponse(
                                reason.code(),
                                reason.name(),
                                reason.count()))
                        .toList(),
                files.stream().limit(RECENT_FILES_LIMIT).toList(),
                processingLogRepository.findRecentHighLevel(RECENT_EVENTS_LIMIT).stream()
                        .map(this::toLogResponse)
                        .toList());
    }

    public com.iroute.ibatch.dto.response.PageResponse<ProcessingLogResponse> findRecentLogs(int page, int size) {
        var offset = page * size;
        var totalElements = processingLogRepository.countAllLogs();
        var content = processingLogRepository.findRecentPaginated(size, offset).stream()
                .map(this::toLogResponse)
                .toList();
        return com.iroute.ibatch.dto.response.PageResponse.of(content, page, size, totalElements);
    }

    private ProcessingLogResponse toLogResponse(ProcessingLogEntry log) {
        return new ProcessingLogResponse(
                log.id(),
                log.fileId(),
                log.transactionId(),
                log.fileName(),
                log.level(),
                log.event(),
                log.message(),
                log.createdAt());
    }
}
