package com.release;

import android.app.ProgressDialog;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.applozic.mobicomkit.Applozic;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.UserLogoutTask;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.MobiComConversationService;
import com.applozic.mobicomkit.exception.ApplozicException;
import com.applozic.mobicomkit.listners.AlLogoutHandler;
import com.applozic.mobicomkit.listners.MessageListHandler;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;
import com.release.fragments.InitiateDialogFragment;
import com.release.fragments.MainContainerFragment;
import com.release.fragments.MessageListFragment;

import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity  implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        MessageListFragment.OnFragmentInteractionListener,MainContainerFragment.OnFragmentInteractionListener{

    private UserLogoutTask userLogoutTask;

    private NavigationDrawerFragment mNavigationDrawerFragment;

    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mTitle = getTitle();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        Log.d("ActionBarTitle", mTitle.toString());
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

    }

    public void setActionBarTitle(String title){
        getSupportActionBar().setTitle(title);
    }

    public void initiateChatClick(View v) {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        DialogFragment fragment = new InitiateDialogFragment();
        FragmentTransaction fragmentTransaction = supportFragmentManager
                .beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("InitiateDialogFragment");
        if (prev != null) {
            fragmentTransaction.remove(prev);
        }
        fragmentTransaction.addToBackStack(null);
        fragment.show(fragmentTransaction, "InitiateDialogFragment");
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments

        if (position == 1) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            MessageListFragment messageListFragment = new MessageListFragment();
            fragmentTransaction.replace(R.id.container,messageListFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
            return;
        }
        if (position == 0) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            MainContainerFragment mainContainerFragment = new MainContainerFragment();
            fragmentTransaction.replace(R.id.container,mainContainerFragment);
            fragmentTransaction.commit();
            return;
        }
        if (position == 2) {
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage("Logging Out! Please Wait...");
            dialog.show();
            Applozic.logoutUser(this, new AlLogoutHandler(){
                @Override
                public void onSuccess(Context context) {
                    if(dialog.isShowing()){
                        dialog.dismiss();
                    }
                    Toast.makeText(getBaseContext(),getBaseContext().getString(R.string.log_out_successful), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(context, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(Exception exception) {
                    if(dialog.isShowing()){
                        dialog.dismiss();
                    }
                    Toast.makeText(getBaseContext(),getBaseContext().getString(R.string.log_out_successful), Toast.LENGTH_SHORT).show();
//                    logoutProgress.setVisibility(View.INVISIBLE);
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle("Alert");
                    alertDialog.setMessage(exception.toString());
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Alert",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    if (!isFinishing()) {
                        alertDialog.show();
                    }
                }
            });
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
//            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        if(id == R.id.action_chat){
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            MessageListFragment messageListFragment = new MessageListFragment();
            fragmentTransaction.replace(R.id.container,messageListFragment);
            fragmentTransaction.commit();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}

