package consensus.crypto;

import consensus.net.data.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;

public abstract class CryptoMessage {
    private static final Logger log = LogManager.getLogger(CryptoMessage.class);
    private final JSONObject doc;
    public final CryptoMessageKind kind;
    public final String sessionId;

    protected CryptoMessage(CryptoMessageKind kind, String sessionId) {
        doc = new JSONObject();
        doc.put("kind", kind.name());
        doc.put("session_id", sessionId);

        this.kind = kind;
        this.sessionId = sessionId;
    }

    protected void append(String key, String str) {
        doc.put(key, str);
    }

    protected void append(String key, JSONObject obj) {
        doc.put(key, obj);
    }

    public static Optional<CryptoMessage> tryFrom(CryptoContext ctx, String string) {
        try {
            var doc = new JSONObject(string);
            var maybeKind = CryptoMessageKind.tryFrom(doc.getString("kind"));
            if (maybeKind.isEmpty()) {
                return Optional.empty();
            }
            var sessionId = doc.optString("session_id");
            if (sessionId.length() == 0) {
                return Optional.empty();
            }

            switch (maybeKind.get()) {
                case KEYGEN_COMMIT:
                    // Extract the data
                    var commitment = doc.optString("commitment");
                    var g = doc.optString("g");

                    // Validate and convert to the desired format
                    if (commitment.length() > 0 && g.length() > 0) {
                        var decodedCommit = CryptoUtils.b64Decode(commitment);
                        var decodedGenerator = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(g));
                        return Optional.of(new KeygenCommitMessage(sessionId, decodedCommit, decodedGenerator));
                    } else {
                        return Optional.empty();
                    }

                case KEYGEN_OPENING:
                    // Extract the data
                    var y_i = doc.optString("y_i");
                    var proof = doc.optJSONObject("proof");

                    // Validate and convert to the desired format
                    var maybeProofKnow = ProofKnowDlog.tryFrom(ctx, proof);

                    if (y_i.length() > 0 && maybeProofKnow.isPresent()) {
                        var y_iActual = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(y_i));
                        return Optional.of(new KeygenOpeningMessage(sessionId, y_iActual, maybeProofKnow.get()));
                    } else {
                        return Optional.empty();
                    }

                case POST_VOTE:
                    var maybeVote = Ciphertext.tryFrom(ctx, doc.getJSONObject("vote"));
                    var maybeProof = ProofKnowDlog.tryFrom(ctx, doc.getJSONObject("proof"));
                    if (maybeVote.isPresent() && maybeProof.isPresent()) {
                        return Optional.of(new PostVoteMessage(sessionId, maybeVote.get(), maybeProof.get()));
                    } else {
                        return Optional.empty();
                    }

                case DECRYPT_SHARE:
                    // Extract the data
                    var a_i = doc.optString("a_i");
                    proof = doc.optJSONObject("proof");
                    g = doc.optString("g");
                    var id = doc.optString("id");

                    // Validate and convert to the desired format
                    var maybeProofEq = ProofEqDlogs.tryFrom(ctx, proof);

                    if (a_i != null && g != null && id != null && maybeProofEq.isPresent()) {
                        var a_iDecoded = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(a_i));
                        var gDecoded = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(g));
                        return Optional.of(new DecryptShareMessage(sessionId, id, a_iDecoded, maybeProofEq.get(), gDecoded));
                    } else {
                        return Optional.empty();
                    }

                default:
                    log.warn("switch missing enum case");
                    return Optional.empty();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            log.warn("failed parsing JSON: " + e.getMessage());
            return Optional.empty();
        }
    }

    public Message encode() {
        return new Message(doc.toString());
    }
}
