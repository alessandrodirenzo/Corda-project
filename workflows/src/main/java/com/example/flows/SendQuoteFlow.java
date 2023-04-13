package com.example.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contracts.QuoteContract;
import com.example.states.Quote;
import net.corda.core.contracts.AttachmentResolutionException;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;



public class SendQuoteFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class SendQuoteFlowInitiator extends FlowLogic<SignedTransaction>{

        //private variables
        private final UniqueIdentifier quoteId;

        //public constructor
        public SendQuoteFlowInitiator(UniqueIdentifier quoteId) {
            this.quoteId = quoteId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            int quote = 1000;
            String message = "This is the best price I can offer.";
            List<UUID> listOfLinearIds = new ArrayList<>();
            listOfLinearIds.add(quoteId.getId());
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);
            Vault.Page results = getServiceHub().getVaultService().queryBy(Quote.class, queryCriteria);
            StateAndRef inputStateAndRef = (StateAndRef) results.getStates().get(0);
            final Quote input = (Quote) inputStateAndRef.getState().getData();
            final Party sender= input.getReceiver().get(0);
            final Party receiver= input.getSender();
            final Quote output = new Quote(quote,message,sender,Arrays.asList(receiver), false, false, false, false);

            Party notary = inputStateAndRef.getState().getNotary();
            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the iou as an output state, as well as a command to the transaction builder.
            builder.addInputState(inputStateAndRef);
            builder.addOutputState(output);
            builder.addCommand(new QuoteContract.Commands.SendProposal(), Arrays.asList(sender.getOwningKey(),receiver.getOwningKey()) );


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

    @InitiatedBy(SendQuoteFlowInitiator.class)
    public static class SendQuoteFlowResponder extends FlowLogic<Void>{
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public SendQuoteFlowResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {

                        Quote outputState= (Quote) stx.getCoreTransaction().getOutputStates().get(0);
                        try {
                            LedgerTransaction ledgerTx = stx.toLedgerTransaction(getServiceHub(), false);
                            Party rec_of_prev_transaction= ledgerTx.inputsOfType(Quote.class).get(0).getReceiver().get(0);
                            if(!rec_of_prev_transaction.equals(outputState.getSender())){
                                throw new FlowException("Only the supplier can make a proposal of quote");
                            }
                        } catch (SignatureException e) {
                            e.printStackTrace();
                        } catch (AttachmentResolutionException e) {
                            e.printStackTrace();
                        } catch (TransactionResolutionException e) {
                            e.printStackTrace();
                        }


                    };
            });
            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }
}


