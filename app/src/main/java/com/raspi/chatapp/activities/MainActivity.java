package com.raspi.chatapp.activities;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.raspi.chatapp.R;
import com.raspi.chatapp.activities.fragments.ChatFragment;
import com.raspi.chatapp.activities.fragments.ChatListFragment;
import com.raspi.chatapp.service.MessageService;
import com.raspi.chatapp.sqlite.AndroidDatabaseManager;
import com.raspi.chatapp.util.MyNotification;

public class MainActivity extends AppCompatActivity implements
        FragmentManager.OnBackStackChangedListener, ChatListFragment
        .OnFragmentInteractionListener, ChatFragment
        .OnFragmentInteractionListener{

  public static final String PREFERENCES = "com.raspi.chatapp.activities.MainActivity.PREFERENCES";
  public static final String USERNAME = "com.raspi.chatapp.activities.MainActivity.USERNAME";
  public static final String PASSWORD = "com.raspi.chatapp.activities.MainActivity.PASSWORD";
  public static final String RECONNECT = "com.raspi.chatapp.activities.MainActivity.RECONNECT";
  public static final String APP_LAUNCHED = "con.raspi.chatapp.MainActivity.APP_CREATED";
  public static final String APP_CLOSED = "con.raspi.chatapp.MainActivity.APP_DESTROYED";
  public static final String BUDDY_ID = "com.raspi.chatapp.activities.MainActivity.BUDDY_ID";
  public static final String CHAT_NAME = "com.raspi.chatapp.activities.MainActivity.CHAT_NAME";
  public static final String MESSAGE_BODY = "com.raspi.chatapp.activities.MainActivity.MESSAGE_BODY";
  public static final String PRESENCE_CHANGED = "com.raspi.chatapp.activities.MainActivity.PRESENCE_CHANGED";
  public static final String PRESENCE_STATUS = "com.raspi.chatapp.activities.MainActivity.PRESENCE_STATUS";
  public static final String RECEIVE_MESSAGE = "com.raspi.chatapp.activities.MainActivity.RECEIVE_MESSAGE";
  public static final String CONN_ESTABLISHED = "com.raspi.chatapp.activities.MainActivity.CONN_ESTABLISHED";
  public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";


  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    getSupportFragmentManager().addOnBackStackChangedListener(this);
    shouldDisplayHomeUp();

    setUserPwd();

    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel
            (MyNotification.NOTIFICATION_ID);
    new MyNotification(this).reset();

    Intent callingIntent = getIntent();
    if (callingIntent != null && MyNotification.NOTIFICATION_CLICK.equals
            (callingIntent.getAction())){
      Log.d("DEBUG", "received intend not click");
      Bundle extras = callingIntent.getExtras();
      if (extras != null && extras.containsKey(BUDDY_ID) && extras.containsKey(CHAT_NAME)){
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(extras);
        getSupportFragmentManager().beginTransaction().add(R.id
                .fragment_container, fragment).commit();
        return;
      }
    }
    getSupportFragmentManager().beginTransaction().add(R.id
            .fragment_container, new ChatListFragment()).commit();
  }

  @Override
  protected void onResume(){
    super.onResume();
    this.startService(new Intent(this, MessageService.class).setAction(APP_LAUNCHED));
    new MyNotification(this).reset();
    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
            .cancel(MyNotification.NOTIFICATION_ID);
  }

  @Override
  protected void onPause(){
    this.startService(new Intent(this, MessageService.class).setAction(APP_CLOSED));
    super.onPause();
  }

  @Override
  protected void onDestroy(){
    //signal the service that the app is about to get destroyed
    this.startService(new Intent(this, MessageService.class).setAction(APP_CLOSED));
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu){
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_chat_list, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item){
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings){
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void onSettingsClick(MenuItem menuItem){
    Intent intent = new Intent(this, SettingsActivity.class);
    startActivity(intent);
  }

  public void onAddChatClick(MenuItem menuItem){
    Intent intent = new Intent(this, AddChatActivity.class);
    startActivity(intent);
  }

  public void onDatabaseDebug(MenuItem menuItem){
    Intent intent = new Intent(this, AndroidDatabaseManager.class);
    startActivity(intent);
  }

  private void setUserPwd(){
    SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);
    //if (!preferences.contains(USERNAME))
    preferences.edit().putString(USERNAME, "aylin").apply();

    //if (!preferences.contains(PASSWORD))
    preferences.edit().putString(PASSWORD, "passwdAylin").apply();
  }

  @Override
  public void onChatOpened(String buddyId, String name){
    ChatFragment fragment = new ChatFragment();
    Bundle extras = new Bundle();
    extras.putString(MainActivity.BUDDY_ID, buddyId);
    extras.putString(MainActivity.CHAT_NAME, name);
    fragment.setArguments(extras);
    getSupportFragmentManager().beginTransaction().replace(R.id
            .fragment_container, fragment).addToBackStack(null).commit();
  }

  @Override
  public void onAttachClicked(String buddyId){

  }

  @Override
  public void onBackStackChanged(){
    shouldDisplayHomeUp();
  }

  public void shouldDisplayHomeUp(){
    boolean canBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
    getSupportActionBar().setDisplayHomeAsUpEnabled(canBack);
    getSupportActionBar().setHomeButtonEnabled(canBack);
  }

  @Override
  public boolean onSupportNavigateUp(){
    getSupportFragmentManager().popBackStack();
    return true;
  }

  //receiving boot intents
  public static class BootReceiver extends BroadcastReceiver{

    public BootReceiver(){
    }

    @Override
    public void onReceive(Context context, Intent intent){
      if (intent != null && intent.getAction() != null){
        if (intent.getAction().equals(BOOT_COMPLETED)){
          context.startService(new Intent(context, MessageService.class));
        }
      }
    }
  }
}
