package co.com.pragma.crediya.sqs.sender.config.properties;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapter.sqs.calculator")
public record SQSCapacidadProperties(
        String queueUrl
) { }
