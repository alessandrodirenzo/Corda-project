package com.example;

import com.example.flows.AskQuoteFlow;
import com.example.flows.SendQuoteFlow;
import com.example.states.Quote;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Collections;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;


public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;

    @Before
    public void setup() {
        MockNetworkParameters mockNetworkParameters = new MockNetworkParameters(
                Arrays.asList(
                        TestCordapp.findCordapp("com.example.contracts"),
                        TestCordapp.findCordapp("com.example.flows")
                )).withNetworkParameters(new NetworkParameters(4, Collections.emptyList(),
                        10485760, 10485760 * 50, Instant.now(), 1,
                        Collections.emptyMap())
        ).withNotarySpecs(Arrays.asList(new MockNetworkNotarySpec(new CordaX500Name("Notary", "London", "GB"))));
        network = new MockNetwork(mockNetworkParameters);
        a = network.createPartyNode(new CordaX500Name("Manufacturing Company", "London", "GB"));
        b = network.createPartyNode(new CordaX500Name("Supplier", "Manchester", "GB"));
        c = network.createPartyNode(new CordaX500Name("Bank", "London", "GB"));
        network.runNetwork();

    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void AskQuoteCorrectDemandingOfQuote() throws Exception {

        AskQuoteFlow.AskQuoteFlowInitiator flow = new AskQuoteFlow.AskQuoteFlowInitiator(-1, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future=a.startFlow(flow);
        network.runNetwork();
        SignedTransaction ptx= future.get();
        assert (ptx.getTx().getOutputs().get(0).getData() instanceof Quote);
        assert ( ((Quote) ptx.getTx().getOutputs().get(0).getData()).getQuote()==-1);
        assert(a.getServices().getVaultService().queryBy(Quote.class).getStates().get(0).getState().getData() instanceof Quote);
        assert(b.getServices().getVaultService().queryBy(Quote.class).getStates().get(0).getState().getData() instanceof Quote);
        assertEquals(a.getServices().getVaultService().queryBy(Quote.class).getStates().get(0).getState().getData().getQuote(),-1);
        assertEquals(a.getServices().getVaultService().queryBy(Quote.class).getStates().get(0).getState().getData().getQuote(),b.getServices().getVaultService().queryBy(Quote.class).getStates().get(0).getState().getData().getQuote());
        assertEquals(c.getServices().getVaultService().queryBy(Quote.class).getStates().size(),0);

    }

    @Test
    public void SendQuoteCorrect() throws Exception {
      
        AskQuoteFlow.AskQuoteFlowInitiator flow = new AskQuoteFlow.AskQuoteFlowInitiator(-1, b.getInfo().getLegalIdentities().get(0));
        a.startFlow(flow);
        network.runNetwork();
        UniqueIdentifier id= a.getServices().getVaultService().queryBy(Quote.class).getStates().get(0).getState().getData().getId();
        SendQuoteFlow.SendQuoteFlowInitiator f= new SendQuoteFlow.SendQuoteFlowInitiator(id,100);
        CordaFuture<SignedTransaction> future=b.startFlow(f);
        network.runNetwork();
        SignedTransaction ptx= future.get();

        assert(ptx.getTx().getOutputs().get(0).getData() instanceof Quote);
        assert(!ptx.getInputs().isEmpty());
        assertEquals(a.getServices().getVaultService().queryBy(Quote.class).getStates().get(0).getState().getData().getQuote(),100);
        assertEquals(b.getServices().getVaultService().queryBy(Quote.class).getStates().get(0).getState().getData().getQuote(),100);
        assertEquals(a.getServices().getVaultService().queryBy(Quote.class).getStates().get(0).getState().getData().getId(), id);
        assertEquals(b.getServices().getVaultService().queryBy(Quote.class).getStates().get(0).getState().getData().getId(), id);
    }


    
}
