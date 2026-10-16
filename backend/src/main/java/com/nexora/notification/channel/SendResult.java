package com.nexora.notification.channel;

import com.nexora.notification.entity.DeliveryAttempt;

public record SendResult(DeliveryAttempt.Status status, String detail) {
    public static SendResult delivered(String detail) { return new SendResult(DeliveryAttempt.Status.DELIVERED, detail); }
    public static SendResult failed(String detail) { return new SendResult(DeliveryAttempt.Status.FAILED, detail); }
    public static SendResult skipped(String detail) { return new SendResult(DeliveryAttempt.Status.SKIPPED, detail); }
}
