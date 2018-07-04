package com.release.adapters;

import android.util.Log;

import com.applozic.mobicomkit.api.conversation.Message;

import java.util.List;

/**
 * Created by shivam on 9/12/17.
 */

/**
 * Stores information about the contact or channel which is selected.
 * This information is then used by conversation adapter to display detailed conversation for that contact or channel.
 */
public class ClickedItem {
    static List<Message> message;

    public static List<Message> getMessage() {
        return message;
    }

    public static void setMessage(List<Message> message) {
        ClickedItem.message = null;
        ClickedItem.message = message;
    }
}
