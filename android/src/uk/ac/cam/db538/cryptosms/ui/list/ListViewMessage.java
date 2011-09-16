/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.db538.cryptosms.ui.list;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.ListView;

/*
 * List view showing message history
 */
public final class ListViewMessage extends ListView {
    
    /**
     * Instantiates a new list view message.
     *
     * @param context the context
     */
    public ListViewMessage(Context context) {
        super(context);
    }
    
    /**
     * Instantiates a new list view message.
     *
     * @param context the context
     * @param attrs the attrs
     */
    public ListViewMessage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    /* (non-Javadoc)
     * @see android.view.View#onKeyShortcut(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
//        switch (keyCode) {
//        case KeyEvent.KEYCODE_C:
//            MessageListItem view = (MessageListItem)getSelectedView();
//            if (view == null) {
//                break;
//            }
//            MessageItem item = view.getMessageItem();
//            if (item != null && item.isSms()) {
//                ClipboardManager clip =
//                    (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
//                clip.setText(item.mBody);
//                return true;
//            }
//            break;
//        }

        return super.onKeyShortcut(keyCode, event);
    }
    
}

