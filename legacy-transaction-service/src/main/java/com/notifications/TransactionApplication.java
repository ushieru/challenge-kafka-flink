package com.notifications;

import com.notifications.avro.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class TransactionApplication {

    private static final Logger LOG = Logger.getLogger(TransactionApplication.class);

    @Inject
    @Channel("transactions-out")
    Emitter<Transaction> transactionEmitter;

    public void processTransaction(TransactionRequest request) {
        var avroTransaction = Transaction.newBuilder()
                .setTransactionId(UUID.randomUUID().toString())
                .setAmount(request.amount())
                .setMerchantName(request.merchant())
                .setAccountNumber(request.account())
                .build();
        LOG.info("SEND TRANSACTION");
        LOG.info(avroTransaction.toString());
        transactionEmitter.send(avroTransaction);
    }
}
