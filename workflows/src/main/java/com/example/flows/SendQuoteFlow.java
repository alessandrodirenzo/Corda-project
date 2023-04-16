package com.example.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contracts.QuoteContract;
import com.example.states.Quote;
import net.corda.core.contracts.AttachmentResolutionException;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
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
import java.util.UUID;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;


public class SendQuoteFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class SendQuoteFlowInitiator extends FlowLogic<SignedTransaction>{

        //private variables
        private UniqueIdentifier quoteId;
        private int quote;
        //public constructor
        public SendQuoteFlowInitiator(UniqueIdentifier quoteId, int quote) {
            this.quoteId = quoteId;
            this.quote=quote;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            String message = "This is the best price I can offer.";
            QueryCriteria queryCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            Vault.Page results = getServiceHub().getVaultService().queryBy(Quote.class, queryCriteria);
            StateAndRef inputStateAndRef= (StateAndRef) results.getStates().get(0);
            final Quote input = (Quote) inputStateAndRef.getState().getData();
            final Party sender= input.getReceiver().get(0);
            final Party receiver= input.getSender();
            final Quote output = new Quote(input.getId(), quote,message,sender,Arrays.asList(receiver), false, false, false, false);
            System.out.println("--------------------------------");
            System.out.println("Stato info: "+output.toString());
            Party notary = inputStateAndRef.getState().getNotary();
            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the iou as an output state, as well as a command to the transaction builder.
            builder.addInputState(inputStateAndRef);
            builder.addOutputState(output);
            builder.addCommand(new QuoteContract.Commands.SendQuote(), Arrays.asList(sender.getOwningKey(),receiver.getOwningKey()) );


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

                }


            }
            final SignTxFlow signTxFlow = new SignTxFlow(counterpartySession);
            final SecureHash txId = subFlow(signTxFlow).getId();

            subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
            return null;
        }
    }
}


