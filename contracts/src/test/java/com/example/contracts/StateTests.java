package com.example.contracts;

import com.example.states.Quote;
import org.junit.Test;

public class StateTests {

    //Mock State test check for if the state has correct parameters type
    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        Quote.class.getDeclaredField("quote");
        assert (Quote.class.getDeclaredField("quote").getType().equals(int.class));
    }
}