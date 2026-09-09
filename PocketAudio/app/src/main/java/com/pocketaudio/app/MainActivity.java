package com.pocketaudio.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    private final int ink=Color.rgb(31,29,54), muted=Color.rgb(111,107,132), purple=Color.rgb(87,70,217);
    private EditText link,name;private Spinner quality,mode;private Button convert,cancel,open,share;
    private TextView heading,detail;private ProgressBar progress;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){render();handler.postDelayed(this,400);}};
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(5),0,dp(5));return t;}
    private void gap(LinearLayout l,int size){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(size)));}
    private Button button(String label,boolean primary){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(primary?Color.WHITE:purple);b.setBackground(bg(primary?purple:0xFFECE9FC,14));b.setMinHeight(dp(52));return b;}
    private EditText input(String hint,int id){EditText e=new EditText(this);e.setId(id);e.setHint(hint);e.setTextColor(ink);e.setTextSize(16);e.setHintTextColor(muted);e.setSingleLine(true);e.setPadding(dp(15),dp(14),dp(15),dp(14));e.setBackground(bg(0xFFF2F1F8,12));return e;}
    public void onCreate(Bundle b){
        super.onCreate(b);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xFFF7F6FC);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(24),dp(18),dp(24),dp(24));scroll.addView(root);
        if(Build.VERSION.SDK_INT>=30) root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(24)+i.left,dp(18)+i.top,dp(24)+i.right,dp(24)+i.bottom);return insets;});
        // API 29 fallback: fitsSystemWindows handles insets below Android 11.
        if(Build.VERSION.SDK_INT<30){root.setOnApplyWindowInsetsListener(null);root.setFitsSystemWindows(true);}
        root.addView(text("POCKET MEDIA",13,purple,true));gap(root,20);
        root.addView(text("Save the video.\nKeep the audio.",28,ink,true));
        root.addView(text("Video & audio downloads · Up to 8K",16,muted,false));gap(root,22);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(20),dp(18),dp(20),dp(20));card.setBackground(bg(Color.WHITE,22));root.addView(card);
        card.addView(text("1   Paste your video link",17,ink,true));gap(card,8);
        link=input("Paste a video or Reel link",R.id.link_input);link.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);card.addView(link);
        Button paste=button("Paste link",false);LinearLayout.LayoutParams pasteLp=new LinearLayout.LayoutParams(-1,dp(48));pasteLp.topMargin=dp(8);card.addView(paste,pasteLp);
        paste.setOnClickListener(v->{ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cm.hasPrimaryClip()&&cm.getPrimaryClip()!=null){CharSequence t=cm.getPrimaryClip().getItemAt(0).coerceToText(this);link.setText(t);link.setSelection(link.length());}else Toast.makeText(this,"Copy a video link first",Toast.LENGTH_SHORT).show();});
        gap(card,14);card.addView(text("2   Choose your download",17,ink,true));
        mode=new Spinner(this);mode.setId(R.id.mode_input);mode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Audio · MP3","Video · MP4"}));card.addView(mode,new LinearLayout.LayoutParams(-1,dp(50)));
        card.addView(text("File name",13,muted,false));name=input("My media",R.id.name_input);name.setText("My media");card.addView(name);
        gap(card,10);card.addView(text("Quality",13,muted,false));quality=new Spinner(this);quality.setId(R.id.quality_input);
        quality.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"128 kbps · Smaller file","192 kbps · Recommended","320 kbps · Higher bitrate"}));quality.setSelection(1);card.addView(quality,new LinearLayout.LayoutParams(-1,dp(50)));
        mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> a){}
            public void onItemSelected(AdapterView<?> a,View v,int position,long id){setQualities(position==1);}
        });
        card.addView(text("Video quality uses the best available source up to your selection. 60 fps is preferred when available. No artificial upscaling. High-resolution playback depends on your phone.",12,muted,false));
        gap(card,18);convert=button("Download MP3",true);card.addView(convert,new LinearLayout.LayoutParams(-1,dp(56)));convert.setOnClickListener(v->start());
        gap(root,18);heading=text("Ready when you are",18,ink,true);root.addView(heading);detail=text("",14,muted,false);root.addView(detail);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(purple));root.addView(progress,new LinearLayout.LayoutParams(-1,dp(8)));
        gap(root,12);cancel=button("Cancel download",false);root.addView(cancel);cancel.setOnClickListener(v->startService(new Intent(this,ConvertService.class).setAction("cancel")));
        LinearLayout actions=new LinearLayout(this);open=button("Open file",true);share=button("Share",false);LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(52),1);a.setMarginEnd(dp(8));actions.addView(open,a);actions.addView(share,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(actions);
        open.setOnClickListener(v->useResult(false));share.setOnClickListener(v->useResult(true));
        gap(root,24);root.addView(text("No account. Saved on your phone.",14,ink,true));
        root.addView(text("Paste a public YouTube video/Short, Instagram Reel/post, or MP4 link. Private, login-required and restricted videos may not download. Platforms can temporarily block requests. Videos appear in Gallery / Movies; audio is in Music / PocketMedia. Available storage limits large downloads.",12,muted,false));
        TextView licenses=text("About & open-source licenses",12,purple,false);root.addView(licenses);licenses.setOnClickListener(v->{
            String license="Pocket Media 3.0 · GPL-3.0\nVideo support: yt-dlp / youtubedl-android 0.18.1\nAudio conversion: FFmpeg\nSource: https://github.com/ahtishamite/webstorage/tree/pocket-audio-app/PocketAudio\n\nLAME 3.100 MP3 encoder · LGPL 2.0 or later\nhttps://lame.sourceforge.io/\n\n";
            try(java.io.InputStream in=getAssets().open("LAME-LICENSE.txt")){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)bytes.write(buf,0,n);license+=bytes.toString("UTF-8");}catch(Exception ignored){license+="License text unavailable in this build.";}
            TextView t=text(license,13,ink,false);t.setPadding(dp(20),dp(15),dp(20),dp(15));ScrollView sv=new ScrollView(this);sv.addView(t);new AlertDialog.Builder(this).setTitle("Pocket Media 3.0").setView(sv).setPositiveButton("Close",null).show();
        });
        Button history=button("Recent downloads",false);root.addView(history);history.setOnClickListener(v->showHistory());
        setContentView(scroll);
        if(b==null)receiveSharedLink(getIntent());
        if(b!=null){mode.setSelection(b.getInt("mode",0));link.setText(b.getString("link",""));name.setText(b.getString("name","My audio"));}
        if(!ConvertService.busy && ConvertService.result==null){String last=getSharedPreferences("audio",MODE_PRIVATE).getString("last",null);if(last!=null){ConvertService.result=Uri.parse(last);ConvertService.resultMime=getSharedPreferences("audio",0).getString("mime","audio/mpeg");ConvertService.status="Your last download";ConvertService.detail=getSharedPreferences("audio",MODE_PRIVATE).getString("filename","")+"";}}
    }
    private void setQualities(boolean video){
        String[] q=video?new String[]{"Best available · Up to 8K","720p HD","1080p Full HD","1080p · 60 fps preferred","4K · Up to 2160p","6K · Up to 3240p","8K · Up to 4320p"}:new String[]{"128 kbps · Smaller file","192 kbps · Recommended","320 kbps · Higher bitrate"};
        quality.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,q));quality.setSelection(video?2:1);
    }
    protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);receiveSharedLink(intent);}
    private void receiveSharedLink(Intent i){if(Intent.ACTION_SEND.equals(i.getAction())&&i.hasExtra(Intent.EXTRA_TEXT))link.setText(i.getStringExtra(Intent.EXTRA_TEXT));}
    private void showHistory(){
        try{org.json.JSONArray a=new org.json.JSONArray(getSharedPreferences("audio",0).getString("history","[]"));if(a.length()==0){Toast.makeText(this,"Your saved downloads will appear here.",Toast.LENGTH_SHORT).show();return;}
        String[] names=new String[a.length()];for(int j=0;j<a.length();j++)names[j]=a.getJSONObject(j).getString("name");
        new AlertDialog.Builder(this).setTitle("Recent downloads").setItems(names,(d,index)->{try{org.json.JSONObject item=a.getJSONObject(index);ConvertService.result=Uri.parse(item.getString("uri"));ConvertService.resultMime=item.getString("mime");new AlertDialog.Builder(this).setTitle(item.getString("name")).setItems(new String[]{"Open","Share"},(dialog,action)->useResult(action==1)).show();}catch(Exception ignored){}}).setNegativeButton("Close",null).show();
        }catch(Exception e){Toast.makeText(this,"History unavailable",Toast.LENGTH_SHORT).show();}
    }
    private void start(){
        if(ConvertService.busy)return;
        try{link.setText(ConvertService.normalize(link.getText().toString()));}catch(Exception e){link.setError("Paste a full public video URL, including https://");link.requestFocus();return;}
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},5);
        Intent i=new Intent(this,ConvertService.class).putExtra("url",link.getText().toString().trim()).putExtra("name",name.getText().toString()).putExtra("video",mode.getSelectedItemPosition()==1).putExtra("height",mode.getSelectedItemPosition()==1?new int[]{4320,720,1080,1080,2160,3240,4320}[quality.getSelectedItemPosition()]:1080).putExtra("bitrate",mode.getSelectedItemPosition()==0?new int[]{128,192,320}[quality.getSelectedItemPosition()]:192);
        try{startForegroundService(i);convert.setEnabled(false);}catch(Exception e){Toast.makeText(this,"Could not start. Keep the app open and retry.",Toast.LENGTH_LONG).show();}
    }
    private void render(){boolean b=ConvertService.busy;convert.setEnabled(!b);link.setEnabled(!b);name.setEnabled(!b);quality.setEnabled(!b);mode.setEnabled(!b);convert.setText(b?"Downloading…":(mode.getSelectedItemPosition()==1?"Download MP4":"Download MP3"));heading.setText(ConvertService.status);detail.setText(ConvertService.detail);progress.setVisibility(b?View.VISIBLE:View.GONE);progress.setIndeterminate(ConvertService.progress<0);if(ConvertService.progress>=0)progress.setProgress(ConvertService.progress);cancel.setVisibility(b?View.VISIBLE:View.GONE);boolean done=!b&&ConvertService.result!=null;open.setVisibility(done?View.VISIBLE:View.GONE);share.setVisibility(done?View.VISIBLE:View.GONE);}
    private void useResult(boolean sharing){Uri u=ConvertService.result;if(u==null)return;try{Intent i=new Intent(sharing?Intent.ACTION_SEND:Intent.ACTION_VIEW);if(sharing){i.setType(ConvertService.resultMime);i.putExtra(Intent.EXTRA_STREAM,u);i.setClipData(ClipData.newRawUri("MP3 audio",u));}else i.setDataAndType(u,ConvertService.resultMime);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,sharing?"Share media":"Open media"));}catch(Exception e){Toast.makeText(this,"Open Movies / PocketMedia or Music / PocketMedia in your Files app.",Toast.LENGTH_LONG).show();}}
    protected void onResume(){super.onResume();handler.post(refresh);}
    protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    protected void onSaveInstanceState(Bundle b){b.putInt("mode",mode.getSelectedItemPosition());b.putString("link",link.getText().toString());b.putString("name",name.getText().toString());b.putInt("quality",quality.getSelectedItemPosition());super.onSaveInstanceState(b);}
}
