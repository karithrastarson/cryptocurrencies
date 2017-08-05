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
currentPool.

    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        boolean Tx_validity = false;
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();

//        List will be used for rule 3
        ArrayList<UTXO> tx_UTXOs = new ArrayList<>();
//        (1)
        boolean test_1 = true;
        int index = 0;
        for(Transaction.Output o : outputs) {
            UTXO utxo = new UTXO(tx.getHash(), index++);
            //Add it to a list for later use

            tx_UTXOs.add(utxo);
            if(!currentPool.contains(utxo)){
                test_1 = false;
            }
        }

//        (2)
        boolean test_2 = true;
        for(Transaction.Input txI : inputs) {
            //get corresponding output for the public key
            Transaction.Output corr_output = outputs.get(txI.outputIndex);
            if(!Crypto.verifySignature(corr_output.address,tx.getHash(), txI.signature)) {
                test_2 = false;
            }
        }

//        (3)
        boolean test_3 = true;

        for(UTXO i : tx_UTXOs) {
            int counter = 0;
            for(UTXO j : tx_UTXOs){
                if(i.equals(j)) {
                    counter++;
                }
            }
            if (counter > 1) {
                test_3 = false;
            }
        }

//        (4)
        boolean test_4 = true;

        for(Transaction.Output txO : outputs) {
            if(txO.value < 0) {
                test_4 = false;
            }
        }

//        (5)
        boolean test_5 = true;
        double inputSum = 0.0;
        double outputSum = 0.0;
        for(Transaction.Input txI : inputs) {
            inputSum += outputs.get(txI.outputIndex).value;
        }
        for(Transaction.Output txO : outputs) {
            outputSum += txO.value;
        }
        if(inputSum < outputSum) {
            test_5 = false;
        }

        return test_1 && test_2 && test_3 && test_4 && test_5;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        //Initialize an array, where the biggest possible size is the case when all transactions are valid
        Transaction[] validTransactions = new Transaction[possibleTxs.length];

//          For every output of every TX do: Compare against every output of every TX and evaluate:
//          if output is the same, but the transaction is different, then double spend has been violated

        int validIndex = 0;
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
                validTransactions[validIndex++] = tx_i;

            }
        }
        return validTransactions;
    }

}
