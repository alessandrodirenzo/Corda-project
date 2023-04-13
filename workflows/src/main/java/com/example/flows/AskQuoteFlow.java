package com.example.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contracts.QuoteContract;
import com.example.states.Quote;

import net.corda.core.flows.*;

import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class AskQuoteFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class AskQuoteFlowInitiator extends FlowLogic<SignedTransaction>{

        //private variables
        private Party sender ;
        private Party receiver;

        //public constructor
        public AskQuoteFlowInitiator(Party sender, Party receiver ) {
            this.sender = sender;
            this.receiver=receiver;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            int quote = -1;
            String message= "I would be interested in purchasing raw materials from you. Can you send me the quote for 500 pieces?";
            this.sender = getOurIdentity();

            // Step 1. Get a reference to the notary service on our network and our key pair.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            //Step 2. Final state
            final Quote output = new Quote(quote,message,sender,Arrays.asList(receiver), false, false, false, false);

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the iou as an output state, as well as a command to the transaction builder.
            builder.addOutputState(output);
            builder.addCommand(new QuoteContract.Commands.AskProposal(), Arrays.asList(this.sender.getOwningKey(),this.receiver.getOwningKey()) );


            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);


            // Step 6. Collect the other party's signature using the SignTransactionFlow.
            List<Party> otherParties = output.getParticipants().stream().map(el -> (Party)el).collect(Collectors.toList());
            otherParties.remove(getOurIdentity());
            List<FlowSession> sessions = otherParties.stream().map(el -> initiateFlow(el)).collect(Collectors.toList());

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(stx, sessions));
        }
    }

    @InitiatedBy(AskQuoteFlowInitiator.class)
    public static class AskQuoteFlowResponder extends FlowLogic<Void>{
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public AskQuoteFlowResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                }
            });
            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
        }
    }

