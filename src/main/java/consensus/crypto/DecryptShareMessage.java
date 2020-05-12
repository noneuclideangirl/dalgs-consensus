package consensus.crypto;

public class DecryptShareMessage extends CryptoMessage {
    public static final CryptoMessageKind KIND = CryptoMessageKind.DECRYPT_SHARE;
    public final GroupElement a_i;
    public final ProofEqDlogs proof;
    public final GroupElement g;

    public DecryptShareMessage(CryptoContext ctx, KeyShare keyShare, Ciphertext ct) {
        super(KIND);

        this.a_i = ct.a.pow(keyShare.x_i);
        this.proof = new ProofEqDlogs(ctx, ctx.g, ct.a, keyShare.y_i, a_i, keyShare.x_i);
        this.g = ctx.g;

        var a_iEncoded = CryptoUtils.b64Encode(this.a_i.asBytes());
        var proofEncoded = proof.asJson();
        this.append("a_i", a_iEncoded);
        this.append("proof", proofEncoded);
        this.append("g", CryptoUtils.b64Encode(g.asBytes()));
    }

    protected DecryptShareMessage(GroupElement a_i, ProofEqDlogs proof, GroupElement g) {
        super(KIND);
        this.a_i = a_i;
        this.proof = proof;
        this.g = g;
    }

    public boolean verify(GroupElement y_i, Ciphertext ct) {
        return proof.verify() && proof.a.equals(g) && proof.b.equals(ct.a)
                && proof.d.equals(y_i) && proof.e.equals(a_i);
    }
}
