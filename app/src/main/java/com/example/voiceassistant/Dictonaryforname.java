package com.example.voiceassistant;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

public class Dictonaryforname {
    
    Dictionary phonenumber = new Hashtable<String,String>();

    public Dictonaryforname() {
        phonenumber.put("papa","+919870177619");
    }
    
    public void AddNumber(String name, String number){
        name = name.toLowerCase(Locale.ROOT);
        String base = "+91";
        number = base+number;
        phonenumber.put(name,number);
    }

    public String returnNumber(String name){
        if(phonenumber.get(name)!= null)
            return (String) phonenumber.get(name);
        else
            return "0";
    }
}
