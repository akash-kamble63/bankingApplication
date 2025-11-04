package com.banking.keycloak;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;

public class WebhookSender {

    private static final Logger log = Logger.getLogger(WebhookSender.class);

    public void sendWebhook(String url, String secret, String payload) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            
            // Set headers
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("X-Keycloak-Secret", secret);
            
            // Set payload
            StringEntity entity = new StringEntity(payload, "UTF-8");
            httpPost.setEntity(entity);
            
            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode >= 200 && statusCode < 300) {
                    log.infof("Webhook sent successfully. Status: %d", statusCode);
                } else {
                    log.warnf("Webhook returned non-success status: %d", statusCode);
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to send webhook", e);
        }
    }
}