package consensus;

import api.application.WSApplication;
import blockchain.block.Blockchain;
import blockchain.miner.Miner;
import blockchain.p2p.BlockchainActor;
import blockchain.transaction.TransactionPool;
import blockchain.wallet.Wallet;
import consensus.ipc.IpcServer;
import consensus.net.PeerListener;
import consensus.net.data.HostPort;
import consensus.raft.RaftActor;
import consensus.util.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Entry point for the program.
 */
public class ConsensusPeer {
    private static final Logger log = LogManager.getLogger(ConsensusPeer.class);

    public static void main(String[] args) {
        ConfigManager.loadProperties();
        if (ConfigManager.isDebug()) {
            runDebug();
        } else {
            runRelease();
        }
    }

    private static List<HostPort> loadHosts() {
        // Load the targets to connect to and validate them
        var hostList = ConfigManager.getString("hosts").orElse("").split(",");
        var peerHostPorts = Arrays.stream(hostList)
                .map(String::trim)
                .map(HostPort::tryFrom)
                .collect(Collectors.toList());
        if (peerHostPorts.stream().anyMatch(Optional::isEmpty)) {
            log.fatal("failed parsing hosts");
            System.exit(-1);
        }

        // Validation done
        return peerHostPorts.stream()
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private static void runDebug() {
        var hosts = loadHosts();

        var actors = new ArrayList<BlockchainActor>();

        WSApplication wsApplication;
        // Start a peer for each id
        for (int i = 0; i < hosts.size(); ++i) {
            final int id = i;
            var thisPeerHosts = new ArrayList<>(hosts);
            var client = new IpcServer(id);
            var blockchain = new Blockchain();
            var transactionPool = new TransactionPool();
            var miner = new Miner(blockchain, transactionPool);
            var wallet = new Wallet(blockchain, transactionPool);
            var blockchainActor = new BlockchainActor(id, client, blockchain, transactionPool, miner, wallet);
            actors.add(blockchainActor);
            new Thread(() -> new PeerListener(id, thisPeerHosts, blockchainActor)).start();
            // new Thread(() -> new PeerListener(id, thisPeerHosts, new RaftActor(id, hosts.size(), client))).start();

            if (i == 0 || i == 1) {
                try {
                    wsApplication = new WSApplication(blockchain, transactionPool, miner, wallet);
                    String configFile = "";
                    if (i == 0) {
                        configFile = "first-configuration.yml";
                    }
                    if (i == 1) {
                        configFile = "second-configuration.yml";
                    }
                    wsApplication.run("server", configFile);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    private static void runRelease() {
        var maybeId = ConfigManager.getInt("id");
        if (maybeId.isEmpty()) {
            log.fatal("id must be an integer (check configuration file)");
            System.exit(2);
        }
        int id = maybeId.get();

        var hosts = loadHosts();
        var client = new IpcServer(id);
        new Thread(() -> new PeerListener(id, hosts, new RaftActor(id, hosts.size(), client))).start();
    }
}
