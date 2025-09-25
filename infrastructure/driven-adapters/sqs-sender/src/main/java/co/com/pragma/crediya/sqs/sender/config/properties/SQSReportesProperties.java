package co.com.pragma.crediya.sqs.sender.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapter.sqs.approved")
public record SQSReportesProperties (
        String queueUrl
) { }
