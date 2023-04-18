package com.example.contracts;

import com.example.states.Quote;
import net.corda.core.identity.Party;
import org.junit.Test;
import java.util.List;


public class StateTests {


    @Test
    public void hasFieldsOfCorrectType() throws NoSuchFieldException {
        Quote.class.getDeclaredField("quote");
        Quote.class.getDeclaredField("message");
        Quote.class.getDeclaredField("sender");
        Quote.class.getDeclaredField("receiver");
        Quote.class.getDeclaredField("first_category");
        Quote.class.getDeclaredField("second_category");
        Quote.class.getDeclaredField("accepted");
        Quote.class.getDeclaredField("rejected");
        assert(Quote.class.getDeclaredField("quote").getType().equals(int.class));
        assert(Quote.class.getDeclaredField("message").getType().equals(String.class));
        assert(Quote.class.getDeclaredField("sender").getType().equals(Party.class));
        assert(Quote.class.getDeclaredField("receiver").getType().equals(List.class));
        assert(Quote.class.getDeclaredField("first_category").getType().equals(boolean.class));
        assert(Quote.class.getDeclaredField("second_category").getType().equals(boolean.class));
        assert(Quote.class.getDeclaredField("accepted").getType().equals(boolean.class));
        assert(Quote.class.getDeclaredField("rejected").getType().equals(boolean.class));
    }
}