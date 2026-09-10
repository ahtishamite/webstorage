package com.pocketaudio.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;

public class MainActivity extends Activity {
    private final int ink=0xFF172F44,muted=0xFF536C80,accent=0xFF006E83;
    private EditText link,name;
    private Spinner mode,quality;
    private Button convert,cancel,update;
    private TextView heading,detail,updateDetail;
    private ProgressBar progress;
    private LinearLayout historyList;
    private FrameLayout pages;
    private View[] screens;
    private Button[] tabs;
    private int selectedTab,lastMode=-1,pendingQuality=-1;
    private boolean pendingStart;
    private long pressedAt;
    private String historyVersion="";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){render();handler.postDelayed(this,300);}};
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private Drawable glass(int radius,boolean bright){
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{bright?0xEDFFFFFF:0xC2FFFFFF,bright?0xB8F4FFFF:0x75FFFFFF});
        g.setCornerRadius(dp(radius));g.setStroke(dp(1),0xE6FFFFFF);return g;
    }
    private Drawable ripple(Drawable d,int radius){return new RippleDrawable(ColorStateList.valueOf(0x22006E83),d,shape(Color.WHITE,radius));}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    private void gap(LinearLayout l,int h){l.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private Button button(String value,boolean primary){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(primary?Color.WHITE:accent);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(12),0,dp(12),0);
        Drawable d=glass(18,true);if(primary){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF008B9C,0xFF3462C0});g.setCornerRadius(dp(18));d=g;}
        b.setBackground(ripple(d,18));return b;}
    private EditText input(String hint,int id){EditText e=new EditText(this);e.setId(id);e.setHint(hint);e.setTextColor(ink);e.setHintTextColor(muted);e.setTextSize(15);e.setSingleLine(true);e.setPadding(dp(13),0,dp(13),0);e.setBackground(ripple(glass(14,false),14));return e;}
    private LinearLayout card(){LinearLayout l=column();l.setPadding(dp(16),dp(16),dp(16),dp(16));l.setBackground(glass(24,true));l.setElevation(dp(2));return l;}
    private ScrollView scrolling(View child){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.setPadding(dp(20),dp(6),dp(20),dp(12));s.setVerticalScrollBarEnabled(false);s.addView(child);return s;}
    public void onCreate(Bundle state){
        setTheme(R.style.AppTheme);super.onCreate(state);
        if(Build.VERSION.SDK_INT>=31)getSplashScreen().setOnExitAnimationListener(s->{if(android.animation.ValueAnimator.areAnimatorsEnabled())s.animate().alpha(0).setDuration(180).withEndAction(s::remove).start();else s.remove();});
        SocialAudio.warmUp(getApplicationContext());
        getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(0xFFDCECFA);
        FrameLayout scene=new FrameLayout(this);scene.addView(new WaterBackground(this),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout root=column();scene.addView(root,new FrameLayout.LayoutParams(-1,-1));
        if(Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,i)->{Insets bars=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return i;});
        else root.setFitsSystemWindows(true);
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);brand.setPadding(dp(22),dp(7),dp(22),dp(7));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_drop);brand.addView(logo,new LinearLayout.LayoutParams(dp(34),dp(38)));
        TextView title=text("  Pocket Media",20,ink,true);brand.addView(title,new LinearLayout.LayoutParams(0,dp(38),1));title.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge=text("ON DEVICE",10,accent,true);badge.setPadding(dp(10),dp(7),dp(10),dp(7));badge.setBackground(glass(20,false));brand.addView(badge);root.addView(brand,new LinearLayout.LayoutParams(-1,dp(60)));
        pages=new FrameLayout(this);root.addView(pages,new LinearLayout.LayoutParams(-1,0,1));
        screens=new View[]{buildHome(),buildHistory(),buildSettings()};for(View s:screens)pages.addView(s,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(5),dp(5),dp(5),dp(5));nav.setBackground(glass(28,true));nav.setElevation(dp(5));
        tabs=new Button[3];String[] names={"↓  Download","▤  Recent","⚙  Settings"};
        for(int j=0;j<3;j++){final int index=j;tabs[j]=button(names[j],false);tabs[j].setTextSize(13);nav.addView(tabs[j],new LinearLayout.LayoutParams(0,dp(48),1));tabs[j].setOnClickListener(v->selectTab(index,true));}
        LinearLayout.LayoutParams navLp=new LinearLayout.LayoutParams(-1,dp(58));navLp.setMargins(dp(16),dp(4),dp(16),dp(10));root.addView(nav,navLp);
        setContentView(scene);
        SharedPreferences prefs=getSharedPreferences("draft",0);int savedMode=state!=null?state.getInt("mode"):prefs.getInt("mode",0);
        pendingQuality=state!=null?state.getInt("quality",savedMode==1?2:1):prefs.getInt("quality",savedMode==1?2:1);
        mode.setSelection(savedMode);setQualities(savedMode);
        link.setText(state!=null?state.getString("link",""):prefs.getString("link",""));name.setText(state!=null?state.getString("name","My media"):prefs.getString("name","My media"));
        selectTab(state!=null?state.getInt("tab",0):0,false);
        if(state==null)receiveSharedLink(getIntent());
        render();
    }
    private View buildHome(){
        LinearLayout home=column();TextView title=text("A link. A little magic.",25,ink,true);home.addView(title);gap(home,5);home.addView(text("Your favourite moments, saved.",13,muted,false));gap(home,16);
        LinearLayout c=card();home.addView(c);
        c.addView(text("VIDEO LINK",10,accent,true));gap(c,7);
        LinearLayout row=new LinearLayout(this);link=input("Paste your video link",R.id.link_input);link.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);row.addView(link,new LinearLayout.LayoutParams(0,dp(48),1));
        Button paste=button("Paste",false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(72),dp(48));p.setMarginStart(dp(6));row.addView(paste,p);c.addView(row);
        paste.setOnClickListener(v->{if(ConvertService.busy||pendingStart)return;ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cm.hasPrimaryClip()&&cm.getPrimaryClip()!=null){link.setText(cm.getPrimaryClip().getItemAt(0).coerceToText(this));link.setSelection(link.length());}else Toast.makeText(this,"Copy a video link first",Toast.LENGTH_SHORT).show();});
        gap(c,13);LinearLayout labels=new LinearLayout(this);labels.addView(text("FORMAT",10,accent,true),new LinearLayout.LayoutParams(0,-2,1));labels.addView(text("QUALITY",10,accent,true),new LinearLayout.LayoutParams(0,-2,1));c.addView(labels);gap(c,5);
        LinearLayout choices=new LinearLayout(this);mode=new Spinner(this);mode.setId(R.id.mode_input);mode.setBackground(ripple(glass(14,false),14));mode.setAdapter(adapter(new String[]{"Audio · MP3","Video · MP4"}));quality=new Spinner(this);quality.setId(R.id.quality_input);quality.setBackground(ripple(glass(14,false),14));
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,dp(48),1);mp.setMarginEnd(dp(7));choices.addView(mode,mp);choices.addView(quality,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(choices);
        mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){}public void onItemSelected(AdapterView<?> a,View v,int pos,long id){setQualities(pos);}});
        gap(c,13);c.addView(text("FILE NAME",10,accent,true));gap(c,5);name=input("My media",R.id.name_input);c.addView(name,new LinearLayout.LayoutParams(-1,dp(44)));
        gap(c,10);c.addView(text("Best available quality up to your selection.",11,muted,false));gap(c,13);
        convert=button("Download MP3",true);convert.setId(R.id.download_button);c.addView(convert,new LinearLayout.LayoutParams(-1,dp(52)));convert.setOnClickListener(v->start());
        gap(home,15);heading=text("Ready when you are",15,ink,true);home.addView(heading);gap(home,4);detail=text("MP3 to Music · MP4 to Gallery",12,muted,false);detail.setMaxLines(2);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);home.addView(detail);
        heading.setOnClickListener(v->{if(ConvertService.result!=null&&!ConvertService.busy)selectTab(1,true);});
        detail.setOnClickListener(v->{if(!ConvertService.detail.isEmpty())new AlertDialog.Builder(this).setTitle(ConvertService.status).setMessage(ConvertService.detail).setPositiveButton("OK",null).show();});
        gap(home,8);progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setProgressTintList(ColorStateList.valueOf(accent));home.addView(progress,new LinearLayout.LayoutParams(-1,dp(5)));
        cancel=button("Cancel download",false);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(48));cp.topMargin=dp(7);home.addView(cancel,cp);cancel.setOnClickListener(v->{startService(new Intent(this,ConvertService.class).setAction("cancel"));cancel.setEnabled(false);cancel.setText("Cancelling…");});
        return scrolling(home);
    }
    private ArrayAdapter<String> adapter(String[] values){return new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,values){
        public View getView(int pos,View view,android.view.ViewGroup parent){TextView t=(TextView)super.getView(pos,view,parent);t.setTextSize(13);t.setTextColor(ink);t.setSingleLine();t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setPadding(dp(10),0,dp(5),0);return t;}
        public View getDropDownView(int pos,View view,android.view.ViewGroup parent){TextView t=(TextView)super.getDropDownView(pos,view,parent);t.setTextColor(ink);t.setTextSize(15);t.setPadding(dp(16),dp(14),dp(16),dp(14));return t;}
    };}
    private void setQualities(int selected){if(lastMode==selected)return;lastMode=selected;
        String[] q=selected==1?new String[]{"Best · Up to 8K","720p HD","1080p Full HD","1080p · 60 fps","4K · 2160p","6K · 3240p","8K · 4320p"}:new String[]{"128 kbps","192 kbps","320 kbps"};
        quality.setAdapter(adapter(q));quality.setSelection(pendingQuality>=0?Math.min(pendingQuality,q.length-1):(selected==1?2:1));pendingQuality=-1;
    }
    private View buildHistory(){LinearLayout l=column();l.addView(text("Your collection",26,ink,true));gap(l,5);l.addView(text("Recent downloads · Always on your phone",13,muted,false));gap(l,18);historyList=column();l.addView(historyList);return scrolling(l);}
    private void refreshHistory(){String value=getSharedPreferences("audio",0).getString("history","[]");if(value.equals(historyVersion))return;historyVersion=value;historyList.removeAllViews();
        try{JSONArray items=new JSONArray(value);if(items.length()==0){LinearLayout empty=card();empty.setPadding(dp(22),dp(36),dp(22),dp(36));empty.addView(text("Your first save starts here",20,ink,true));gap(empty,10);empty.addView(text("Download a video or audio track. It will appear here, ready to open or share.",14,muted,false));gap(empty,20);Button back=button("Download something",true);empty.addView(back);back.setOnClickListener(v->selectTab(0,true));historyList.addView(empty);}
            for(int j=0;j<items.length();j++){JSONObject item=items.getJSONObject(j);Uri uri=Uri.parse(item.getString("uri"));String mime=item.getString("mime");LinearLayout c=card();historyList.addView(c);c.addView(text(mime.startsWith("video")?"MP4  ·  VIDEO":"MP3  ·  AUDIO",10,accent,true));gap(c,7);TextView filename=text(item.getString("name"),15,ink,true);filename.setMaxLines(2);filename.setEllipsize(android.text.TextUtils.TruncateAt.END);c.addView(filename);gap(c,5);c.addView(text(item.optString("info","Saved on your phone"),12,muted,false));gap(c,12);LinearLayout actions=new LinearLayout(this);Button open=button("Open",true),share=button("Share",false);LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(48),1);a.setMarginEnd(dp(8));actions.addView(open,a);actions.addView(share,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(actions);open.setOnClickListener(v->useMedia(uri,mime,false));share.setOnClickListener(v->useMedia(uri,mime,true));gap(historyList,12);}
        }catch(JSONException e){historyList.addView(text("Could not read recent downloads. Your files are still in Music and Movies / PocketMedia.",14,muted,false));}}
    private View buildSettings(){LinearLayout l=column();l.addView(text("Make it yours",26,ink,true));gap(l,5);l.addView(text("Simple tools. No account needed.",13,muted,false));gap(l,18);
        LinearLayout c=card();l.addView(c);c.addView(text("Video support",18,ink,true));gap(c,8);c.addView(text("Downloads use the installed tools immediately. Check for an update here if a public link stops working.",13,muted,false));gap(c,14);update=button("Update video support",true);c.addView(update,new LinearLayout.LayoutParams(-1,dp(48)));updateDetail=text(SocialAudio.updateStatus,12,muted,false);gap(c,10);c.addView(updateDetail);update.setOnClickListener(v->{if(!SocialAudio.requestUpdate(getApplicationContext()))Toast.makeText(this,"Finish the current download or update first.",Toast.LENGTH_SHORT).show();render();});
        gap(l,14);LinearLayout storage=card();l.addView(storage);storage.addView(text("Saved where you need it",18,ink,true));gap(storage,8);storage.addView(text("Videos → Gallery / Movies / PocketMedia\nAudio → Music / PocketMedia\n\nUp to 8K, with 60 fps preferred when available. Quality depends on the original video; playback depends on your phone.\n\nPrivate, login-required and protected videos may not download. Platforms may also limit requests.",13,muted,false));
        gap(l,16);Button about=button("About & open-source licenses",false);l.addView(about);about.setOnClickListener(v->about());gap(l,10);l.addView(text("POCKET MEDIA 4.0  ·  LIQUID",11,muted,true));return scrolling(l);}
    private void selectTab(int index,boolean animate){selectedTab=Math.max(0,Math.min(2,index));for(int j=0;j<3;j++){screens[j].setVisibility(j==selectedTab?View.VISIBLE:View.GONE);tabs[j].setTextColor(j==selectedTab?Color.WHITE:muted);tabs[j].setBackground(ripple(j==selectedTab?shape(accent,23):shape(Color.TRANSPARENT,23),23));tabs[j].setSelected(j==selectedTab);}if(selectedTab==1)refreshHistory();
        if(animate){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(link.getWindowToken(),0);if(android.animation.ValueAnimator.areAnimatorsEnabled()){View active=screens[selectedTab];active.animate().cancel();active.setAlpha(0.6f);active.animate().alpha(1).setDuration(140).start();}}}
    private void start(){if(ConvertService.busy||pendingStart||SocialAudio.updating)return;
        try{link.setText(ConvertService.normalize(link.getText().toString()));}catch(Exception e){link.setError("Paste a full public video URL");link.requestFocus();return;}
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(link.getWindowToken(),0);link.clearFocus();name.clearFocus();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},5);
        boolean video=mode.getSelectedItemPosition()==1;int q=quality.getSelectedItemPosition();
        Intent i=new Intent(this,ConvertService.class).putExtra("url",link.getText().toString().trim()).putExtra("name",name.getText().toString()).putExtra("video",video).putExtra("height",video?new int[]{4320,720,1080,1080,2160,3240,4320}[q]:1080).putExtra("bitrate",video?192:new int[]{128,192,320}[q]);
        pendingStart=true;pressedAt=SystemClock.elapsedRealtime();render();
        try{startForegroundService(i);}catch(Exception e){pendingStart=false;render();Toast.makeText(this,"Could not start. Keep the app open and retry.",Toast.LENGTH_LONG).show();}}
    private void render(){if(convert==null)return;boolean b=ConvertService.busy;if(b||SystemClock.elapsedRealtime()-pressedAt>4000)pendingStart=false;boolean working=b||pendingStart;
        convert.setEnabled(!working&&!SocialAudio.updating);link.setEnabled(!working);name.setEnabled(!working);quality.setEnabled(!working);mode.setEnabled(!working);convert.setText(working?"Working…":SocialAudio.updating?"Updating tools…":mode.getSelectedItemPosition()==1?"Download MP4":"Download MP3");
        heading.setText(pendingStart?"Starting now…":ConvertService.result!=null&&!b?"Saved · View in Recent":ConvertService.status);
        String d=ConvertService.detail;if(b){long seconds=(SystemClock.elapsedRealtime()-ConvertService.startedAt)/1000;d=seconds+"s · "+d;if(seconds>=15&&ConvertService.progress<0)d=seconds+"s · Waiting for the platform. You can switch tabs or cancel.";}
        detail.setText(pendingStart?"Opening your link…":d);progress.setVisibility(working?View.VISIBLE:View.GONE);progress.setIndeterminate(pendingStart||ConvertService.progress<0);if(ConvertService.progress>=0)progress.setProgress(ConvertService.progress);
        if(!b){cancel.setText("Cancel download");cancel.setEnabled(true);}cancel.setVisibility(b?View.VISIBLE:View.GONE);
        update.setEnabled(!working&&!SocialAudio.updating);update.setText(SocialAudio.updating?"Updating…":"Update video support");updateDetail.setText(SocialAudio.updateStatus);if(selectedTab==1)refreshHistory();}
    private void useMedia(Uri uri,String mime,boolean sharing){try{try(android.content.res.AssetFileDescriptor f=getContentResolver().openAssetFileDescriptor(uri,"r")){if(f==null)throw new java.io.FileNotFoundException();}
        Intent i=new Intent(sharing?Intent.ACTION_SEND:Intent.ACTION_VIEW);if(sharing){i.setType(mime);i.putExtra(Intent.EXTRA_STREAM,uri);i.setClipData(ClipData.newRawUri("Saved media",uri));}else i.setDataAndType(uri,mime);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,sharing?"Share media":"Open media"));
        }catch(Exception e){Toast.makeText(this,"File unavailable, or no compatible player. Check Music / Movies → PocketMedia in Files.",Toast.LENGTH_LONG).show();}}
    private void about(){String license="Pocket Media 4.0 · GPL-3.0\nyt-dlp / youtubedl-android 0.18.1\nFFmpeg · LAME 3.100 (LGPL 2.0 or later)\n\nSource and license:\nhttps://github.com/ahtishamite/webstorage/tree/pocket-audio-app/PocketAudio\n\n";
        try(java.io.InputStream in=getAssets().open("LAME-LICENSE.txt")){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)bytes.write(buf,0,n);license+=bytes.toString("UTF-8");}catch(Exception ignored){}
        TextView t=text(license,13,ink,false);t.setPadding(dp(18),dp(14),dp(18),dp(14));android.text.util.Linkify.addLinks(t,android.text.util.Linkify.WEB_URLS);ScrollView sv=new ScrollView(this);sv.addView(t);new AlertDialog.Builder(this).setTitle("About Pocket Media").setView(sv).setPositiveButton("Close",null).show();}
    protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);receiveSharedLink(i);}
    private void receiveSharedLink(Intent i){if(Intent.ACTION_SEND.equals(i.getAction())&&i.hasExtra(Intent.EXTRA_TEXT)){if(ConvertService.busy)Toast.makeText(this,"Finish the current download, then share the next link.",Toast.LENGTH_SHORT).show();else{link.setText(i.getStringExtra(Intent.EXTRA_TEXT));selectTab(0,false);}}}
    protected void onResume(){super.onResume();handler.post(refresh);}
    protected void onPause(){handler.removeCallbacks(refresh);getSharedPreferences("draft",0).edit().putString("link",link.getText().toString()).putString("name",name.getText().toString()).putInt("mode",mode.getSelectedItemPosition()).putInt("quality",quality.getSelectedItemPosition()).apply();super.onPause();}
    protected void onSaveInstanceState(Bundle b){b.putInt("mode",mode.getSelectedItemPosition());b.putInt("quality",quality.getSelectedItemPosition());b.putInt("tab",selectedTab);b.putString("link",link.getText().toString());b.putString("name",name.getText().toString());super.onSaveInstanceState(b);}
    @Override public void onBackPressed(){if(selectedTab!=0)selectTab(0,true);else super.onBackPressed();}
    private static class WaterBackground extends View {
        final Paint paint=new Paint(3);WaterBackground(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xFFD9F5F4,0xFFE9EFFC,0xFFDCDFF9},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);
            orb(c,w*.96f,h*.24f,w*.70f,0x6692CFEE);orb(c,w*.04f,h*.68f,w*.67f,0x7798E7DA);orb(c,w*.83f,h*.90f,w*.7f,0x559DADF5);
            drop(c,w*.90f,h*.115f,w*.105f);drop(c,w*.025f,h*.51f,w*.072f);}
        void orb(Canvas c,float x,float y,float r,int color){paint.setShader(new RadialGradient(x,y,r,new int[]{color,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r,paint);}
        void drop(Canvas c,float x,float y,float r){paint.setShader(new LinearGradient(x-r,y-r,x+r,y+r,new int[]{0xBFFFFFFF,0x2282BFD7,0x99FFFFFF},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r,paint);paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);paint.setColor(0xBFFFFFFF);c.drawCircle(x,y,r,paint);paint.setStyle(Paint.Style.FILL);orb(c,x-r*.3f,y-r*.4f,r*.43f,0xCCFFFFFF);}
    }
}
