package com.xteam.video.scheduler;

import com.xteam.video.config.DistributedConcurrencyManager;
import com.xteam.video.config.GracefulShutdownConfig;
import com.xteam.video.config.ThirdPartyServiceConfig;
import com.xteam.video.dto.VideoGenerationRequest;
import com.xteam.video.entity.VideoTask;
import com.xteam.video.enums.TaskStatus;
import com.xteam.video.message.VideoTaskNotification;
import com.xteam.video.service.MessageNotificationService;
import com.xteam.video.service.ThirdPartyServiceClient;
import com.xteam.video.service.VideoTaskService;
import com.xteam.video.service.thirdparty.GenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VideoTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(VideoTaskScheduler.class);

    private final VideoTaskService videoTaskService;
    private final ThirdPartyServiceClient thirdPartyServiceClient;
    private final MessageNotificationService messageNotificationService;
    private final DistributedConcurrencyManager concurrencyManager;
    private final ThirdPartyServiceConfig serviceConfig;
    private final GracefulShutdownConfig gracefulShutdownConfig;

    @Value("${task.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    public VideoTaskScheduler(VideoTaskService videoTaskService, 
                              ThirdPartyServiceClient thirdPartyServiceClient,
                              MessageNotificationService messageNotificationService,
                              DistributedConcurrencyManager concurrencyManager,
                              ThirdPartyServiceConfig serviceConfig,
                              GracefulShutdownConfig gracefulShutdownConfig) {
        this.videoTaskService = videoTaskService;
        this.thirdPartyServiceClient = thirdPartyServiceClient;
        this.messageNotificationService = messageNotificationService;
        this.concurrencyManager = concurrencyManager;
        this.serviceConfig = serviceConfig;
        this.gracefulShutdownConfig = gracefulShutdownConfig;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application started, checking for processing tasks...");
        checkProcessingTasks();
    }

    private void checkProcessingTasks() {
        try {
            List<VideoTask> processingTasks = videoTaskService.getProcessingTasks();
            if (processingTasks.isEmpty()) {
                log.info("No processing tasks found");
                return;
            }

            log.info("Found {} processing tasks", processingTasks.size());

            for (VideoTask task : processingTasks) {
                if (task.getModelTaskId() == null || task.getModelTaskId().isEmpty()) {
                    log.warn("Task {} has no modelTaskId, resetting to PENDING", task.getId());
                    videoTaskService.updateTaskStatus(
                            task.getId(),
                            TaskStatus.PENDING.getCode(),
                            null,
                            "No modelTaskId found, restarting task"
                    );
                    continue;
                }

                String serviceKey = determineService(task);
                
                boolean acquired = concurrencyManager.acquireToken(serviceKey);
                if (!acquired) {
                    log.debug("Service {} is at max concurrent limit, skipping task {}", 
                              serviceKey, task.getId());
                    continue;
                }

                boolean locked = videoTaskService.lockAndProcessTask(task.getId());
                if (!locked) {
                    concurrencyManager.releaseToken(serviceKey);
                    log.debug("Task {} is already being processed by another instance", task.getId());
                    continue;
                }

                pollExistingTaskAsync(task, serviceKey);
            }
        } catch (Exception e) {
            log.error("Error checking processing tasks: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${task.scheduler.cron:0/5 * * * * ?}")
    public void processPendingTasks() {
        if (!schedulerEnabled) {
            return;
        }

        if (gracefulShutdownConfig.isShuttingDown()) {
            log.debug("Application is shutting down, skipping task processing");
            return;
        }

        //log.debug("Checking for pending video tasks...");

        try {
            List<VideoTask> pendingTasks = videoTaskService.getPendingTasks();
            if (pendingTasks.isEmpty()) {
                return;
            }

            log.info("Found {} pending tasks", pendingTasks.size());

            for (VideoTask task : pendingTasks) {
                if (gracefulShutdownConfig.isShuttingDown()) {
                    log.info("Application shutting down, stopping task processing");
                    break;
                }

                String serviceKey = determineService(task);
                
                boolean acquired = concurrencyManager.acquireToken(serviceKey);
                if (!acquired) {
                    log.debug("Service {} is at max concurrent limit, skipping task {}", 
                              serviceKey, task.getId());
                    continue;
                }

                boolean locked = videoTaskService.lockAndProcessTask(task.getId());
                if (!locked) {
                    concurrencyManager.releaseToken(serviceKey);
                    log.debug("Task {} is already being processed by another instance", task.getId());
                    continue;
                }

                processTaskAsync(task, serviceKey);
            }
        } catch (Exception e) {
            log.error("Error processing pending tasks: {}", e.getMessage(), e);
        }
    }

    @Async("videoTaskExecutor")
    public void processTaskAsync(VideoTask task, String serviceKey) {
        if (gracefulShutdownConfig.isShuttingDown()) {
            log.info("Application shutting down, skipping task {}", task.getId());
            concurrencyManager.releaseToken(serviceKey);
            return;
        }

        try {
            log.info("Processing task: id={}, userId={}, service={}", 
                     task.getId(), task.getUserId(), serviceKey);

            VideoGenerationRequest request = new VideoGenerationRequest();
            request.setTextContent(task.getTextContent());
            request.setGenerateAudio(task.getGenerateAudio() != null ? task.getGenerateAudio() : true);
            request.setVideoRatio(task.getVideoRatio() != null ? task.getVideoRatio() : "16:9");
            request.setVideoDuration(task.getVideoDuration() != null ? task.getVideoDuration() : 11L);
            request.setShowWatermark(task.getShowWatermark() != null ? task.getShowWatermark() : true);
            request.setResolution(task.getResolution() != null ? task.getResolution() : "720p");

            if (task.getFilePath() != null && !task.getFilePath().isEmpty()) {
                String extension = getFileExtension(task.getFilePath()).toLowerCase();
                if (isImageFile(extension)) {
                    request.addResource(VideoGenerationRequest.ResourceItem.image(task.getFilePath()));
                } else if (isVideoFile(extension)) {
                    request.addResource(VideoGenerationRequest.ResourceItem.video(task.getFilePath()));
                }
            }

            String modelTaskId = thirdPartyServiceClient.getModelTaskId(request, serviceKey);
            videoTaskService.updateModelTaskId(task.getId(), modelTaskId);
            log.info("Task {} created with modelTaskId: {}", task.getId(), modelTaskId);

            GenerationResult result = thirdPartyServiceClient.pollForExistingTask(modelTaskId, serviceKey);
            
            videoTaskService.updateTaskWithResult(
                    task.getId(),
                    TaskStatus.COMPLETED.getCode(),
                    result.getVideoUrl(),
                    null,
                    result.getFullResponse()
            );

            ThirdPartyServiceConfig.ServiceConfig config = serviceConfig.getServiceConfig(serviceKey);
            String serviceName = config != null ? config.getName() : serviceKey;

            VideoTaskNotification notification = VideoTaskNotification.success(
                    task.getId(),
                    task.getUserId(),
                    result.getVideoUrl(),
                    serviceName
            );
            messageNotificationService.sendNotification(notification);

            log.info("Task {} completed successfully", task.getId());

        } catch (Exception e) {
            log.error("Failed to process task {}: {}", task.getId(), e.getMessage(), e);

            if (task.getRetryCount() >= 3) {
                videoTaskService.updateTaskStatus(
                        task.getId(),
                        TaskStatus.FAILED.getCode(),
                        null,
                        e.getMessage()
                );

                ThirdPartyServiceConfig.ServiceConfig config = serviceConfig.getServiceConfig(serviceKey);
                String serviceName = config != null ? config.getName() : serviceKey;

                VideoTaskNotification notification = VideoTaskNotification.failure(
                        task.getId(),
                        task.getUserId(),
                        e.getMessage(),
                        serviceName
                );
                messageNotificationService.sendNotification(notification);

                log.info("Task {} failed after 3 retries", task.getId());
            } else {
                videoTaskService.updateTaskStatus(
                        task.getId(),
                        TaskStatus.PENDING.getCode(),
                        null,
                        e.getMessage()
                );
                log.info("Task {} will be retried (attempt {})", task.getId(), task.getRetryCount() + 1);
            }
        } finally {
            concurrencyManager.releaseToken(serviceKey);
        }
    }

    @Async("videoTaskExecutor")
    public void pollExistingTaskAsync(VideoTask task, String serviceKey) {
        if (gracefulShutdownConfig.isShuttingDown()) {
            log.info("Application shutting down, skipping task {}", task.getId());
            concurrencyManager.releaseToken(serviceKey);
            return;
        }

        try {
            log.info("Polling existing task: id={}, modelTaskId={}, service={}", 
                     task.getId(), task.getModelTaskId(), serviceKey);

            GenerationResult result = thirdPartyServiceClient.pollForExistingTask(task.getModelTaskId(), serviceKey);
            
            videoTaskService.updateTaskWithResult(
                    task.getId(),
                    TaskStatus.COMPLETED.getCode(),
                    result.getVideoUrl(),
                    null,
                    result.getFullResponse()
            );

            ThirdPartyServiceConfig.ServiceConfig config = serviceConfig.getServiceConfig(serviceKey);
            String serviceName = config != null ? config.getName() : serviceKey;

            VideoTaskNotification notification = VideoTaskNotification.success(
                    task.getId(),
                    task.getUserId(),
                    result.getVideoUrl(),
                    serviceName
            );
            messageNotificationService.sendNotification(notification);

            log.info("Task {} completed successfully", task.getId());

        } catch (Exception e) {
            log.error("Failed to poll task {}: {}", task.getId(), e.getMessage(), e);

            if (task.getRetryCount() >= 3) {
                videoTaskService.updateTaskStatus(
                        task.getId(),
                        TaskStatus.FAILED.getCode(),
                        null,
                        e.getMessage()
                );

                ThirdPartyServiceConfig.ServiceConfig config = serviceConfig.getServiceConfig(serviceKey);
                String serviceName = config != null ? config.getName() : serviceKey;

                VideoTaskNotification notification = VideoTaskNotification.failure(
                        task.getId(),
                        task.getUserId(),
                        e.getMessage(),
                        serviceName
                );
                messageNotificationService.sendNotification(notification);

                log.info("Task {} failed after 3 retries", task.getId());
            } else {
                videoTaskService.updateTaskStatus(
                        task.getId(),
                        TaskStatus.PENDING.getCode(),
                        null,
                        e.getMessage()
                );
                log.info("Task {} will be retried (attempt {})", task.getId(), task.getRetryCount() + 1);
            }
        } finally {
            concurrencyManager.releaseToken(serviceKey);
        }
    }

    private String determineService(VideoTask task) {
        if (task.getThirdPartyService() != null && !task.getThirdPartyService().isEmpty()) {
            return task.getThirdPartyService();
        }
        return serviceConfig.getDefaultService();
    }

    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filePath.substring(lastDotIndex + 1);
    }

    private boolean isImageFile(String extension) {
        return List.of("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(extension);
    }

    private boolean isVideoFile(String extension) {
        return List.of("mp4", "mov", "avi", "mkv", "webm", "flv").contains(extension);
    }
}