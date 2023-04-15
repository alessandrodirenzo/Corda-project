package com.example.states;

import com.example.contracts.QuoteContract;
import net.corda.core.contracts.BelongsToContract;

import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(QuoteContract.class)
public class Quote implements ContractState{


    //private variables
    private final UniqueIdentifier id;
    private String message;
    private int quote;
    private Party sender;
    private List<Party> receiver;
    private boolean second_category;
    private boolean first_category;
    private boolean accepted;
    private boolean rejected;



    /* Constructor of your Corda state */

    public Quote(UniqueIdentifier id, int quote, String message, Party sender, List<Party> receiver, boolean second_category, boolean first_category, boolean accepted, boolean rejected) {
        this.id = id;
        this.message=message;
        this.quote = quote;
        this.sender = sender;
        this.receiver = receiver;
        this.second_category = second_category;
        this.first_category = first_category;
        this.accepted = accepted;
        this.rejected = rejected;
    }

    //getters

    public String getMessage() { return message; }

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

    @Override
    public String toString() {
        return "Quote{" +
                "linearId=" + id +
                ", message='" + message + '\'' +
                ", quote=" + quote +
                ", sender=" + sender +
                ", receiver=" + receiver +
                ", second_category=" + second_category +
                ", first_category=" + first_category +
                ", accepted=" + accepted +
                ", rejected=" + rejected +
                '}';
    }

    public UniqueIdentifier getId() {
        return null;
    }
}