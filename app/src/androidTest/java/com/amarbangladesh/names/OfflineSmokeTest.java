package com.amarbangladesh.names;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** End-to-end tests using only the Android platform: no downloaded test libraries. */
public final class OfflineSmokeTest extends Instrumentation {
    private Activity activity;
    private int checks=0;
    private String previewPrefix="";
    private boolean capture=false;
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);previewPrefix=arguments.getString("previewPrefix","");capture=arguments.getString("capture","false").equals("true");start();
    }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            PackageInfo info=getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),PackageManager.GET_PERMISSIONS);
            check(info.requestedPermissions==null||info.requestedPermissions.length==0,"APK requests no permissions");
            Catalog catalog=new Catalog(getTargetContext());
            check(catalog.categories.size()==10,"Ten categories");check(catalog.entries.size()==200,"200 entries");
            Set<String> unique=new HashSet<>();
            for(Catalog.Category c:catalog.categories)check(catalog.filter(c.id,"").size()==20,"20 names in "+c.id);
            for(Catalog.Entry e:catalog.entries) {
                check(unique.add(e.id),"Unique ID "+e.id);check(!e.bn.isEmpty()&&!e.en.isEmpty(),"Bilingual "+e.id);
                check(catalog.picture(e).getWidth()==480,"Bundled picture "+e.id);
                for(String language:new String[]{"bn","en"}) {
                    android.media.MediaMetadataRetriever metadata=new android.media.MediaMetadataRetriever();
                    try(android.content.res.AssetFileDescriptor audio=getTargetContext().getAssets().openFd("audio/"+language+"/"+e.id+".mp3")) {
                        metadata.setDataSource(audio.getFileDescriptor(),audio.getStartOffset(),audio.getLength());
                        long duration=Long.parseLong(metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION));
                        check(duration>=300&&duration<=15000,"Decodable bundled "+language+" audio: "+e.id);
                    } finally {metadata.release();}
                }
            }
            check(catalog.filter("","  MANGO  ").size()==1,"Case insensitive trimmed English search");
            check(catalog.filter("","মাগুর").size()==1,"Bangla search");
            check(catalog.filter("vehicles","হেলিকপ্টার").size()==1,"Bangla vehicle search");
            check(catalog.filter("vehicles","  AUTO RICKSHAW  ").size()==1,"English vehicle search");
            check(catalog.filter("insects","গান্ধি পোকা").size()==1,"Bangla insect search");
            check(catalog.filter("insects","  SILKWORM  ").size()==1,"English insect search");
            check(catalog.filter("colors","ফিরোজা").size()==1,"Bangla color search");
            check(catalog.filter("crops","কালোজিরা").size()==1,"Bangla nigella search");
            check(catalog.filter("crops","ধনে").size()==1,"Bangla coriander seed search");
            check(catalog.filter("crops","  CORIANDER SEEDS  ").size()==1,"English crop search");
            check(catalog.filter("crops","Oats").isEmpty(),"Oats excluded from agreed crops");
            check(catalog.filter("crops","Pearl millet").isEmpty(),"Pearl millet excluded from agreed crops");
            check(catalog.filter("","Sunflower").size()==2,"Flower and sunflower seed both searchable");
            check(catalog.filter("colors","  NAVY BLUE  ").size()==1,"English color search");
            check(catalog.filter("","কমলা").size()==2,"Orange fruit and color both searchable in Bangla");
            check(catalog.filter("","Orange").size()==2,"Orange fruit and color both searchable in English");
            check(catalog.filter("colors","Orange").get(0).id.equals("color_orange"),"Color orange separate from fruit");
            check(catalog.filter("fruits","Orange").get(0).id.equals("orange"),"Fruit orange preserved");
            check(catalog.filter("vegetables","কাঁকরোল").size()==1,"Spine gourd Bangla search");
            check(catalog.filter("vegetables","  SPINE GOURD  ").size()==1,"Spine gourd English search");
            check(catalog.filter("vegetables","Spinach").isEmpty(),"Spinach removed");
            check(catalog.filter("body_parts","").isEmpty(),"Body parts removed");
            getTargetContext().getSharedPreferences("collection",0).edit().clear().commit();
            Set<String> oldFavorites=new HashSet<>();oldFavorites.add("spinach");oldFavorites.add("head");oldFavorites.add("tiger");
            getTargetContext().getSharedPreferences("collection",0).edit().putStringSet("favorites",oldFavorites).putStringSet("seen",oldFavorites).commit();
            launch();
            Set<String> survivingFavorites=getTargetContext().getSharedPreferences("collection",0).getStringSet("favorites",new HashSet<>());
            check(survivingFavorites.size()==1&&survivingFavorites.contains("tiger"),"Content update preserves current favorites and drops retired entries");
            Set<String> survivingSeen=getTargetContext().getSharedPreferences("collection",0).getStringSet("seen",new HashSet<>());
            check(survivingSeen.size()==1&&survivingSeen.contains("tiger"),"Content update removes retired visited entries");
            ui(()->{
                view("nav_favorites").performClick();check(countEntryCards(view("results"))==1,"Only current favorite visible after content update");
                view("favorite_tiger").performClick();view("nav_explore").performClick();
                check(view("category_body_parts")==null,"Retired category absent from home");
            });
            screenshot("home");
            checkNameAudio();
            ui(()->{
                checkEnglishLabels(activity.getWindow().getDecorView());
                View home=activity.getWindow().getDecorView();
                checkHomeContent(home);
                int[] first=new int[2],nav=new int[2];
                view("category_animals").getLocationOnScreen(first);view("nav_explore").getLocationOnScreen(nav);
                check(first[1]+80< nav[1],"Category artwork visible on initial home screen");
                for(Catalog.Category c:catalog.categories) {
                    TextView name=(TextView)view("category_name_"+c.id);
                    check(name.getLayout()!=null,"Category name laid out: "+c.id);
                    for(int line=0;line<name.getLineCount();line++)
                        check(name.getLayout().getLineMax(line)<=name.getWidth()+1,"Bangla category name fits: "+c.id);
                }
            });
            // Check every English name after layout, including off-screen rows and the longest labels.
            for(Catalog.Category c:catalog.categories) {
                ui(()->view("category_"+c.id).performClick());
                ui(()->{
                    check(countEntryCards(view("results"))==20,"All 20 entries in "+c.id);
                    checkEnglishLabels(view("results"));
                    for(Catalog.Entry e:catalog.filter(c.id,""))checkFavoriteLayout(e.id);
                });
                screenshot("category_"+c.id);
                ui(()->view("nav_explore").performClick());
            }
            ui(()->view("category_crops").performClick());
            ui(()->{
                view("entry_paddy").performClick();
                check(((TextView)view("detail_bn")).getText().toString().equals("ধান"),"Crop detail in Bangla");
                check(((TextView)view("detail_en")).getText().toString().equals("Paddy"),"Crop detail in English");
                view("favorite_paddy").performClick();
                check(view("favorite_paddy").isSelected(),"Crop favorite toggles");
                view("next_entry").performClick();
                check(((TextView)view("detail_en")).getText().toString().equals("Wheat"),"Crop detail paging");
                activity.onBackPressed();view("entry_nigella").performClick();
                check(((TextView)view("detail_bn")).getText().toString().equals("কালোজিরা"),"Nigella replaces oats");
            });
            screenshot("nigella_detail");
            ui(()->{
                view("next_entry").performClick();
                check(((TextView)view("detail_en")).getText().toString().equals("Coriander seeds"),"Coriander follows nigella");
            });
            screenshot("coriander_seed_detail");
            ui(()->{
                activity.onBackPressed();view("entry_sunflower_seed").performClick();
                check(((TextView)view("detail_bn")).getText().toString().equals("সূর্যমুখীর বীজ"),"Last crop reachable");
            });
            ui(()->checkEnglishLabel((TextView)view("detail_en"),18));
            screenshot("sunflower_seed_detail");
            ui(()->{
                activity.onBackPressed();view("nav_favorites").performClick();
                check(view("entry_paddy")!=null,"Crop appears in favorites");
                view("favorite_paddy").performClick();view("nav_explore").performClick();
            });
            ui(()->view("category_colors").performClick());
            ui(()->{
                view("entry_color_red").performClick();
                check(((TextView)view("detail_bn")).getText().toString().equals("লাল"),"Color detail in Bangla");
                check(((TextView)view("detail_en")).getText().toString().equals("Red"),"Color detail in English");
                view("favorite_color_red").performClick();
                check(view("favorite_color_red").isSelected(),"Color favorite toggles");
                view("next_entry").performClick();
                check(((TextView)view("detail_en")).getText().toString().equals("Blue"),"Color detail paging");
                activity.onBackPressed();view("entry_color_gold").performClick();
                check(((TextView)view("detail_bn")).getText().toString().equals("সোনালি"),"Gold detail");
            });
            screenshot("color_gold_detail");
            ui(()->{
                view("next_entry").performClick();
                check(((TextView)view("detail_en")).getText().toString().equals("Silver"),"Silver follows gold");
            });
            screenshot("color_silver_detail");
            ui(()->{
                view("next_entry").performClick();
                check(((TextView)view("detail_bn")).getText().toString().equals("ঘিয়ে"),"Last color reachable");
                activity.onBackPressed();view("entry_color_white").performClick();
            });
            screenshot("color_white_detail");
            ui(()->{
                activity.onBackPressed();view("nav_favorites").performClick();
                check(view("entry_color_red")!=null,"Color appears in favorites");
                view("favorite_color_red").performClick();view("nav_explore").performClick();
            });
            ui(()->view("category_insects").performClick());
            ui(()->{
                view("entry_ant").performClick();
                check(((TextView)view("detail_bn")).getText().toString().equals("পিঁপড়া"),"Insect detail in Bangla");
                check(((TextView)view("detail_en")).getText().toString().equals("Ant"),"Insect detail in English");
                view("favorite_ant").performClick();
                check(view("favorite_ant").isSelected(),"Insect favorite toggles");
                view("next_entry").performClick();
                check(((TextView)view("detail_en")).getText().toString().equals("Butterfly"),"Insect detail paging");
                activity.onBackPressed();view("entry_hornet").performClick();
                check(((TextView)view("detail_bn")).getText().toString().equals("ভিমরুল"),"Last insect entry reachable");
            });
            screenshot("insect_detail");
            ui(()->{
                activity.onBackPressed();view("nav_favorites").performClick();
                check(view("entry_ant")!=null,"Insect appears in favorites");
                view("favorite_ant").performClick();view("nav_explore").performClick();
            });
            ui(()->view("category_vegetables").performClick());
            ui(()->{
                view("entry_spine_gourd").performClick();
                check(((TextView)view("detail_bn")).getText().toString().equals("কাঁকরোল"),"Replacement vegetable detail in Bangla");
                check(((TextView)view("detail_en")).getText().toString().equals("Spine gourd"),"Replacement vegetable detail in English");
            });
            screenshot("spine_gourd_detail");
            ui(()->{activity.onBackPressed();view("nav_explore").performClick();});
            ui(()->view("category_vehicles").performClick());
            ui(()->{
                view("entry_car").performClick();
                check(((TextView)view("detail_bn")).getText().toString().equals("গাড়ি"),"Vehicle detail in Bangla");
                check(((TextView)view("detail_en")).getText().toString().equals("Car"),"Vehicle detail in English");
                view("favorite_car").performClick();
                check(view("favorite_car").isSelected(),"Vehicle favorite toggles");
                view("next_entry").performClick();
                check(((TextView)view("detail_en")).getText().toString().equals("Bus"),"Vehicle detail paging");
                activity.onBackPressed();view("nav_favorites").performClick();
                check(view("entry_car")!=null,"Vehicle appears in favorites");
                view("favorite_car").performClick();
                view("nav_explore").performClick();
            });
            ui(()->view("category_fish").performClick());
            ui(()->view("entry_snakehead").performClick());
            ui(()->checkEnglishLabel((TextView)view("detail_en"),18));
            screenshot("long_english_name");
            ui(()->activity.onBackPressed());
            ui(()->view("nav_explore").performClick());
            ui(()->{
                check(view("category_animals")!=null,"Home categories rendered");
                EditText search=(EditText)view("search");search.setText("MANGO");check(view("entry_mango")!=null,"English search visible");
                search.setText("মাগুর");check(view("entry_walking_catfish")!=null,"Bangla search visible");
                search.setText("কমলা");check(view("entry_orange")!=null&&view("entry_color_orange")!=null,"Orange fruit and color visible together");
                search.setText("কালোজিরা");check(view("entry_nigella")!=null,"Nigella visible in Bangla search");
                search.setText("Coriander seeds");check(view("entry_coriander_seed")!=null,"Coriander visible in English search");
                search.setText("nonexistent-name");check(countEntryCards(view("results"))==0,"Empty search state");
                search.setText("");view("category_animals").performClick();
                check(countEntryCards(view("results"))==20,"Category lists all 20 pictures");
            });
            screenshot("animals");
            ui(()->{
                view("entry_tiger").performClick();check(((TextView)view("detail_bn")).getText().toString().equals("বাঘ"),"Bangla detail");
                check(((TextView)view("detail_en")).getText().toString().equals("Tiger"),"English detail");
                view("favorite_tiger").performClick();check(view("favorite_tiger").isSelected(),"Favorite toggles");
            });
            screenshot("detail");
            ui(()->checkEnglishLabel((TextView)view("detail_en"),18));
            ui(()->{
                view("next_entry").performClick();check(((TextView)view("detail_en")).getText().toString().equals("Elephant"),"Next entry");
                view("previous_entry").performClick();check(view("favorite_tiger").isSelected(),"Favorite retained through paging");
                activity.onBackPressed();view("nav_favorites").performClick();check(view("entry_tiger")!=null,"Favorite collection");
            });
            screenshot("favorites");
            ui(()->activity.finish());waitForIdleSync();launch();
            ui(()->{
                view("nav_favorites").performClick();check(view("entry_tiger")!=null,"Favorite survives activity restart");
                view("favorite_tiger").performClick();check(countEntryCards(view("results"))==0,"Remove last favorite updates empty state");
                view("nav_quiz").performClick();view("start_quiz").performClick();
            });
            screenshot("quiz");
            ui(()->checkEnglishLabels(activity.getWindow().getDecorView()));
            Instrumentation.ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
            ui(()->activity.recreate());
            Activity recreated=monitor.waitForActivityWithTimeout(5000);removeMonitor(monitor);
            check(recreated!=null,"Activity recreated");activity=recreated;waitForIdleSync();
            ui(()->check(view("answer_0")!=null,"Quiz restored after activity recreation"));
            for(int i=0;i<5;i++)ui(()->{
                check(view("answer_0")!=null,"Quiz choices available");view("answer_0").performClick();
                check(view("quiz_feedback")!=null,"Quiz gives answer feedback");
                check(view("next_question")!=null,"Quiz can advance");view("next_question").performClick();
            });
            ui(()->check(view("quiz_score")!=null,"Five questions reach results"));
            screenshot("result");
            ui(()->{
                view("play_again").performClick();check(view("answer_0")!=null,"Quiz replay");
                view("nav_explore").performClick();check(view("category_fish")!=null,"Return home");
            });
            result.putString("stream","\nPASS: "+checks+" offline content and UI checks.\n");
            result.putInt("checks",checks);finish(Activity.RESULT_OK,result);
        } catch(Throwable e) {
            try{screenshot("failure");}catch(Exception ignored){}
            result.putString("stream","\nFAIL after "+checks+" checks: "+android.util.Log.getStackTraceString(e));
            finish(Activity.RESULT_CANCELED,result);
        }
    }
    private void screenshot(String name)throws Exception {
        if(!capture)return;
        waitForIdleSync();
        // Let the compositor present the new screen after programmatic navigation.
        android.os.SystemClock.sleep(350);
        android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();
        check(bitmap!=null,"Screenshot "+name);
        java.io.File folder=new java.io.File(getTargetContext().getExternalFilesDir(null),"previews");folder.mkdirs();
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,previewPrefix+name+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
        bitmap.recycle();
    }
    private void launch(){
        Intent intent=new Intent(getTargetContext(),MainActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity=startActivitySync(intent);waitForIdleSync();
    }
    private View view(String tag){return activity.getWindow().getDecorView().findViewWithTag(tag);}
    private void checkFavoriteLayout(String id) {
        View picture=view("picture_"+id),heart=view("favorite_"+id);
        TextView name=(TextView)view("name_"+id);
        int[] picturePos=new int[2],heartPos=new int[2],namePos=new int[2];
        picture.getLocationOnScreen(picturePos);heart.getLocationOnScreen(heartPos);name.getLocationOnScreen(namePos);
        check(heartPos[1]>=picturePos[1]+picture.getHeight(),"Favorite never overlaps image: "+id);
        check(heartPos[0]>=namePos[0]+name.getWidth(),"Favorite right of name: "+id);
        int target=Math.round(48*heart.getResources().getDisplayMetrics().density);
        check(heart.getWidth()>=target&&heart.getHeight()>=target,"48dp favorite touch target: "+id);
        View listen=view("listen_"+id);
        int audioHeight=Math.round(36*heart.getResources().getDisplayMetrics().density);
        check(listen!=null&&listen.getHeight()>=audioHeight,"Compact pronunciation control: "+id);
        check(!listen.isSoundEffectsEnabled(),"Pronunciation button has no extra tap click: "+id);
        View englishListen=view("listen_en_"+id);
        check(englishListen!=null&&englishListen.getWidth()>=target&&englishListen.getHeight()>=audioHeight,"Compact English audio control: "+id);
        check(!englishListen.isSoundEffectsEnabled(),"English audio has no tap click: "+id);
        checkListenLabel((ViewGroup)listen);checkListenLabel((ViewGroup)englishListen);
        int[] listenPos=new int[2];listen.getLocationOnScreen(listenPos);
        View english=view("english_entry_"+id);int[] englishPos=new int[2];english.getLocationOnScreen(englishPos);
        check(listenPos[1]>=englishPos[1]+english.getHeight(),"Audio does not squeeze or overlap either name: "+id);
        check(name.getLayout()!=null&&name.getLayout().getHeight()<=name.getHeight()-name.getCompoundPaddingTop()-name.getCompoundPaddingBottom(),"Complete Bangla label height: "+id);
        for(int line=0;line<name.getLineCount();line++) {
            check(name.getLayout().getEllipsisCount(line)==0,"No Bangla ellipsis: "+id);
            // LineMax excludes trailing spaces at a word wrap; those do not clip visible text.
            check(name.getLayout().getLineMax(line)<=name.getWidth()-name.getCompoundPaddingLeft()-name.getCompoundPaddingRight()+1,"Bangla name fits beside favorite: "+id+" line width="+name.getLayout().getLineMax(line)+" available="+name.getWidth());
        }
    }
    private void checkEnglishLabels(View v) {
        if(v instanceof TextView&&v.getTag()!=null&&v.getTag().toString().startsWith("english_"))
            checkEnglishLabel((TextView)v,14);
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)checkEnglishLabels(((ViewGroup)v).getChildAt(i));
    }
    private NameAudioPlayer nameAudio() throws Exception {
        java.lang.reflect.Field field=MainActivity.class.getDeclaredField("nameAudio");field.setAccessible(true);
        return (NameAudioPlayer)field.get(activity);
    }
    private void checkListenLabel(ViewGroup button) {
        for(int i=0;i<button.getChildCount();i++) {
            View child=button.getChildAt(i);
            check(child.getRight()<=button.getWidth()-button.getPaddingRight(),"Audio control content fits its button");
            if(child instanceof TextView) {
                TextView label=(TextView)child;
                check(label.getLineCount()==1&&label.getPaint().measureText(label.getText().toString())<=label.getWidth()+1,"Audio language label fits fully");
            }
        }
    }
    private void awaitAudio(NameAudioPlayer audio,boolean playing) {
        long deadline=android.os.SystemClock.uptimeMillis()+5000;
        java.util.concurrent.atomic.AtomicBoolean matched=new java.util.concurrent.atomic.AtomicBoolean();
        do {
            ui(()->matched.set(audio.isPlaying()==playing));
            if(matched.get())return;
            android.os.SystemClock.sleep(30);
        } while(android.os.SystemClock.uptimeMillis()<deadline);
        check(false,playing?"Bundled pronunciation starts":"Pronunciation finishes");
    }
    private void checkNameAudio() throws Exception {
        NameAudioPlayer audio=nameAudio();
        ui(()->{view("category_animals").performClick();view("listen_tiger").performClick();});
        awaitAudio(audio,true);
        ui(()->{
            check(view("detail_bn")==null,"Audio button does not open detail");
            check(view("listen_tiger").isSelected(),"Playing button selected");
            view("listen_en_tiger").performClick();
            check(!view("listen_tiger").isSelected()&&audio.isActive("tiger","en"),"English replaces Bangla for same entry");
        });
        awaitAudio(audio,true);
        ui(()->{
            check(view("listen_en_tiger").isSelected(),"English playback selected");
            view("listen_elephant").performClick();
            check(!view("listen_en_tiger").isSelected()&&audio.isActive("elephant","bn"),"Bangla replaces English without overlapping audio");
        });
        awaitAudio(audio,true);
        ui(()->{view("listen_elephant").performClick();check(!audio.isActive("elephant","bn"),"Second tap stops audio");});
        ui(()->view("listen_tiger").performClick());awaitAudio(audio,true);awaitAudio(audio,false);
        ui(()->check(!view("listen_tiger").isSelected(),"Completion resets button"));
        ui(()->{
            view("entry_tiger").performClick();
            check(!view("listen_tiger").isSoundEffectsEnabled(),"Detail pronunciation has no extra tap click");
            check(!view("listen_en_tiger").isSoundEffectsEnabled(),"English detail audio has no extra tap click");
            view("listen_en_tiger").performClick();
        });
        awaitAudio(audio,true);
        ui(()->view("listen_en_tiger").performClick());
        ui(()->check(!audio.isActive("tiger","en"),"Second tap stops English audio"));
        ui(()->view("listen_en_tiger").performClick());awaitAudio(audio,true);awaitAudio(audio,false);
        ui(()->check(!view("listen_en_tiger").isSelected(),"English completion resets control"));
        ui(()->view("listen_en_tiger").performClick());awaitAudio(audio,true);
        ui(()->{view("next_entry").performClick();check(!audio.isPlaying(),"Detail paging stops audio");view("listen_elephant").performClick();});
        awaitAudio(audio,true);
        ui(()->activity.finish());waitForIdleSync();
        long stopDeadline=android.os.SystemClock.uptimeMillis()+3000;
        while(!activity.isDestroyed()&&android.os.SystemClock.uptimeMillis()<stopDeadline)android.os.SystemClock.sleep(30);
        check(activity.isDestroyed(),"Activity completes finish lifecycle");
        ui(()->check(!audio.isPlaying()&&!audio.isActive("elephant","bn"),"Leaving activity releases audio"));
        launch();
        java.util.concurrent.atomic.AtomicInteger errors=new java.util.concurrent.atomic.AtomicInteger();
        NameAudioPlayer current=nameAudio();
        ui(()->{
            current.play("missing_test_audio","en",()->{},errors::incrementAndGet);
            check(errors.get()==1&&!current.isActive("missing_test_audio","en"),"Missing audio handled without a crash");
        });
    }
    private void checkHomeContent(View v) {
        if(v instanceof TextView) {
            String value=((TextView)v).getText().toString();
            check(!value.contains("অফলাইন")&&!value.toLowerCase(java.util.Locale.ROOT).contains("offline"),"Home has no offline label");
        }
        String description=String.valueOf(v.getContentDescription());
        check(!description.contains("About this app")&&!description.contains("Discover animals"),"Home has no info button or featured animal banner");
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)checkHomeContent(((ViewGroup)v).getChildAt(i));
    }
    private void checkEnglishLabel(TextView v,int minimumSp) {
        String name=v.getText().toString();
        check(v.getLayout()!=null&&v.getLineCount()==1,"Single-line English: "+name);
        check(v.getLayout().getEllipsisCount(0)==0,"No ellipsis: "+name);
        int available=v.getWidth()-v.getCompoundPaddingLeft()-v.getCompoundPaddingRight();
        check(v.getPaint().measureText(name)<=available+1,"Complete English name fits: "+name);
        float minimum=android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,minimumSp,v.getResources().getDisplayMetrics());
        check(v.getTextSize()>=minimum-1,"Larger readable English: "+name);
    }
    private int countEntryCards(View v){
        int count=v.getTag()!=null&&v.getTag().toString().startsWith("entry_")?1:0;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)count+=countEntryCards(((ViewGroup)v).getChildAt(i));return count;
    }
    private void ui(Runnable action){
        AtomicReference<Throwable> failure=new AtomicReference<>();runOnMainSync(()->{try{action.run();}catch(Throwable e){failure.set(e);}});
        waitForIdleSync();if(failure.get()!=null)throw new AssertionError("UI check failed",failure.get());
    }
    private void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
}
