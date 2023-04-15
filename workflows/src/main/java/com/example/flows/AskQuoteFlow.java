package com.example.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contracts.QuoteContract;
import com.example.states.Quote;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;

import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static net.corda.core.contracts.ContractsDSL.requireThat;


public class AskQuoteFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class AskQuoteFlowInitiator extends FlowLogic<SignedTransaction> {

        //private variables
        private int quote;
        private Party receiver;

        //public constructor
        public AskQuoteFlowInitiator(int quote, Party receiver) {
            this.quote = quote;
            this.receiver = receiver;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {


            String message = "I would be interested in purchasing raw materials from you. Can you send me the quote for 500 pieces?";
            Party sender = getOurIdentity();

            // Step 1. Get a reference to the notary service on our network and our key pair.

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            UniqueIdentifier id= new UniqueIdentifier();

            //Step 2. Final state
            final Quote output = new Quote(id, quote, message, sender, Arrays.asList(receiver), false, false, false, false);

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the iou as an output state, as well as a command to the transaction builder.
            builder.addOutputState(output);
            builder.addCommand(new QuoteContract.Commands.AskQuote(), Arrays.asList(sender.getOwningKey(), this.receiver.getOwningKey()));


            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // Step 6. Collect the other party's signature using the SignTransactionFlow.
            FlowSession otherPartySession = initiateFlow(receiver);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(ptx, Arrays.asList(otherPartySession), CollectSignaturesFlow.Companion.tracker()));
            return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)));
        }
    }

    @InitiatedBy(AskQuoteFlowInitiator.class)
    public static class AskQuoteFlowResponder extends FlowLogic<Void> {
        //private variable
        private final FlowSession counterpartySession;

        //Constructor
        public AskQuoteFlowResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow) {
                    super(otherPartyFlow);
                }


                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    requireThat(require -> {

                        stx.getTx().getInputs().isEmpty();
                        return null;
                    });
                }


            }
            final SignTxFlow signTxFlow = new SignTxFlow(counterpartySession);
            final SecureHash txId = subFlow(signTxFlow).getId();

            subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
            return null;
        }
    }
}



