package com.notifications;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.notifications.avro.EnrichedNotification;
import com.notifications.avro.Transaction;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroSerializationSchema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

public class EnrichmentJob {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        var schemaRegistryUrl = "http://apicurio:8080/apis/ccompat/v7";
        var bootstrapServers = "kafka:29092";

        var source = KafkaSource.<Transaction>builder()
                .setBootstrapServers(bootstrapServers)
                .setTopics("transactions")
                .setGroupId("enricher-group-container")
                .setValueOnlyDeserializer(
                        ConfluentRegistryAvroDeserializationSchema.forSpecific(Transaction.class, schemaRegistryUrl))
                .build();

        var sink = KafkaSink.<EnrichedNotification>builder()
                .setBootstrapServers(bootstrapServers)
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("enriched-notifications")
                        .setValueSerializationSchema(ConfluentRegistryAvroSerializationSchema
                                .forSpecific(EnrichedNotification.class, "enriched-notifications-value", schemaRegistryUrl))
                        .build())
                .build();

        env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
                .map(new MongoEnricher())
                .name("Enrichment Job Container Mode")
                .sinkTo(sink);

        env.execute("Enrichment Job Container Mode");
    }

    public static class MongoEnricher extends RichMapFunction<Transaction, EnrichedNotification> {
        private transient MongoClient mongoClient;
        private transient MongoCollection<Document> collection;

        @Override
        public void open(Configuration parameters) {
            mongoClient = MongoClients.create("mongodb://mongodb:27017");
            MongoDatabase database = mongoClient.getDatabase("notification_db");
            collection = database.getCollection("users");
        }

        @Override
        public EnrichedNotification map(Transaction transaction) {
            Document user = collection.find(eq("accountNumber", transaction.getAccountNumber())).first();
            return EnrichedNotification.newBuilder()
                    .setTransactionId(transaction.getTransactionId())
                    .setAmount(transaction.getAmount())
                    .setMerchantName(transaction.getMerchantName())
                    .setAccountNumber(transaction.getAccountNumber())
                    .setEmail(user.getString("email"))
                    .setPhoneNumber(user.getString("phoneNumber"))
                    .setDeviceId(user.getString("deviceId"))
                    .build();
        }

        @Override
        public void close() {
            if (mongoClient != null) mongoClient.close();
        }
    }
}