package com.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;

public class CrptApi {

    private static final Logger logger = LoggerFactory.getLogger(CrptApi.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final Semaphore semaphore;
    private final String token;

    public CrptApi(TimeUnit timeUnit, int requestLimit, String token) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.semaphore = new Semaphore(requestLimit);
        this.token = token;

        scheduler.scheduleAtFixedRate(semaphore::release, timeUnit.toMillis(1), timeUnit.toMillis(1), TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Void> createDocument(Document document, String signature) {
        return CompletableFuture.runAsync(() -> {
            try {
                semaphore.acquire();

                String json = objectMapper.writeValueAsString(document);
                String authorizationHeader = String.format("Bearer %s", token.trim());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", authorizationHeader)
                        .header("Signature", signature)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    logger.error("Failed to create document: {}", response.statusCode());
                    throw new RuntimeException(String.format("Failed to create document: %d", response.statusCode()));
                }
            } catch (Exception e) {
                logger.error("Error while creating document", e);
                throw new RuntimeException(e);
            }
        });
    }

    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public static class Description {
            @JsonProperty("participantInn")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public String participantInn;
        }

        public static class Product {
            @JsonProperty("certificate_document")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public String certificate_document;

            @JsonProperty("certificate_document_date")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public String certificate_document_date;

            @JsonProperty("certificate_document_number")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public String certificate_document_number;

            @JsonProperty("owner_inn")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public String owner_inn;

            @JsonProperty("producer_inn")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public String producer_inn;

            @JsonProperty("production_date")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public String production_date;

            @JsonProperty("tnved_code")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public String tnved_code;

            @JsonProperty("uit_code")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public String uit_code;

            @JsonProperty("uitu_code")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public String uitu_code;
        }
    }

    public static void main(String[] args) {
        // Пример использования
        try {
            String token = "ваш_токен"; // Убедитесь, что здесь нет лишних пробелов
            String signature = "ваша_подпись"; // Убедитесь, что здесь нет лишних пробелов

            CrptApi api = new CrptApi(TimeUnit.MINUTES, 10, token);
            Document document = new Document();
            // Установка значений для документа
            document.description = new Document.Description();
            document.description.participantInn = "1234567890";
            document.doc_id = "doc123";
            document.doc_status = "NEW";
            document.doc_type = "LP_INTRODUCE_GOODS";
            document.importRequest = true;
            document.owner_inn = "1234567890";
            document.participant_inn = "1234567890";
            document.producer_inn = "1234567890";
            document.production_date = "2020-01-23";
            document.production_type = "TYPE";
            document.products = new Document.Product[1];
            document.products[0] = new Document.Product();
            document.products[0].certificate_document = "cert123";
            document.products[0].certificate_document_date = "2020-01-23";
            document.products[0].certificate_document_number = "cert_num123";
            document.products[0].owner_inn = "1234567890";
            document.products[0].producer_inn = "1234567890";
            document.products[0].production_date = "2020-01-23";
            document.products[0].tnved_code = "tnved123";
            document.products[0].uit_code = "uit123";
            document.products[0].uitu_code = "uitu123";
            document.reg_date = "2020-01-23";
            document.reg_number = "reg123";

            api.createDocument(document, signature)
                    .thenRun(() -> logger.info("Document created successfully"))
                    .exceptionally(ex -> {
                        logger.error("Failed to create document", ex);
                        return null;
                    }).join();
        } catch (Exception e) {
            logger.error("Unexpected error", e);
        }
    }
}
