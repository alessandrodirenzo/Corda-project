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

        if (commandData instanceof Commands.AskQuote) {
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
        if (commandData instanceof Commands.SendQuote) {
            //Retrieve the input and output state of the transaction
            Quote input= tx.inputsOfType(Quote.class).get(0);
            Quote output = tx.outputsOfType(Quote.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("Input state present", input!=null);
                require.using("Quote with positive value",output.getQuote()>0);
                require.using("No decision made", (!output.isAccepted()) && (!output.isRejected()));
                require.using("No approval of decision", (!output.isFirst_category()) && (!output.isSecond_category()));
                require.using("Only the supplier can send the quote", output.getSender().equals(input.getReceiver().get(0)));
                return null;
            });
        }

        if (commandData instanceof Commands.RejectionIntention) {
            //Retrieve the input and output state of the transaction
            Quote input= tx.inputsOfType(Quote.class).get(0);
            Quote output = tx.outputsOfType(Quote.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("Input state present", !tx.getInputStates().isEmpty());
                require.using("Input quote without decisions", (!input.isAccepted()) && (!input.isRejected()));
                require.using("Input quote without approval of validators", (!input.isFirst_category()) && (!input.isSecond_category()));
                require.using("Quote didn't change", output.getQuote()==input.getQuote());
                require.using("Positive quote", output.getQuote()>0);
                require.using("Intention of rejection", !output.isAccepted()&&output.isRejected());
                require.using("Only validators of second category required to approve", output.isSecond_category()&&!output.isFirst_category());
                require.using("Only the Manufacturing Company can reject or approve the quote", !output.getSender().equals(input.getSender()));
                return null;
            });
        }
        if (commandData instanceof Commands.RejectionConfirmed) {
            //Retrieve the input and output state of the transaction
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
                require.using("Only the Manufacturing Company can reject or approve the quote", input.getSender().equals(output.getSender()));
                require.using("Wrong receiver",!input.getReceiver().get(0).equals(output.getReceiver().get(0)));
                return null;
            });
        }

    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class AskQuote implements Commands {}
        class SendQuote implements Commands {}
        class RejectionIntention implements Commands {}
        class RejectionConfirmed implements Commands {}
    }
}