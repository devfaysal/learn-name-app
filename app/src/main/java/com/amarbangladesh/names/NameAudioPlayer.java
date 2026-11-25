package com.amarbangladesh.names;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;

/** One short, bundled pronunciation at a time. All methods run on the main thread. */
final class NameAudioPlayer {
    private final Context context;
    private final AudioManager manager;
    private final AudioAttributes attributes=new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
    private MediaPlayer player;
    private AudioFocusRequest focus;
    private String activeId="";
    private boolean started=false;
    private Runnable finished;

    NameAudioPlayer(Context context) {
        this.context=context.getApplicationContext();
        manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    }
    boolean isActive(String id,String language) {return player!=null&&activeId.equals(language+"/"+id);}
    boolean isPlaying() {return started&&player!=null&&player.isPlaying();}

    void play(String id,String language,Runnable onFinished,Runnable onError) {
        stop();
        if(!language.equals("bn")&&!language.equals("en")){onFinished.run();onError.run();return;}
        finished=onFinished;
        MediaPlayer current=new MediaPlayer();player=current;activeId=language+"/"+id;
        try {
            current.setAudioAttributes(attributes);
            current.setOnPreparedListener(p->{
                if(player!=p)return;
                focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(change->{if(change<0&&player==p)stop();}).build();
                if(manager.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    stop();onError.run();return;
                }
                try {p.start();started=true;} catch(IllegalStateException ex) {stop();onError.run();}
            });
            current.setOnCompletionListener(p->{if(player==p)stop();});
            current.setOnErrorListener((p,what,extra)->{
                if(player==p){stop();onError.run();}return true;
            });
            try(AssetFileDescriptor asset=context.getAssets().openFd("audio/"+language+"/"+id+".mp3")) {
                current.setDataSource(asset.getFileDescriptor(),asset.getStartOffset(),asset.getLength());
            }
            current.prepareAsync();
        } catch(Exception ex) {stop();onError.run();}
    }
    void stop() {
        MediaPlayer old=player;player=null;activeId="";started=false;
        if(old!=null)old.release();
        if(focus!=null){manager.abandonAudioFocusRequest(focus);focus=null;}
        Runnable callback=finished;finished=null;
        if(callback!=null)callback.run();
    }
}
