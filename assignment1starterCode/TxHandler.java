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

//        (1)

        int index = 0;
        for (Transaction.Input i : inputs) {
            UTXO utxo = new UTXO(i.prevTxHash, i.outputIndex);

            if (!currentPool.contains(utxo)) {
                return false;
            }

//        (2)
            Transaction.Output corr_output = currentPool.getTxOutput(utxo);
            if (!Crypto.verifySignature(corr_output.address, tx.getRawDataToSign(index++), i.signature)) {
                return false;
            }

//          (3)
            if (tx_UTXOs.contains(utxo)) return false;
            tx_UTXOs.addUTXO(utxo, corr_output);
        }


//        (4)
        boolean test_4 = true;

        for (Transaction.Output txO : outputs) {
            if (txO.value < 0) {
                test_4 = false;
            }
        }

//        (5)
        double inputSum = 0.0;
        double outputSum = 0.0;
        for (Transaction.Input txI : inputs) {
            inputSum += outputs.get(txI.outputIndex).value;
        }
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
        //Initialize an array, where the biggest possible size is the case when all transactions are valid
        ArrayList<Transaction> validTransactions = new ArrayList<>();

//          For every output of every TX do: Compare against every output of every TX and evaluate:
//          if output is the same, but the transaction is different, then double spend has been violated

        for(Transaction tx_i : possibleTxs) {
            //Make sure that multiple TXs don't spend the same output
            boolean doubleSpend = false;
            for(Transaction.Output tx_io : tx_i.getOutputs()) {
                for(Transaction tx_j : possibleTxs) {
                    for(Transaction.Output tx_jo : tx_j.getOutputs()) {
                        if(!tx_i.equals(tx_j) && tx_io.equals(tx_jo)) {
                            doubleSpend = true;
                        }
                    }
                }
            }

            if(!doubleSpend && isValidTx(tx_i)){
//                  If the transaction does not violate double spend and is validated using the validation method,
//                  then add it to the list of valid transactions
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
        return vTx;
    }

}
