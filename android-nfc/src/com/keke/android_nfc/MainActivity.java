package com.keke.android_nfc;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private final static String LOG_TAG = MainActivity.class.getSimpleName();
	
	private NfcAdapter nfcAdapter;
	
	private PendingIntent pendingIntent;
	
	private IntentFilter[] intentFilters;
	
	private String[][] techLists;
	
	private EditText etSecretKey;
	
	private RadioGroup rgSecretKeyType;
	
	private EditText etSector;
	
	private TextView tvSectorData;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		
		if(this.nfcAdapter == null) {
			Toast.makeText(this, "Not support NFC.", Toast.LENGTH_LONG).show();
			return;
		}
		
		if(!this.nfcAdapter.isEnabled()) {
			Toast.makeText(this, "Please open NFC.", Toast.LENGTH_LONG).show();
			return;
		}
		
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0); 
		
	    IntentFilter filter1 = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);  
	    IntentFilter filter2 = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);  
	    
	    try {  
	    	
	    	filter1.addDataType("*/*");  
	    	
	    } catch (IntentFilter.MalformedMimeTypeException e) {  
	        e.printStackTrace();  
	    }  
	    
	    intentFilters = new IntentFilter[]{filter1, filter2};
	    
	    techLists = null;
	    
	    this.etSecretKey = (EditText) findViewById(R.id.et_secret_key);
	    
	    this.rgSecretKeyType = (RadioGroup) findViewById(R.id.rg_secret_key_type)
	    		;
	    this.etSector = (EditText) findViewById(R.id.et_sector);
	    
	    this.tvSectorData = (TextView) findViewById(R.id.tv_sector_data);
	}


	@SuppressLint("DefaultLocale")
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		this.tvSectorData.setText("");
		
		byte[] secretKey = MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY;
		
		try {
			
			secretKey = GetSecretKeyData(etSecretKey.getText().toString());
			
		} catch(Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "Invalid secret key.", Toast.LENGTH_SHORT).show();
			return;
		}
		
		int secretKeyType = rgSecretKeyType.getCheckedRadioButtonId();
		
		int sector = 0;
		
		try {
			
			sector = Integer.parseInt(etSector.getText().toString());
			
			if(sector < 0 || sector >= 16) {
				Toast.makeText(this, "Invalid sector number.", Toast.LENGTH_SHORT).show();
				return;
			}
			
		} catch(Exception e) {
			Toast.makeText(this, "Invalid sector Number.", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		
		MifareClassic mfc = null;
		
		try {
			
			mfc = MifareClassic.get(tag);
			
			mfc.connect();
			
			boolean result = true;
			
			if(secretKeyType == R.id.rb_keya) {
				result = mfc.authenticateSectorWithKeyA(sector, secretKey);
			} else {
				result = mfc.authenticateSectorWithKeyB(sector, secretKey);
			}
			
			if(result) {
				
				String sectorData = "";
				
				for(int i = 0; i < 4; i++) {
					
					byte[] blockData = mfc.readBlock(sector * 4 + i);
					
					String strBlockData = bytesToHexString(blockData);
					
					sectorData += strBlockData + "\n";
				}
				
				this.tvSectorData.setText(sectorData);

			} else {
				Toast.makeText(this, "Authenticate failed.", Toast.LENGTH_SHORT).show();
			}
			
		} catch(Exception ex) {
			Log.e(LOG_TAG, ex.getMessage());
		} finally {
			if(mfc != null) {
				try {
					mfc.close();
				} catch (IOException e) {
					Toast.makeText(this, "NFC close failed.", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
		}
	}

	private byte[] GetSecretKeyData(String secretKey) throws Exception {
		
		byte[] secretKeyData = new byte[6];
		
		String[] items = secretKey.split("-");
		
		if(items == null || items.length != 6) {
			throw new Exception("Invalid secret key.");
		}
		
		for(int i = 0; i < 6; i++) {
			
			String item = items[i];
			
			if(item.length() != 2) {
				throw new Exception("Invalid secret key.");
			}
			
			secretKeyData[i] = (byte)Integer.parseInt(item, 16);
		}
		
		return secretKeyData;
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		nfcAdapter.disableForegroundDispatch(this);
	}


	@Override
	protected void onResume() {
		super.onResume();
		
		nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists);
	}

	public static String bytesToHexString(byte[] src) {  
		
        StringBuilder stringBuilder = new StringBuilder("0x"); 
        
        if (src == null || src.length <= 0) {  
            return null;  
        }  
        
        char[] buffer = new char[2];  
        
        for (int i = 0; i < src.length; i++) {  
            buffer[0] = Character.toUpperCase(Character.forDigit((src[i] >>> 4) & 0x0F, 16));  
            buffer[1] = Character.toUpperCase(Character.forDigit(src[i] & 0x0F, 16));  
            stringBuilder.append(buffer);  
        }  
        
        return stringBuilder.toString();  
    }  
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
