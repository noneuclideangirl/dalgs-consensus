package consensus.net.data;

import consensus.util.Validation;

import java.util.Optional;

public class HostPort {
    public final String address;
    public final int port;

    private HostPort(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public static Optional<HostPort> tryFrom(String combined) {
        var tokens = combined.split(":");
        if (tokens.length != 2) {
            return Optional.empty();
        }
        var address = tokens[0];
        var maybePort = Validation.tryParseUInt(tokens[1]);
        return maybePort.map(port -> new HostPort(address, port));
    }

    @Override
    public String toString() {
        return String.format("%s:%d", address, port);
    }
}
