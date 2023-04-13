package com.example;

import com.example.flows.AskQuoteFlow;
import com.example.flows.SendQuoteFlow;
import com.example.states.Quote;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.concurrent.Future;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;
    @Before
    public void setup() {
        MockNetworkParameters mockNetworkParameters = new MockNetworkParameters().withCordappsForAllNodes(
                Arrays.asList(
                        TestCordapp.findCordapp("com.example.contracts"),
                        TestCordapp.findCordapp("com.example.flows")
                )
        ).withNotarySpecs(Arrays.asList(new MockNetworkNotarySpec(new CordaX500Name("Notary", "London", "GB"))));
        network = new MockNetwork(mockNetworkParameters);
        a = network.createNode(new MockNodeParameters());
        b = network.createNode(new MockNodeParameters());
        c = network.createNode(new MockNodeParameters());
        ArrayList<StartedMockNode> startedNodes = new ArrayList<>();
        startedNodes.add(a);
        startedNodes.add(b);
        startedNodes.add(c);
        startedNodes.forEach(el -> el.registerInitiatedFlow(AskQuoteFlow.AskQuoteFlowResponder.class));
        startedNodes.forEach(el -> el.registerInitiatedFlow(SendQuoteFlow.SendQuoteFlowResponder.class));
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void AskQuoteCorrectDemandingOfQuote() throws Exception {

        Party sender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party receiver = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        AskQuoteFlow.AskQuoteFlowInitiator flow = new AskQuoteFlow.AskQuoteFlowInitiator(sender, receiver);

        Future<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();


        SignedTransaction ptx = future.get();

        // Print the transaction for debugging purposes.
        System.out.println(ptx.getTx());
        assert (ptx.getTx().getInputs().isEmpty());
        assert (ptx.getTx().getOutputs().get(0).getData() instanceof Quote);
        assert ( ((Quote) ptx.getTx().getOutputs().get(0).getData()).getQuote()==-1);

    }
    
}
