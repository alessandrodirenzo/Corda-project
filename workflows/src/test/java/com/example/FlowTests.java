package com.example;

import com.google.common.collect.ImmutableList;
import com.example.flows.AskQuoteFlow;
import com.example.states.Quote;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Future;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.example.contracts"),
                TestCordapp.findCordapp("com.example.flows")))
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB")))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void dummyTest() {
        //Modify it
        AskQuoteFlow.AskQuoteFlowInitiator flow= new AskQuoteFlow.AskQuoteFlowInitiator(a.getInfo().getLegalIdentities().get(0), b.getInfo().getLegalIdentities().get(0));
        Future<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        //successful query means the state is stored at node b's vault. Flow went through.
        QueryCriteria inputCriteria = new QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED);
        Quote state = b.getServices().getVaultService().queryBy(Quote.class,inputCriteria).getStates().get(0).getState().getData();
    }
}
