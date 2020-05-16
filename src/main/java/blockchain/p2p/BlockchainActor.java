package blockchain.p2p;

import blockchain.block.BlockChain;
import blockchain.model.BlockchainMessage;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionPool;
import consensus.crypto.StringUtils;
import consensus.net.Actor;
import consensus.net.data.IncomingMessage;

public class BlockchainActor extends Actor {
    private final int id;
    private final BlockchainClient blockchainClient;

    public BlockchainActor(int id, BlockchainClient blockchainClient) {
        this.id = id;
        this.blockchainClient = blockchainClient;
    }

    @Override
    protected void task() throws InterruptedException {
        while (!Thread.interrupted()) {
            var message = blockchainClient.getBroadcastQueue().take();
            blockchainClient.receiveEntry(new IncomingMessage(message, id));
            BlockChain blockChain = blockchainClient.getBlockChain();
            TransactionPool transactionPool = blockchainClient.getTransactionPool();
            BlockchainMessage blockchainMessage = StringUtils.fromJson(message.data, BlockchainMessage.class);
            System.out.println(blockchainMessage.getMessageType());
            System.out.println(blockchainMessage.getJsonData());
            switch (blockchainMessage.getMessageType()) {
                case REPLICATE_CHAIN:
                    BlockChain newBlockChain = StringUtils.fromJson(blockchainMessage.getJsonData(), BlockChain.class);
                    blockChain.replaceListOfBlocks(newBlockChain);
                    break;
                case ADD_TRANSACTION:
                    Transaction newTransaction = StringUtils.fromJson(blockchainMessage.getJsonData(), Transaction.class);
                    transactionPool.updateOrAddTransaction(newTransaction);
                    break;
                case CLEAR_TRANSACTIONS:
                    transactionPool.clear();
                    break;
            }
            // this.sendMessageToAll(message);
        }
    }
}
