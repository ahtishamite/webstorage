package com.pocketaudio.app;
import android.app.*;
import android.os.Bundle;
import android.media.*;
import android.graphics.*;
import android.net.Uri;
import org.json.*;
public class PlaybackChecks extends Instrumentation {
 public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
 public void onStart(){Bundle result=new Bundle();int count=0;
  try{JSONArray history=new JSONArray(getTargetContext().getSharedPreferences("audio",0).getString("history","[]"));
   for(int j=0;j<history.length();j++){JSONObject item=history.getJSONObject(j);if(!item.getString("mime").startsWith("video"))continue;
    MediaMetadataRetriever r=new MediaMetadataRetriever();try{r.setDataSource(getTargetContext(),Uri.parse(item.getString("uri")));Bitmap frame=r.getFrameAtTime(100000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
     if(frame==null)throw new IllegalStateException("Android could not decode "+item.getString("name"));
     int color=frame.getPixel(frame.getWidth()/2,frame.getHeight()/2);if(Color.blue(color)<30)throw new IllegalStateException("Decoded frame is black for "+item.getString("name"));frame.recycle();count++;
    }finally{r.release();}
   }
   if(count<3)throw new IllegalStateException("Expected 3 video fixtures, got "+count);
   result.putString("stream","ANDROID_VISIBLE_FRAMES_PASS count="+count);finish(Activity.RESULT_OK,result);
  }catch(Exception e){result.putString("stream","PLAYBACK_FAILURE "+e.toString());finish(Activity.RESULT_CANCELED,result);}
 }
}
