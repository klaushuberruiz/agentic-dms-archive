package com.dms.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlobStorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "azure.storage.blob", name = "connection-string")
    public BlobServiceClient blobServiceClient(
        @org.springframework.beans.factory.annotation.Value("${azure.storage.blob.connection-string}") String connectionString
    ) {
        return new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
    }

    @Bean
    @ConditionalOnProperty(prefix = "azure.storage.blob", name = "connection-string")
    public BlobContainerClient blobContainerClient(
        BlobServiceClient blobServiceClient,
        @org.springframework.beans.factory.annotation.Value("${azure.storage.blob.container-name:documents}") String containerName
    ) {
        BlobContainerClient client = blobServiceClient.getBlobContainerClient(containerName);
        if (!client.exists()) {
            client.create();
        }
        return client;
    }
}
