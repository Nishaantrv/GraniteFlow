package com.graniteflow.app.data.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String DEFAULT_BASE_URL = "http://10.0.2.2:8080/"; // Standard Android emulator localhost
    private static Retrofit retrofit = null;

    public static Retrofit getClient(String customBaseUrl) {
        String baseUrl = (customBaseUrl != null && !customBaseUrl.isEmpty()) ? customBaseUrl : DEFAULT_BASE_URL;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
    }

    public static GraniteFlowApiService getApiService(String baseUrl) {
        return getClient(baseUrl).create(GraniteFlowApiService.class);
    }

    public static DocumentIntelligenceService getOcrService(String baseUrl) {
        return getClient(baseUrl).create(DocumentIntelligenceService.class);
    }
}
