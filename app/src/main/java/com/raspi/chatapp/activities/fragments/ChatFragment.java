package com.raspi.chatapp.activities.fragments;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.raspi.chatapp.R;
import com.raspi.chatapp.activities.MainActivity;
import com.raspi.chatapp.sqlite.MessageHistory;
import com.raspi.chatapp.ui_util.message_array.Date;
import com.raspi.chatapp.ui_util.message_array.ImageMessage;
import com.raspi.chatapp.ui_util.message_array.MessageArrayAdapter;
import com.raspi.chatapp.ui_util.message_array.MessageArrayContent;
import com.raspi.chatapp.ui_util.message_array.TextMessage;
import com.raspi.chatapp.util.Globals;
import com.raspi.chatapp.util.MyNotification;
import com.raspi.chatapp.util.UploadTask;
import com.raspi.chatapp.util.XmppManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChatFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment{
  private static final int MESSAGE_LIMIT = 30;

  private String buddyId;
  private String chatName;

  private MessageArrayAdapter maa;
  private MessageHistory messageHistory;

  private ListView listView;
  private EditText textIn;
  private ActionBar actionBar;

  private OnFragmentInteractionListener mListener;
  private Handler mHandler;

  private BroadcastReceiver MessageReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      reloadMessages();
      new MyNotification(getContext()).reset();
      ((NotificationManager) getContext().getSystemService(Context
              .NOTIFICATION_SERVICE))
              .cancel(MyNotification.NOTIFICATION_ID);
    }
  };
  private BroadcastReceiver PresenceChangeReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent){
      Bundle extras = intent.getExtras();
      if (extras != null && extras.containsKey(MainActivity.BUDDY_ID) && extras.containsKey(MainActivity.PRESENCE_STATUS)){
        if (buddyId.equals(extras.getString(MainActivity.BUDDY_ID))){
          updateStatus(extras.getString(MainActivity.PRESENCE_STATUS));
        }
      }
    }
  };

  public ChatFragment(){
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param buddyId  TRhe buddyId of the chat partner
   * @param chatName The name of the chat
   * @return A new instance of fragment ChatFragment.
   */
  public static ChatFragment newInstance(String buddyId, String chatName){
    ChatFragment fragment = new ChatFragment();
    Bundle args = new Bundle();
    args.putString(MainActivity.BUDDY_ID, buddyId);
    args.putString(MainActivity.CHAT_NAME, chatName);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState){
    super.onActivityCreated(savedInstanceState);
    actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
  }

  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    if (getArguments() != null){
      buddyId = getArguments().getString(MainActivity.BUDDY_ID);
      chatName = getArguments().getString(MainActivity.CHAT_NAME);
    }else
      return;
    messageHistory = new MessageHistory(getContext());

    //See XmppManager for further explanation
    mHandler = new Handler(Looper.getMainLooper()){
      @Override
      public void handleMessage(Message msg){
        XmppManager.UploadHandlerTask progress = (XmppManager.UploadHandlerTask) msg.obj;
        if (progress != null && chatName.equals(progress.chatId)){
          int i;
          for (i = maa.getCount(); i>=0;i--){
            MessageArrayContent mac = maa.getItem(i);
            if ((mac instanceof ImageMessage) && ((ImageMessage) mac)._ID ==
                    progress.messageID){
              ImageMessage im = (ImageMessage) mac;
              im.progress = progress.progress;
              switch (msg.what){
                case (XmppManager.STATUS_ERROR):
                  im.status = MessageHistory.STATUS_WAITING;
                  break;
                case (XmppManager.STATUS_SENDING):
                  im.status = MessageHistory.STATUS_SENDING;
                  break;
                case (XmppManager.STATUS_SENT):
                  im.status = MessageHistory.STATUS_SENT;
                  im.progress = 0;
                  break;
              }
              maa.notifyDataSetInvalidated();
            }
          }
        }
      }
    };
  }

  @Override
  public void onResume(){
    super.onResume();
    LocalBroadcastManager.getInstance(getContext()).registerReceiver
            (MessageReceiver, new IntentFilter(MainActivity.RECEIVE_MESSAGE));
    LocalBroadcastManager.getInstance(getContext()).registerReceiver
            (PresenceChangeReceiver, new IntentFilter(MainActivity.PRESENCE_CHANGED));
    initUI();
  }

  @Override
  public void onPause(){
    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver
            (MessageReceiver);
    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver
            (PresenceChangeReceiver);
    super.onPause();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState){
    // Inflate the layout for this fragment
    setHasOptionsMenu(true);
    return inflater.inflate(R.layout.fragment_chat, container, false);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater){
    menuInflater.inflate(R.menu.menu_chat, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item){
    switch (item.getItemId()){
      case R.id.action_settings:
        return false;
      case R.id.action_attach:
        mListener.onAttachClicked();
        return true;
      case R.id.home:
      default:
        break;
    }
    return false;
  }

  @Override
  public void onAttach(Context context){
    super.onAttach(context);
    if (context instanceof OnFragmentInteractionListener){
      mListener = (OnFragmentInteractionListener) context;
    }else{
      throw new RuntimeException(context.toString()
              + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach(){
    super.onDetach();
    mListener = null;
  }

  private void initUI(){
    if (actionBar != null)
      actionBar.setTitle(chatName);
    maa = new MessageArrayAdapter(getContext(), R.layout.message_text);

    listView = (ListView) getView().findViewById(R.id.chat_listview);
    textIn = (EditText) getView().findViewById(R.id.chat_in);
    Button sendBtn = (Button) getView().findViewById(R.id.chat_sendBtn);

    sendBtn.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v){
        sendMessage(textIn.getText().toString());
      }
    });

    listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
    listView.setAdapter(maa);

    maa.registerDataSetObserver(new DataSetObserver(){
      @Override
      public void onChanged(){
        super.onChanged();
        listView.setSelection(maa.getCount() - 1);
      }
    });
    reloadMessages();
    String lastOnline = messageHistory.getOnline(buddyId);
    updateStatus(lastOnline);
  }

  private void sendMessage(String message){
    XmppManager xmppManager = ((Globals) getActivity().getApplication())
            .getXmppManager();

    String status = MessageHistory.STATUS_WAITING;
    if (xmppManager != null && xmppManager.isConnected() && xmppManager.sendMessage(message, buddyId))
      status = MessageHistory.STATUS_SENT;
    else{
      Log.e("ERROR", "There was an error with the connection while sending a message.");
      //TODO messageHistory.addSendRequest(buddyId, message);
    }
    messageHistory.addMessage(buddyId, getContext().getSharedPreferences
            (MainActivity.PREFERENCES, 0).getString(MainActivity
            .USERNAME, ""), MessageHistory.TYPE_TEXT, message, status);
    textIn.setText("");
    maa.clear();
    reloadMessages();
  }

  private void reloadMessages(){
    maa.clear();
    MessageArrayContent[] messages = messageHistory.getMessages(buddyId, MESSAGE_LIMIT);
    long oldDate = 0;
    final int c = 24 * 60 * 60 * 1000;
    for (MessageArrayContent message : messages){
      if (message instanceof TextMessage){
        TextMessage msg = (TextMessage) message;
        if ((msg.time - oldDate) / c > 0)
          maa.add(new Date(msg.time));
        oldDate = msg.time;
        maa.add(msg);
        if (msg.left)
          messageHistory.updateMessageStatus(buddyId, msg._ID, MessageHistory
                  .STATUS_READ);
      }else if (message instanceof ImageMessage){
        ImageMessage msg = (ImageMessage) message;
        if ((msg.time - oldDate) / c > 0)
          maa.add(new Date(msg.time));
        oldDate = msg.time;
        maa.add(msg);
        if (msg.left)
          messageHistory.updateMessageStatus(buddyId, msg._ID, MessageHistory
                  .STATUS_READ);
        else if (MessageHistory.STATUS_WAITING.equals(msg.status)){
          XmppManager xmppManager = ((Globals) getActivity().getApplication())
                  .getXmppManager();
          if (xmppManager != null){
            UploadTask task = new UploadTask(msg.file, msg.description,
                    buddyId, msg._ID, xmppManager.getConnection(), messageHistory);
            xmppManager.sendImage(task, mHandler);
          }
        }
      }
    }
  }

  private void updateStatus(String lastOnline){
    try{
      long time = Long.valueOf(lastOnline);
      Calendar startOfDay = Calendar.getInstance();
      startOfDay.set(Calendar.HOUR_OF_DAY, 0);
      startOfDay.set(Calendar.MINUTE, 0);
      startOfDay.set(Calendar.SECOND, 0);
      startOfDay.set(Calendar.MILLISECOND, 0);
      long diff = startOfDay.getTimeInMillis() - time;
      if (diff <= 0)
        lastOnline = getResources().getString(R.string.last_online_today) + " ";
      else if (diff > 1000 * 60 * 60 * 24)
        lastOnline = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format
                (time) + " " + getResources().getString(R.string
                .last_online_at) + " ";
      else
        lastOnline = getResources().getString(R.string.last_online_yesterday)
                + " ";
      lastOnline += new SimpleDateFormat("HH:mm", Locale.GERMANY)
              .format(time);
      if (actionBar != null)
        actionBar.setSubtitle(lastOnline);
    }catch (NumberFormatException e){
      if (actionBar != null)
        actionBar.setSubtitle(Html
                .fromHtml("<font " +
                        "color='#55AAFF'>" + lastOnline + "</font>"));
    }
  }


  /*
   * functions for the uploadImage.
   * If an image is uploaded, the task response to the GUI via the interface
   * declaring there functions.
   * /

  @Override
  public void onProgressUpdate(String chatId, long messageID, double progress){
    if (chatId.equals(buddyId)){
      int c = maa.getCount();
      int i;
      for (i = 0; i < c; i++){
        try{
          ImageMessage msg = (ImageMessage) maa.getItem(i);
          if (msg._ID == messageID){
            msg.progress = progress;
          }
        }catch (ClassCastException e){
        }
      }
    }
  }

  @Override
  public void onPostUpload(String chatId, long messageID, boolean success){
    if (chatId.equals(buddyId)){
      int c = maa.getCount();
      int i;
      for (i = 0; i < c; i++){
        try{
          ImageMessage msg = (ImageMessage) maa.getItem(i);
          if (msg._ID == messageID){
            msg.progress = 1;
            msg.status = MessageHistory.STATUS_SENT;
          }
        }catch (ClassCastException e){
        }
      }
    }
  }
*/
  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p/>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnFragmentInteractionListener{
    void onAttachClicked();
  }
}
