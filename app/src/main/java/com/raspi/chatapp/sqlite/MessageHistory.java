package com.raspi.chatapp.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.raspi.chatapp.ui_util.ChatEntry;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MessageHistory{
    public static final String TYPE_TEXT = "com.raspi.sqlite.MessageHistory.TYPE_TEXT";
    public static final String TYPE_IMAGE = "com.raspi.sqlite.MessageHistory.TYPE_IMAGE";

    public static final String STATUS_WAITING = "com.raspi.sqlite.MessageHistory.STATUS_WAITING";
    public static final String STATUS_SENT = "com.raspi.sqlite.MessageHistory.STATUS_SENT";
    public static final String STATUS_RECEIVED = "com.raspi.sqlite.MessageHistory.STATUS_RECEIVED";
    public static final String STATUS_READ = "com.raspi.sqlite.MessageHistory.STATUS_READ";

    MessageHistoryDbHelper mDbHelper;

    public MessageHistory(Context context){
        mDbHelper = new MessageHistoryDbHelper(context);
    }

    public ChatEntry[] getChats(){
        Log.d("DATABASE", "Getting chats");
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor chats = db.query(MessageHistoryContract.ChatEntry.TABLE_NAME_ALL_CHATS, new
                        String[]{MessageHistoryContract.ChatEntry.COLUMN_NAME_BUDDY_ID,
                        MessageHistoryContract.ChatEntry
                                .COLUMN_NAME_NAME},
                null, null, null, null, null);
        int chatCount = chats.getColumnCount();
        ChatEntry[] resultChats = new ChatEntry[chatCount];
        int i = 0;
        chats.moveToFirst();
        do{
            String buddyId = chats.getString(0);
            String name = chats.getString(1);
            Log.d("DATABASE", "retrieving entry: " + buddyId + " - " + name);

            String[] columns = new String[]{
                    MessageHistoryContract.MessageEntry.COLUMN_NAME_BUDDY_ID,
                    MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TYPE,
                    MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_CONTENT,
                    MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_STATUS,
                    MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TIMESTAMP
            };
            Cursor lastMessage = db.query(buddyId, columns, null, null, null, null,
                    MessageHistoryContract.MessageEntry
                    .COLUMN_NAME_MESSAGE_TIMESTAMP + " DESC", "1");
            lastMessage.moveToFirst();

            String lastMessageStatus = "";
            String lastMessageDate = "";
            String lastMessageMessage = "";
            if (lastMessage.moveToFirst()){
                lastMessageStatus = lastMessage.getString(3);
                Date msgTime = new Date(lastMessage.getLong(4));
                Calendar startOfDay = Calendar.getInstance();
                startOfDay.set(Calendar.HOUR_OF_DAY, 0);
                startOfDay.set(Calendar.MINUTE, 0);
                startOfDay.set(Calendar.SECOND, 0);
                startOfDay.set(Calendar.MILLISECOND, 0);
                long diff = startOfDay.getTimeInMillis() - msgTime.getTime();
                if (diff <= 0)
                    lastMessageDate = new SimpleDateFormat("HH:mm", Locale.GERMANY).format(msgTime);
                else if (diff > 1000 * 60 * 60 * 24)
                    lastMessageDate = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format
                            (msgTime);
                else
                    lastMessageDate = new SimpleDateFormat("Yesterday", Locale.GERMANY).format(msgTime);
                lastMessageMessage = lastMessage.getString(2);
                //TODO do something with the types and who send the message...

            }
            resultChats[i] = new ChatEntry(buddyId, name, lastMessageStatus, lastMessageDate,
                    lastMessageMessage);
            i++;
        } while (chats.move(1));

        return resultChats;
    }

    public void addChat(String buddyId, String name){
        Log.d("DATABASE", "Adding a chat: " + buddyId + " - " + name);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        //remove everything after @ if it exists
        int index = buddyId.indexOf('@');
        if (index >= 0){
            buddyId = buddyId.substring(0, index);
        }
        index = name.indexOf('@');
        if (index >= 0){
            name = name.substring(0, index);
        }
        ContentValues values = new ContentValues();
        values.put(MessageHistoryContract.ChatEntry.COLUMN_NAME_BUDDY_ID, buddyId);
        values.put(MessageHistoryContract.ChatEntry.COLUMN_NAME_NAME, name);
        try{
            db.insertOrThrow(MessageHistoryContract.ChatEntry.TABLE_NAME_ALL_CHATS,
                    MessageHistoryContract.ChatEntry._ID, values);
        } catch (SQLException e){
            Log.d("DATABASE", "Couldn't insert --> is already inserted.");
            return;
        } catch (Exception e){
            Log.e("ERROR", "got an error while inserting a row into " + MessageHistoryContract
                    .ChatEntry.TABLE_NAME_ALL_CHATS);
            return;
        }
        mDbHelper.createMessageTable(buddyId);
    }

    public Cursor getMessages(String buddyId, int limit){
        return getMessages(buddyId, limit, 0);
    }

    public Cursor getMessages(String buddyId, int amount, int offset){
        Log.d("DATABASE", "Getting messages");
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String[] columns = new String[]{
                MessageHistoryContract.MessageEntry.COLUMN_NAME_BUDDY_ID,
                MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TYPE,
                MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_CONTENT,
                MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_STATUS,
                MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TIMESTAMP
        };
        return db.query(buddyId, columns, null, null, null, null, MessageHistoryContract.MessageEntry
                .COLUMN_NAME_MESSAGE_TIMESTAMP + " DESC", offset + "," + amount);
    }

    public void addMessage(String chatId, String buddyId, String type, String content, String
            status){
        Log.d("DATABASE", "Adding a message");
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        //remove everything after @ if it exists
        int index = buddyId.indexOf('@');
        if (index >= 0){
            buddyId = buddyId.substring(0, index);
        }
        index = chatId.indexOf('@');
        if (index >= 0){
            chatId = chatId.substring(0, index);
        }

        ContentValues values = new ContentValues();
        values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_BUDDY_ID, buddyId);
        values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TYPE, type);
        values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_CONTENT, content);
        values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_STATUS, status);
        values.put(MessageHistoryContract.MessageEntry.COLUMN_NAME_MESSAGE_TIMESTAMP, new Date().getTime());
        db.insert(chatId, MessageHistoryContract.MessageEntry._ID, values);
    }


}
