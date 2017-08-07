import javax.xml.crypto.dsig.TransformService;
import java.util.ArrayList;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */

    private UTXOPool currentPool;

    public TxHandler(UTXOPool utxoPool) {
        currentPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        boolean Tx_validity = false;
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();

//        Pool will be used for rule 3
        UTXOPool tx_UTXOs = new UTXOPool();

        //          Will be used for rule 5
        double inputSum = 0.0;
        double outputSum = 0.0;

//        (1)

        int index = 0;
        for (Transaction.Input i : inputs) {
            UTXO utxo = new UTXO(i.prevTxHash, i.outputIndex);

            if (!currentPool.contains(utxo)) {
                return false;
            }

//        (2)
            Transaction.Output corr_output = currentPool.getTxOutput(utxo);
            inputSum += corr_output.value;
            if (!Crypto.verifySignature(corr_output.address, tx.getRawDataToSign(index++), i.signature)) {
                return false;
            }

//          (3)
            if (tx_UTXOs.contains(utxo)) return false;
            tx_UTXOs.addUTXO(utxo, corr_output);
        }


//        (4)
        for (Transaction.Output txO : outputs) {
            if (txO.value < 0) {
                return false;
            }
        }

//        (5)
        for (Transaction.Output txO : outputs) {
            outputSum += txO.value;
        }

        return inputSum >= outputSum;
    }


    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        //Initialize an array with valid Transactions
        ArrayList<Transaction> validTransactions = new ArrayList<>();

        for(Transaction tx_i : possibleTxs) {
            if(isValidTx(tx_i)){
                validTransactions.add(tx_i);
//              Update the current UTXO pool as appropriate.
                for (Transaction.Input in : tx_i.getInputs()) {
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    currentPool.removeUTXO(utxo);
                }
                for (int i = 0; i < tx_i.numOutputs(); i++) {
                    Transaction.Output out = tx_i.getOutput(i);
                    UTXO utxo = new UTXO(tx_i.getHash(), i);
                    currentPool.addUTXO(utxo, out);
                }
            }
        }

        Transaction[] vTx = new Transaction[validTransactions.size()];
        int counter = 0;
        for(Transaction t : validTransactions) {
            vTx[counter++] = t;
        }

        return vTx;
    }

}
