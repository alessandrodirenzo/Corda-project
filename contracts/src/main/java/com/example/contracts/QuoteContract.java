package com.example.contracts;

import com.example.states.Quote;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class QuoteContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.example.contracts.QuoteContract";

    // A transaction is valid if verify() method of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {

        /* We can use the requireSingleCommand function to extract command data from transaction.
         * However, it is possible to have multiple commands in a single transaction.*/
        //final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final CommandData commandData = tx.getCommands().get(0).getValue();

        if (commandData instanceof Commands.AskProposal) {
            //Retrieve the output state of the transaction
            Quote output = tx.outputsOfType(Quote.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("No inputs should be consumed when asking the proposal.", tx.getInputStates().isEmpty());
                require.using("No decision made", (!output.isAccepted()) && (!output.isRejected()));
                require.using("No approval of decision", (!output.isFirst_category()) && (!output.isSecond_category()));
                require.using("Quote with negative value representing no quote already sent",output.getQuote()==-1);
                return null;
            });
        }
        if (commandData instanceof Commands.SendProposal) {
            //Retrieve the output state of the transaction
            Quote output = tx.outputsOfType(Quote.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("Input state present", !tx.getInputStates().isEmpty());
                require.using("Quote with positive value",output.getQuote()>0);
                require.using("No decision made", (!output.isAccepted()) && (!output.isRejected()));
                require.using("No approval of decision", (!output.isFirst_category()) && (!output.isSecond_category()));
                return null;
            });
        }

        if (commandData instanceof Commands.RejectionIntention) {
            //Retrieve the output state of the transaction
            Quote input= tx.inputsOfType(Quote.class).get(0);
            Quote output = tx.outputsOfType(Quote.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("Input state present", !tx.getInputStates().isEmpty());
                require.using("Input quote without decisions", (!input.isAccepted()) && (!input.isRejected()));
                require.using("Input quote without approval of validators", (!input.isFirst_category()) && (!input.isSecond_category()));
                require.using("Quote didn't change", output.getQuote()==input.getQuote());
                require.using("Intention of rejection", !output.isAccepted()&&output.isRejected());
                require.using("Only validators of second category required to approve", output.isSecond_category()&&!output.isFirst_category());
                return null;
            });
        }
        if (commandData instanceof Commands.RejectionConfirmed) {
            //Retrieve the output state of the transaction
            Quote input= tx.inputsOfType(Quote.class).get(0);
            Quote output = tx.outputsOfType(Quote.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("Input state present", !tx.getInputStates().isEmpty());
                require.using("Input quote with decision of rejection", (!input.isAccepted()) && (input.isRejected()));
                require.using("Input quote with approval of  of second category", (!input.isFirst_category()) && (input.isSecond_category()));
                require.using("Quote didn't change", output.getQuote()==input.getQuote());
                require.using("Rejection confirmed", output.isAccepted()==input.isAccepted() && output.isRejected()==input.isRejected());
                require.using("Validators of first category required to approve", output.isFirst_category());
                return null;
            });
        }

    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class AskProposal implements Commands {}
        class SendProposal implements Commands {}
        class RejectionIntention implements Commands {}
        class RejectionConfirmed implements Commands {}
    }
}