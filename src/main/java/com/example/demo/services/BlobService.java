package com.example.demo.services;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class BlobService {
    private BlobContainerClient blobContainerClient;

    public BlobService(@Value("${azure.storage.connection-string}") String connectionString, @Value("${azure.storage.blob-container-name}") String containerName) {
        super();
        this.blobContainerClient = new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .buildClient();
    }
    
    public void upload(String blobName, byte[] data) {
        InputStream dataStream = new ByteArrayInputStream(data);
        this.blobContainerClient.getBlobClient(blobName).upload(dataStream, data.length, true);
    }

    public byte[] download(String blobName) {
        return this.blobContainerClient.getBlobClient(blobName).downloadContent().toBytes();
    }

    public void delete(String blobName) {
        this.blobContainerClient.getBlobClient(blobName).delete();
    }
}