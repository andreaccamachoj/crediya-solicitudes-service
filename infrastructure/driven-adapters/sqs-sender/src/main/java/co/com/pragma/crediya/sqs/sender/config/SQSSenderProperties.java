package co.com.pragma.crediya.sqs.sender.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapter.sqs")
public record SQSSenderProperties(
     String region,
     String queueUrl,
     String endpoint,
     String awsAccessKeyId,
     String awsSecretAccessKey){
}
