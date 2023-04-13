package com.example.contracts;

import com.example.states.Quote;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;


import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.transaction;


public class ContractTests {
    private MockServices ledgerServices = new MockServices(new TestIdentity(new CordaX500Name("TestId", "", "IN")));;
    private final TestIdentity compA= new TestIdentity(new CordaX500Name("Company A",  "London",  "UK"));
    private final TestIdentity compB = new TestIdentity(new CordaX500Name("Company B",  "Rome",  "IT"));
    private final TestIdentity compC = new TestIdentity(new CordaX500Name("Company C",  "Berlin",  "DE"));
    private Quote state = new Quote(5, compA.getParty(), Arrays.asList(compB.getParty(), compC.getParty()), false, false, false, false);

    @Test
    public void InputandOutputState() {
        transaction(ledgerServices, tx -> {
                tx.input(QuoteContract.ID, state);
                tx.output(QuoteContract.ID, state);
                tx.command(Arrays.asList(compA.getPublicKey(), compB.getPublicKey(), compC.getPublicKey()), new QuoteContract.Commands.AskAndRecProposal());
                tx.fails(); //fails because of having inputs
                return null;
            });
        transaction(ledgerServices, tx ->  {
                tx.output(QuoteContract.ID, state);
                tx.command(Arrays.asList(compA.getPublicKey(), compB.getPublicKey(), compC.getPublicKey()), new QuoteContract.Commands.AskAndRecProposal());
                tx.verifies();
                return null;
            });


    }
}