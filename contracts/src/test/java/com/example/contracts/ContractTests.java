package com.example.contracts;

import com.example.states.Quote;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;


import java.util.Arrays;


import static net.corda.testing.node.NodeTestUtils.ledger;


public class ContractTests {
    private final MockServices ledgerServices = new MockServices(Arrays.asList("com.example"));
    TestIdentity compA= new TestIdentity(new CordaX500Name("Company A",  "London",  "UK"));
    TestIdentity compB = new TestIdentity(new CordaX500Name("Company B",  "Rome",  "IT"));
    TestIdentity compC = new TestIdentity(new CordaX500Name("Company C",  "Berlin",  "DE"));
    @Test
    public void InputandOutputState() {
        Quote state = new Quote(5, compA.getParty(), Arrays.asList(compB.getParty(), compC.getParty()), false, false, false, false);
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(QuoteContract.ID, state);
                tx.output(QuoteContract.ID, state);
                tx.command(compA.getPublicKey(), new QuoteContract.Commands.AskAndRecProposal());
                return tx.fails(); //fails because of having inputs
            });
            l.transaction(tx -> {
                tx.output(QuoteContract.ID, state);
                tx.command(compA.getPublicKey(), new QuoteContract.Commands.AskAndRecProposal());
                return tx.verifies();
            });
            return null;
        });
    }
}