package blockchain.wallet;

import blockchain.block.Block;
import blockchain.block.BlockChain;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionInput;
import blockchain.transaction.TransactionOutput;
import blockchain.transaction.TransactionPool;
import consensus.crypto.CryptoUtils;
import consensus.crypto.ECCCipher;
import consensus.crypto.StringUtils;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Wallet {
    private static final float INITIAL_AMOUNT = 50.0f;
    private float amount;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public Wallet() {
        this.amount = INITIAL_AMOUNT;
        KeyPair keyPair = ECCCipher.generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
    }

    public float getAmount() {
        return amount;
    }

    public String getAddress() {
        return ECCCipher.publicKeyToHex(publicKey);
    }

    public Transaction createTransaction(String recipient, float amount, BlockChain blockchain, TransactionPool transactionPool) {
        this.amount = this.calculateBalance(blockchain);
        if (amount > this.amount) {
            return null;
        }

        Transaction transaction = transactionPool.existingTransaction(this.getAddress());
        if (transaction != null) {
            transaction.update(this, recipient, amount);
        } else {
            transaction = Transaction.newTransaction(this, recipient, amount);
            transactionPool.updateOrAddTransaction(transaction);
        }

        return transaction;
    }

    private byte[] sign(String data) throws Exception {
        return ECCCipher.sign(privateKey, data);
    }

    public boolean signTransaction(Transaction transaction) {
        try {
            long timestamp = (new Date()).getTime();
            List<TransactionOutput> transactionOutputs = transaction.getTransactionOutputs();
            String data = CryptoUtils.hash(StringUtils.toJson(transactionOutputs));
            byte[] signature = sign(data);
            TransactionInput transactionInput = new TransactionInput(timestamp, getAddress(), amount, signature);
            transaction.setTransactionInput(transactionInput);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public float calculateBalance(BlockChain blockChain) {
        float balance = this.amount;

        List<Transaction> allTransactions = new ArrayList<>();
        List<Block> blockList = blockChain.getBlockList();
        for (Block block : blockList) {
            List<Transaction> transactionList = block.getTransactionList();
            allTransactions.addAll(transactionList);
        }

        allTransactions = allTransactions.stream()
                .filter(transaction -> transaction.getTransactionInput().getAddress().equals(getAddress()))
                .collect(Collectors.toList());

        double startTime = 0.0f;
        if (allTransactions.size() > 0) {
            Transaction recentTransaction = allTransactions.stream()
                    .reduce((prev, current) ->
                            prev.getTransactionInput().getTimestamp() > current.getTransactionInput().getTimestamp() ? prev : current)
                    .get();
            TransactionOutput transactionOutput = recentTransaction.getTransactionOutputs().stream()
                    .filter(output -> output.getAddress().equals(getAddress())).findAny().orElse(null);
            if (transactionOutput != null) {
                balance = transactionOutput.getAmount();
                startTime = recentTransaction.getTransactionInput().getTimestamp();
            }
        }

        for (Transaction transaction : allTransactions) {
            if (transaction.getTransactionInput().getTimestamp() > startTime) {
                List<TransactionOutput> transactionOutputs = transaction.getTransactionOutputs();
                for (TransactionOutput transactionOutput : transactionOutputs) {
                    if (transactionOutput.getAddress().equals(getAddress())) {
                        balance += transactionOutput.getAmount();
                    }
                }
            }
        }

        return balance;
    }
}
