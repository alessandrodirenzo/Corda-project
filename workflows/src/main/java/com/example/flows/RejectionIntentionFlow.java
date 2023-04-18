package com.example.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contracts.QuoteContract;
import com.example.states.Quote;
import net.corda.core.contracts.AttachmentResolutionException;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class RejectionIntentionFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class RejectionIntentionFlowInitiator extends FlowLogic<SignedTransaction>{
        //private variables
        private Party receiver;
        //public constructor
        public RejectionIntentionFlowInitiator( Party receiver) {
            this.receiver=receiver;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            String message = "I have the intention to reject the proposal due to the expensive cipher and I ask for your approval.";
            final Party sender= getOurIdentity();
            QueryCriteria queryCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            Vault.Page results = getServiceHub().getVaultService().queryBy(Quote.class, queryCriteria);
            StateAndRef inputStateAndRef= (StateAndRef) results.getStates().get(0);
            final Quote input = (Quote) inputStateAndRef.getState().getData();
            final Party otherreceiver= ((Quote) inputStateAndRef.getState().getData()).getSender();
            final Quote output = new Quote(input.getId(), input.getQuote(),message,sender,Arrays.asList(receiver), true, false, false, true);
            Party notary = inputStateAndRef.getState().getNotary();
            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add inputState, outputState and Command.
            builder.addInputState(inputStateAndRef);
            builder.addOutputState(output);
            builder.addCommand(new QuoteContract.Commands.RejectionIntention(), Arrays.asList(sender.getOwningKey(),receiver.getOwningKey()) );


            // Step 5. Verify and sign it.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // Step 6. Collect the other party's signature using the SignTransactionFlow.
            ArrayList<AbstractParty> parties= new ArrayList<>();
            parties= (ArrayList) output.getParticipants();

            List<FlowSession> signerFlows = parties.stream()
                    // We don't need to inform ourselves and we signed already.
                    .filter(it -> !it.equals(getOurIdentity()))
                    .map(this::initiateFlow)
                    .collect(Collectors.toList());
            FlowSession extra=initiateFlow(otherreceiver);

            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(ptx, signerFlows, CollectSignaturesFlow.Companion.tracker()));
            signerFlows.add(extra);
            return subFlow(new FinalityFlow(fullySignedTx, signerFlows));

        }
    }
    @InitiatedBy(RejectionIntentionFlow.RejectionIntentionFlowInitiator.class)
    public static class RejectionIntentionFlowResponder extends FlowLogic<Void>{
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public RejectionIntentionFlowResponder(FlowSession counterpartySession) {
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
                    Quote outputState= (Quote) stx.getCoreTransaction().getOutputStates().get(0);
                    try {
                        LedgerTransaction ledgerTx = stx.toLedgerTransaction(getServiceHub(), false);
                        Party rec_of_prev_transaction= ledgerTx.inputsOfType(Quote.class).get(0).getReceiver().get(0);
                        Party send_of_prev_transaction= ledgerTx.inputsOfType(Quote.class).get(0).getSender();
                        if(outputState.getSender().getName().equals("O=Supplier,L=Manchester,C=GB")){
                            throw new FlowException("You are not the Manufacturing Company");
                        }
                        if(rec_of_prev_transaction.equals(outputState.getReceiver()) || send_of_prev_transaction.equals(outputState.getReceiver())){
                            throw new FlowException("I'm not receiver of second category");
                        }
                    } catch (SignatureException e) {
                        e.printStackTrace();
                    } catch (AttachmentResolutionException e) {
                        e.printStackTrace();
                    } catch (TransactionResolutionException e) {
                        e.printStackTrace();
                    }

                }


            }
            final SignTxFlow signTxFlow = new SignTxFlow(counterpartySession);
            final SecureHash txId = subFlow(signTxFlow).getId();

            subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
            return null;
        }
    }
}
