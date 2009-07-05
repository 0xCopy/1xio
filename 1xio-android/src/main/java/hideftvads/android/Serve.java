package hideftvads.android;

import android.app.*;
import android.os.*;
import hideftvads.server.*;

import java.io.*;

public class Serve extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Runnable runnable = new Runnable() {
            public void run() {
                Agent x;
                try {
                    x = new Agent(

                    );
                } catch (IOException e) {
                    e.printStackTrace();  //TODO: Verify for a purpose
                }
            }
        };
        
        
        new Thread(runnable).start();
        setContentView(R.layout.main);
    

    }
}
