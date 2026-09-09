package com.pocketaudio.app;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.concurrent.CancellationException;

public class ConvertService extends Service {
    static volatile boolean busy=false;
    static volatile int progress=0;
    static volatile String status="Ready when you are", detail="Paste a direct MP4 link to get started.";
    static volatile Uri result;
    private volatile boolean cancelled;
    private volatile HttpURLConnection connection;
    private PowerManager.WakeLock wake;
    private static final long MAX_BYTES=1024L*1024*1024;
    private static final String CHANNEL="conversion";
    private long lastUpdate;
    public IBinder onBind(Intent intent){ return null; }
    public void onCreate(){
        super.onCreate();
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Audio conversion",NotificationManager.IMPORTANCE_LOW));
    }
    public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null && "cancel".equals(intent.getAction())) { cancel(); return START_NOT_STICKY; }
        if(busy) return START_NOT_STICKY;
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        busy=true; cancelled=false; result=null; progress=0;
        status="Starting…";detail="Preparing your conversion.";
        startForeground(7,notification());
        wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"PocketAudio:convert");
        wake.acquire(2*60*60*1000L);
        String url=intent.getStringExtra("url"), name=intent.getStringExtra("name");
        int bitrate=intent.getIntExtra("bitrate",192);
        new Thread(()->runJob(url,name,bitrate),"audio-conversion").start();
        return START_NOT_STICKY;
    }
    private Notification notification(){
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent tap=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent cancel=PendingIntent.getService(this,1,new Intent(this,ConvertService.class).setAction("cancel"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_audio).setContentTitle(status).setContentText(detail)
            .setContentIntent(tap).setOnlyAlertOnce(true).setOngoing(true).setProgress(100,Math.max(0,progress),progress<0)
            .addAction(new Notification.Action.Builder(null,"Cancel",cancel).build()).build();
    }
    @android.annotation.SuppressLint("MissingPermission") // Foreground conversion works even when notification permission is declined.
    private void update(String s,String d,int p){
        status=s;detail=d;progress=p;
        long now=SystemClock.elapsedRealtime();
        if(now-lastUpdate>350){getSystemService(NotificationManager.class).notify(7,notification());lastUpdate=now;}
    }
    private void check(){if(cancelled || Thread.currentThread().isInterrupted())throw new CancellationException();}
    private void cancel(){cancelled=true;HttpURLConnection c=connection;if(c!=null)c.disconnect();}
    public void onTimeout(int startId,int fgsType){cancel();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    public void onDestroy(){cancel();if(wake!=null && wake.isHeld())wake.release();super.onDestroy();}
    static URL checkedUrl(String text) throws Exception {
        URL u=new URI(text.trim()).toURL();
        if(!("https".equalsIgnoreCase(u.getProtocol())||"http".equalsIgnoreCase(u.getProtocol())) || u.getHost().isEmpty() || u.getUserInfo()!=null)
            throw new IOException("Enter a public HTTP or HTTPS video link without login details.");
        return u;
    }
    private void download(String link,File dest) throws Exception {
        URL u=checkedUrl(link);
        for(int redirect=0;redirect<=5;redirect++){
            check();
            HttpURLConnection c=(HttpURLConnection)u.openConnection();connection=c;
            c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","PocketAudio/1.0");c.setRequestProperty("Accept-Encoding","identity");
            try {
                int code=c.getResponseCode();
                if(code>=300 && code<400){
                    String loc=c.getHeaderField("Location");
                    if(loc==null)throw new IOException("This link redirects without a destination.");
                    URL next=checkedUrl(new URL(u,loc).toString());
                    if(u.getProtocol().equalsIgnoreCase("https") && next.getProtocol().equalsIgnoreCase("http"))throw new IOException("The link redirects to an insecure download. Use the final HTTPS MP4 link.");
                    u=next;continue;
                }
                if(code==401||code==403)throw new IOException("This link is private, expired or blocks downloads. Try a fresh public MP4 link.");
                if(code!=200)throw new IOException("The server could not download this link (HTTP "+code+").");
                String type=c.getContentType();
                if(type!=null && (type.contains("text/html")||type.contains("application/json")))throw new IOException("This is a webpage, not a direct video file. Paste the MP4 download link.");
                long total=c.getContentLengthLong();
                if(total>MAX_BYTES)throw new IOException("This version supports videos up to 1 GB.");
                if(total>0 && total+64*1024*1024L>getCacheDir().getUsableSpace())throw new IOException("Not enough phone storage. Free some space and try again.");
                long count=0; byte[] buf=new byte[65536];
                try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(dest)){
                    int n;
                    while((n=in.read(buf))!=-1){
                        check(); count+=n;
                        if(count>MAX_BYTES)throw new IOException("This version supports videos up to 1 GB.");
                        if(getCacheDir().getUsableSpace()<32*1024*1024L)throw new IOException("Phone storage is almost full.");
                        out.write(buf,0,n);
                        update("Downloading video",String.format(java.util.Locale.US,"%.1f MB downloaded",count/1048576.0),total>0?(int)(count*40/total):-1);
                    }
                }
                if(count==0 || (total>0 && count!=total))throw new IOException("The download was incomplete. Please retry.");
                return;
            } finally {c.disconnect();connection=null;}
        }
        throw new IOException("Too many redirects. Use the final MP4 download link.");
    }
    private void decode(File input,File output,int bitrate) throws Exception {
        MediaExtractor ex=new MediaExtractor();MediaCodec codec=null;Mp3 encoder=null;
        try(OutputStream out=new BufferedOutputStream(new FileOutputStream(output))){
            ex.setDataSource(input.getAbsolutePath());MediaFormat fmt=null;
            for(int i=0;i<ex.getTrackCount();i++){
                MediaFormat f=ex.getTrackFormat(i);String mime=f.getString(MediaFormat.KEY_MIME);
                if(mime!=null && mime.startsWith("audio/")){fmt=f;ex.selectTrack(i);break;}
            }
            if(fmt==null)throw new IOException("This video has no supported audio track.");
            long duration=fmt.containsKey(MediaFormat.KEY_DURATION)?fmt.getLong(MediaFormat.KEY_DURATION):0;
            fmt.setInteger(MediaFormat.KEY_PCM_ENCODING,AudioFormat.ENCODING_PCM_16BIT);
            codec=MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME));codec.configure(fmt,null,null,0);codec.start();
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();boolean inputDone=false,outputDone=false;
            int channels=0,rate=0;long framesWritten=0,lastActivity=SystemClock.elapsedRealtime();byte[] mp3=new byte[32768];
            update("Converting to MP3","Audio stays on your phone.",40);
            while(!outputDone){
                check();
                if(SystemClock.elapsedRealtime()-lastActivity>60000)throw new IOException("The audio decoder stopped responding. Try another MP4 file.");
                if(!inputDone){
                    int i=codec.dequeueInputBuffer(10000);
                    if(i>=0){
                        ByteBuffer in=codec.getInputBuffer(i); if(in==null)throw new IOException("Audio decoder unavailable.");in.clear();
                        int n=ex.readSampleData(in,0);
                        if(n<0){codec.queueInputBuffer(i,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputDone=true;}
                        else {
                            if((ex.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_ENCRYPTED)!=0)throw new IOException("Protected videos cannot be converted.");
                            codec.queueInputBuffer(i,0,n,ex.getSampleTime(),0);ex.advance();
                        }
                        lastActivity=SystemClock.elapsedRealtime();
                    }
                }
                int i=codec.dequeueOutputBuffer(info,10000);
                if(i==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    MediaFormat f=codec.getOutputFormat();int ch=f.getInteger(MediaFormat.KEY_CHANNEL_COUNT),sr=f.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int encoding=f.containsKey(MediaFormat.KEY_PCM_ENCODING)?f.getInteger(MediaFormat.KEY_PCM_ENCODING):AudioFormat.ENCODING_PCM_16BIT;
                    if(ch<1||ch>2)throw new IOException("Only mono and stereo audio are supported in this version.");
                    if(encoding!=AudioFormat.ENCODING_PCM_16BIT)throw new IOException("This device returned an unsupported audio format.");
                    if(encoder!=null && (ch!=channels || sr!=rate))throw new IOException("The audio format changes inside this video.");
                    channels=ch;rate=sr;if(encoder==null)encoder=new Mp3(rate,channels,bitrate);
                    lastActivity=SystemClock.elapsedRealtime();
                } else if(i>=0){
                    try {
                        if(info.size>0 && (info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){
                            if(encoder==null)throw new IOException("The decoder did not provide an audio format.");
                            ByteBuffer pcm=codec.getOutputBuffer(i);if(pcm==null)throw new IOException("Empty audio buffer.");
                            pcm.position(info.offset);pcm.limit(info.offset+info.size);pcm.order(ByteOrder.nativeOrder());
                            if(pcm.remaining()%(channels*2)!=0)throw new IOException("The decoded audio is malformed.");
                            while(pcm.remaining()>0){
                                check();int frames=Math.min(1152,pcm.remaining()/(2*channels));short[] samples=new short[frames*channels];
                                for(int j=0;j<samples.length;j++)samples[j]=pcm.getShort();
                                int n=encoder.encode(samples,frames,mp3);out.write(mp3,0,n);framesWritten+=frames;
                            }
                            update("Converting to MP3",bitrate+" kbps · "+channels+" channel"+(channels==2?"s":""),duration>0?40+(int)Math.min(55,Math.max(0,info.presentationTimeUs)*55/duration):-1);
                        }
                        outputDone=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                    } finally {codec.releaseOutputBuffer(i,false);}
                    lastActivity=SystemClock.elapsedRealtime();
                }
            }
            if(encoder==null || framesWritten==0)throw new IOException("This video contains no decodable audio.");
            out.write(mp3,0,encoder.finish(mp3));
        } finally {
            if(encoder!=null)encoder.close();
            if(codec!=null){try{codec.stop();}catch(Exception ignored){} codec.release();}
            ex.release();
        }
    }
    private void runJob(String url,String name,int bitrate){
        File video=null,audio=null;Uri pending=null;
        try {
            video=File.createTempFile("input-",".mp4",getCacheDir());audio=File.createTempFile("audio-",".mp3",getCacheDir());
            download(url,video);check();decode(video,audio,bitrate);check();
            update("Saving audio","Adding MP3 to Downloads / PocketAudio…",97);
            String safe=(name==null?"audio":name).replaceAll("(?i)\\.mp3$", "").replaceAll("[^\\p{L}\\p{N} _-]","_").trim();
            if(safe.isEmpty())safe="audio";if(safe.length()>80)safe=safe.substring(0,80);
            String filename=safe+"-"+System.currentTimeMillis()+".mp3";
            ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,filename);values.put(MediaStore.Downloads.MIME_TYPE,"audio/mpeg");
            values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/PocketAudio");values.put(MediaStore.Downloads.IS_PENDING,1);
            pending=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
            if(pending==null)throw new IOException("Could not create the audio file in Downloads.");
            try(InputStream in=new FileInputStream(audio);OutputStream out=getContentResolver().openOutputStream(pending)){
                if(out==null)throw new IOException("Could not save the audio file.");
                byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){check();out.write(b,0,n);}
            }
            check();values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);
            if(getContentResolver().update(pending,values,null,null)<1)throw new IOException("Could not finish saving audio.");
            result=pending;pending=null;
            getSharedPreferences("audio",MODE_PRIVATE).edit().putString("last",result.toString()).putString("filename",filename).apply();
            update("Your MP3 is ready",filename+"\nSaved in Downloads / PocketAudio",100);
        } catch(Exception|LinkageError e){
            if(cancelled || e instanceof CancellationException)update("Conversion cancelled","Paste a link whenever you are ready to try again.",0);
            else {
                String message=e instanceof IOException?e.getMessage():"Could not convert this video. Check the link and try a standard MP4 with mono or stereo audio.";
                update("Couldn’t convert this link",message==null?"Please try again.":message,0);
            }
        } finally {
            if(pending!=null)try{getContentResolver().delete(pending,null,null);}catch(Exception ignored){}
            if(video!=null)video.delete();if(audio!=null)audio.delete();
            stopForeground(STOP_FOREGROUND_REMOVE);if(wake!=null && wake.isHeld())wake.release();busy=false;stopSelf();
        }
    }
}
