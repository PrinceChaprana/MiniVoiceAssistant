package com.example.voiceassistant;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class chatArrayAdapter  extends ArrayAdapter<ChatMessage> {
    Context context;
    TextView textView;
    private List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();

    public chatArrayAdapter(Context context, int resource) {
        super(context, resource);
        this.context = context;
    }

    @Override
    public int getCount() {
        return this.chatMessageList.size();
    }

    @Override
    public View getView(int position,  View convertView,  ViewGroup parent) {
        ChatMessage chatMessage = getItem(position);
        View row = convertView;
        LayoutInflater inflater =(LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(chatMessage.right){
            row = inflater.inflate(R.layout.right_msg,parent,false);
        }else{
            row = inflater.inflate(R.layout.left_msg,parent,false);
        }
        textView = (TextView) row.findViewById(R.id.msgr);
        textView.setText(chatMessage.message);
        return row;
        }

    @Override
    public void add(ChatMessage object) {
        chatMessageList.add(object);
        super.add(object);
        this.notifyDataSetChanged();
    }
}
