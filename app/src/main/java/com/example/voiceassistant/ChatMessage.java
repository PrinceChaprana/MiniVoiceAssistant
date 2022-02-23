package com.example.voiceassistant;

public class ChatMessage {
    public String message;
    public boolean right;
    //true means right false means left

    public ChatMessage(boolean right,String message){
        super();
        this.message = message;
        this.right = right;
    }
}
