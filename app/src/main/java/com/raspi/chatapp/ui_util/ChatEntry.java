package com.raspi.chatapp.ui_util;

/**
 * Created by gamer on 12/4/2015.
 */
public class ChatEntry{

    public String buddyId;
    public String name;
    public String lastMessageStatus;
    public String lastMessageDate;
    public String lastMessageMessage;

    public ChatEntry(String buddyId, String name, String lastMessageStatus, String
            lastMessageDate, String lastMessageMessage){
        this.buddyId = buddyId;
        this.name = name;
        this.lastMessageStatus = lastMessageStatus;
        this.lastMessageDate = lastMessageDate;
        this.lastMessageMessage = lastMessageMessage;
    }
}