package com.jobr.worker_service.executor;

public interface JobExecutor {
    void execute(String payload) throws Exception;
}