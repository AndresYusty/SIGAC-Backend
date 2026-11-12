package com.university.sigac.core.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Amazon S3 bucket. Credentials and region fall back to the AWS default provider chains. */
final class S3FileStore implements FileStore {

    private final S3Client s3;
    private final String bucket;

    S3FileStore(String bucket, String region) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("SIGAC_S3_BUCKET is required when SIGAC_STORAGE_TYPE=s3");
        }
        S3ClientBuilder builder = S3Client.builder();
        if (region != null && !region.isBlank()) {
            builder.region(Region.of(region.trim()));
        }
        this.s3 = builder.build();
        this.bucket = bucket.trim();
    }

    @Override
    public void write(String key, byte[] data) throws IOException {
        try {
            s3.putObject(r -> r.bucket(bucket).key(key).contentType(contentType(key)), RequestBody.fromBytes(data));
        } catch (SdkException e) {
            throw new IOException("S3 put failed for " + key, e);
        }
    }

    @Override
    public void write(String key, InputStream in) throws IOException {
        write(key, in.readAllBytes());
    }

    @Override
    public byte[] read(String key) throws IOException {
        try {
            return s3.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundException(key);
        } catch (SdkException e) {
            throw new IOException("S3 get failed for " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return head(key) != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public long size(String key) throws IOException {
        HeadObjectResponse head = head(key);
        return head == null ? -1 : head.contentLength();
    }

    @Override
    public void delete(String key) throws IOException {
        try {
            s3.deleteObject(r -> r.bucket(bucket).key(key));
        } catch (SdkException e) {
            throw new IOException("S3 delete failed for " + key, e);
        }
    }

    private HeadObjectResponse head(String key) throws IOException {
        try {
            return s3.headObject(r -> r.bucket(bucket).key(key));
        } catch (NoSuchKeyException e) {
            return null;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return null;
            }
            throw new IOException("S3 head failed for " + key, e);
        } catch (SdkException e) {
            throw new IOException("S3 head failed for " + key, e);
        }
    }

    private static String contentType(String key) {
        String k = key.toLowerCase();
        if (k.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (k.endsWith(".zip")) {
            return "application/zip";
        }
        return "application/octet-stream";
    }
}
