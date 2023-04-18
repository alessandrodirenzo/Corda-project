package com.example.contracts;

import com.example.states.Quote;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;


import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.transaction;


public class ContractTests {
    private MockServices ledgerServices = new MockServices(new TestIdentity(new CordaX500Name("TestId", "", "IN")));;
    private final TestIdentity compA= new TestIdentity(new CordaX500Name("Company A",  "London",  "GB"));
    private final TestIdentity compB = new TestIdentity(new CordaX500Name("Company B",  "Rome",  "IT"));
    private final TestIdentity compC = new TestIdentity(new CordaX500Name("Company C",  "Berlin",  "DE"));
    private Quote state0 = new Quote(new UniqueIdentifier(), 10,"", compA.getParty(), Arrays.asList(compB.getParty()), false, false, false, false);
    private Quote state1 = new Quote(new UniqueIdentifier(), -1,"", compA.getParty(), Arrays.asList(compB.getParty()), false, false, false, false);
    private Quote state2 = new Quote(new UniqueIdentifier(), 300,"", compB.getParty(), Arrays.asList(compA.getParty()), false, false, false, false);
    private Quote state3 = new Quote(new UniqueIdentifier(), 300,"", compA.getParty(), Arrays.asList(compB.getParty()), false, false, false, false);
    private Quote state4 = new Quote(new UniqueIdentifier(), 300,"", compA.getParty(), Arrays.asList(compC.getParty()), true, false, false, true);
    private Quote state5 = new Quote(new UniqueIdentifier(), 300,"", compB.getParty(), Arrays.asList(compC.getParty()), true, false, false, true);
    private Quote state6 = new Quote(new UniqueIdentifier(), 300,"", compA.getParty(), Arrays.asList(compB.getParty()), true, true, false, true);
    private Quote state7 = new Quote(new UniqueIdentifier(), 300,"", compA.getParty(), Arrays.asList(compC.getParty()), true, true, false, true);

    @Test
    public void AskQuoteCommandTest() {
        transaction(ledgerServices, tx -> {
            tx.input(QuoteContract.ID, state1);
            tx.output(QuoteContract.ID, state1);
            tx.command(Arrays.asList(compA.getPublicKey(), compB.getPublicKey()), new QuoteContract.Commands.AskQuote());
            tx.fails(); //fails because of having inputs
            return null;
        });

        transaction(ledgerServices, tx ->  {
            tx.output(QuoteContract.ID, state0);
            tx.command(Arrays.asList(compA.getPublicKey(), compB.getPublicKey()), new QuoteContract.Commands.AskQuote());
            tx.fails();
            return null;
        });
        
        transaction(ledgerServices, tx ->  {
                tx.output(QuoteContract.ID, state1);
                tx.command(Arrays.asList(compA.getPublicKey(), compB.getPublicKey()), new QuoteContract.Commands.AskQuote());
                tx.verifies();
                return null;
            });


    }

    @Test
    public void SendQuoteTest(){
        transaction(ledgerServices, tx -> {
            tx.input(QuoteContract.ID, state1);
            tx.output(QuoteContract.ID, state2);
            tx.command(Arrays.asList(compB.getPublicKey(), compA.getPublicKey()), new QuoteContract.Commands.SendQuote());
            tx.verifies();
            return null;
        });

        transaction(ledgerServices, tx ->  {
            tx.input(QuoteContract.ID, state1);
            tx.output(QuoteContract.ID, state3);
            tx.command(Arrays.asList(compA.getPublicKey(), compB.getPublicKey()), new QuoteContract.Commands.SendQuote());
            tx.fails();
            return null;
        });
    }
    @Test
    public void RejectionIntentionTest(){
        transaction(ledgerServices, tx -> {
            tx.input(QuoteContract.ID, state2);
            tx.output(QuoteContract.ID, state4);
            tx.command(Arrays.asList(compA.getPublicKey(), compC.getPublicKey()), new QuoteContract.Commands.RejectionIntention());
            tx.verifies();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.input(QuoteContract.ID, state2);
            tx.output(QuoteContract.ID, state5);
            tx.command(Arrays.asList(compB.getPublicKey(), compC.getPublicKey()), new QuoteContract.Commands.RejectionIntention());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.input(QuoteContract.ID, state1);
            tx.output(QuoteContract.ID, state5);
            tx.command(Arrays.asList(compB.getPublicKey(), compC.getPublicKey()), new QuoteContract.Commands.RejectionIntention());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.input(QuoteContract.ID, state2);
            tx.output(QuoteContract.ID, state6);
            tx.command(Arrays.asList(compB.getPublicKey(), compC.getPublicKey()), new QuoteContract.Commands.RejectionIntention());
            tx.fails();
            return null;
        });
    }
    @Test
    public void RejectionConfirmedTest(){
        transaction(ledgerServices, tx -> {
            tx.input(QuoteContract.ID, state4);
            tx.output(QuoteContract.ID, state6);
            tx.command(Arrays.asList(compA.getPublicKey(), compB.getPublicKey()), new QuoteContract.Commands.RejectionConfirmed());
            tx.verifies();
            return null;
        });
        transaction(ledgerServices, tx -> {
            tx.input(QuoteContract.ID, state5);
            tx.output(QuoteContract.ID, state6);
            tx.command(Arrays.asList(compB.getPublicKey(), compA.getPublicKey()), new QuoteContract.Commands.RejectionConfirmed());
            tx.fails();
            return null;
        });
        transaction(ledgerServices, tx -> {
            tx.input(QuoteContract.ID, state4);
            tx.output(QuoteContract.ID, state7);
            tx.command(Arrays.asList(compB.getPublicKey(), compA.getPublicKey()), new QuoteContract.Commands.RejectionConfirmed());
            tx.fails();
            return null;
        });
    }
}