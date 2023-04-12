package com.example.states;

import com.example.contracts.QuoteContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;

import java.util.ArrayList;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(QuoteContract.class)
public class Quote implements ContractState {

    //private variables
    private int quote;
    private Party sender;
    private List<Party> receiver;
    private boolean second_category;
    private boolean first_category;
    private boolean accepted;
    private boolean rejected;



    /* Constructor of your Corda state */

    public Quote(int quote, Party sender, List<Party> receiver, boolean second_category, boolean first_category, boolean accepted, boolean rejected) {
        this.quote = quote;
        this.sender = sender;
        this.receiver = receiver;
        this.second_category = second_category;
        this.first_category = first_category;
        this.accepted = accepted;
        this.rejected = rejected;
    }

    //getters
    public int getQuote(){
        return quote;
    }

    public Party getSender() {
        return sender;
    }

    public List<Party> getReceiver() {
        return receiver;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public boolean isRejected() {
        return rejected;
    }

    public boolean isFirst_category() {
        return first_category;
    }

    public boolean isSecond_category() {
        return second_category;
    }


    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> entities= new ArrayList<AbstractParty>();
        entities.add(sender);
        entities.addAll(receiver);
        return entities;
    }
}