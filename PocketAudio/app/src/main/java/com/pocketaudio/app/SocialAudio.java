package com.pocketaudio.app;

import android.content.Context;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.ffmpeg.FFmpeg;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import kotlin.Unit;

final class SocialAudio {
    interface Progress { void update(String title,String message,int percent); boolean cancelled(); }
    static final String PROCESS="pocket-audio";
    static void cancel(){new Thread(()->{try{YoutubeDL.getInstance().destroyProcessById(PROCESS);}catch(Exception ignored){}},"cancel-extractor").start();}
    static void convert(Context c,String url,File output,int bitrate,boolean video,int height,Progress p) throws Exception {
        File work=new File(c.getCacheDir(),"social-"+System.nanoTime());
        if(!work.mkdirs())throw new IOException("Could not create temporary storage.");
        try {
            p.update("Preparing video tools","First use may take a little longer.",-1);
            YoutubeDL.getInstance().init(c);
            FFmpeg.getInstance().init(c);
            if(p.cancelled())throw new CancellationException();
            long last=c.getSharedPreferences("engine",Context.MODE_PRIVATE).getLong("checked",0);
            if(System.currentTimeMillis()-last>86400000L){
                p.update("Checking video support","Checking for updated YouTube and Instagram support…",-1);
                try{YoutubeDL.getInstance().updateYoutubeDL(c,YoutubeDL.UpdateChannel._STABLE);c.getSharedPreferences("engine",0).edit().putLong("checked",System.currentTimeMillis()).apply();}catch(Exception ignored){}
            }
            if(p.cancelled())throw new CancellationException();
            p.update("Reading video link","Finding audio from your video…",-1);
            YoutubeDLRequest r=new YoutubeDLRequest(url);
            r.addOption("--no-playlist");r.addOption("--playlist-items","1");
            r.addOption("--socket-timeout","20");r.addOption("--retries","2");r.addOption("--extractor-retries","2");
            r.addOption("--max-filesize",String.valueOf(Math.max(1,c.getCacheDir().getUsableSpace()/3)));r.addOption("--no-mtime");r.addOption("--newline");
            if(video){
                String landscape="[height<=?"+height+"]", portrait="[width<=?"+height+"]";
                r.addOption("-f","bv"+landscape+"+ba/bv"+portrait+"+ba/b"+landscape+"/b"+portrait);
                r.addOption("-S","res:"+height+",fps:60,ext:mp4:m4a");
                r.addOption("--merge-output-format","mp4");r.addOption("--remux-video","mp4");
            }else{r.addOption("-f","bestaudio/best");r.addOption("-x");r.addOption("--audio-format","mp3");r.addOption("--audio-quality",bitrate+"K");}
            r.addOption("--fragment-retries","3");r.addOption("--concurrent-fragments","2");
            r.addOption("-o",new File(work,"audio.%(ext)s").getAbsolutePath());
            YoutubeDL.getInstance().execute(r,PROCESS,(progress,eta,line)->{
                boolean encoding=line.contains("ExtractAudio")||line.contains("ffmpeg");
                p.update(encoding?(video?"Merging video and audio":"Converting to MP3"):(video?"Downloading video":"Downloading audio"),encoding?"Finishing your media on this phone.":"You can leave this screen while the download finishes.",encoding?94:Math.min(90,Math.max(1,(int)(progress*0.9f))));
                return Unit.INSTANCE;
            });
            if(p.cancelled())throw new CancellationException();
            File mp3=new File(work,video?"audio.mp4":"audio.mp3");
            if(!mp3.isFile()||mp3.length()==0)throw new IOException("No audio was returned. Check that the link opens a public video.");
            Files.move(mp3.toPath(),output.toPath(),StandardCopyOption.REPLACE_EXISTING);
        } catch(com.yausername.youtubedl_android.YoutubeDLException e){
            if(p.cancelled())throw new CancellationException();
            String err=String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
            if(err.contains("sign in")||err.contains("login")||err.contains("cookies")||err.contains("private")||err.contains("confirm you're not a bot"))
                throw new IOException("The platform requires sign-in or has blocked this request. Private/restricted videos cannot be downloaded without login. Try a public video or retry later on your normal connection.");
            if(err.contains("429")||err.contains("rate-limit")||err.contains("rate limit"))throw new IOException("The platform is temporarily limiting downloads. Wait a while and try again.");
            if(err.contains("unsupported url"))throw new IOException("This link is not supported. Paste the full public YouTube video, Short, Instagram Reel/post, or direct MP4 URL.");
            if(err.contains("403"))throw new IOException("The video host refused the download. Try again later with a fresh public link; some videos require login.");
            if(err.contains("unavailable")||err.contains("not available"))throw new IOException("This video is unavailable, removed, or restricted in your region.");
            if(err.contains("no space"))throw new IOException("Not enough phone storage. Free some space and try again.");
            throw new IOException("Could not download media from this link. Check that it is a public video, then retry. The platform may have changed or blocked access.");
        } finally {delete(work);}
    }
    private static void delete(File f){File[] a=f.listFiles();if(a!=null)for(File x:a)delete(x);f.delete();}
}
