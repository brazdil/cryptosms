package uk.ac.cam.db538.cryptosms.ui;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.data.KeysMessage;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.data.Message.MessageException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageSendingListener;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.ui.DialogManager.DialogBuilder;
import uk.ac.cam.db538.cryptosms.utils.SimNumber;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

public class UtilsKeyExchange {
	private static final String DIALOG_SENDING = "DIALOG_EXCHANGE_SENDING";
	private static final String DIALOG_SENDING_ERROR = "DIALOG_EXCHANGE_SENDING_ERROR";
	private static final String PARAM_SENDING_ERROR = "PARAM_EXCHANGE_SENDING_ERROR";
	
	public static void prepareDialogs(DialogManager dialogManager, final Context context) {
        dialogManager.addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				Resources res = context.getResources();
				ProgressDialog pd = new ProgressDialog(context);
				pd.setCancelable(false);
				pd.setMessage(res.getString(R.string.sending));
				pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				try {
					pd.setMax(KeysMessage.getPartsCount());
				} catch (MessageException e) {
					// should not happen, ever...
				}
				return pd;
			}
			
			@Override
			public String getId() {
				return DIALOG_SENDING;
			}
		});
        
        dialogManager.addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				String error = params.getString(PARAM_SENDING_ERROR);
				Resources res = context.getResources();
				
				return new AlertDialog.Builder(context)
				       .setTitle(res.getString(R.string.error_sms_service))
				       .setMessage(res.getString(R.string.error_sms_service_details) + "\nError: " + error)
				       .setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
				       .create();
			}
			
			@Override
			public String getId() {
				return DIALOG_SENDING_ERROR;
			}
		});
	}
	
	public static void sendKeys(final String phoneNumber, final long contactId, final String contactKey, final Context context, final DialogManager dialogManager, final MessageSendingListener listener) {
    	dialogManager.showDialog(DIALOG_SENDING, null);
		
		// generate session keys
		final KeysMessage keysMessage = new KeysMessage(contactId, contactKey);

		// send the message
		try {
			keysMessage.sendSMS(phoneNumber, context, new MessageSendingListener() {
				@Override
				public void onMessageSent() {
					Log.d(MyApplication.APP_TAG, "Sent all!");
					dialogManager.dismissDialog(DIALOG_SENDING);
					
					try {
						Conversation conv = Conversation.getConversation(phoneNumber);
						if (conv == null) {
							conv = Conversation.createConversation();
							conv.setPhoneNumber(phoneNumber);
							// no need to save, because it will get saved while
							// creating the session keys
						}
						SimNumber simNumber = SimCard.getSingleton().getNumber();
						conv.deleteSessionKeys(simNumber);
						SessionKeys keys = SessionKeys.createSessionKeys(conv);
						keys.setSessionKey_Out(keysMessage.getKeyOut());
						keys.setSessionKey_In(keysMessage.getKeyIn());
						keys.setSimNumber(simNumber);
						keys.setKeysSent(true);
						keys.setKeysConfirmed(false);
						keys.saveToFile();
					} catch (StorageFileException e) {
						State.fatalException(e);
						return;
					}
					
					listener.onMessageSent();
				}
				
				@Override
				public void onError(String message) {
					dialogManager.dismissDialog(DIALOG_SENDING);
					
					Bundle params = new Bundle();
					params.putString(PARAM_SENDING_ERROR, message);
					dialogManager.showDialog(DIALOG_SENDING_ERROR, params);
					
					listener.onError(message);
				}

				@Override
				public boolean onPartSent(int index) {
					ProgressDialog pd = (ProgressDialog) dialogManager.getDialog(DIALOG_SENDING);
					if (pd != null) 
						pd.setProgress(index + 1);
					return listener.onPartSent(index);
				}
			});
		} catch (StorageFileException e) {
			State.fatalException(e);
			return;
		} catch (MessageException e) {
			State.fatalException(e);
			return;
		} catch (EncryptionException e) {
			State.fatalException(e);
			return;
		}
    }
}
