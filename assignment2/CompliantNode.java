import java.util.ArrayList;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    ArrayList<Integer> badFollowees;
    boolean[] myFollowees;
    Set<Transaction> pTransactions;
    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        badFollowees = new ArrayList<>();
    }

    public void setFollowees(boolean[] followees) {
        myFollowees = followees;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        pTransactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        return pTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        for(Candidate c : candidates) {
            if(c.tx.hashCode() == 0 || c.tx == null) {
                badFollowees.add(c.sender);
            }
            if(!badFollowees.contains(c.sender)) {
                pTransactions.add(c.tx);
            }
        }
    }
}
