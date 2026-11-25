package com.amarbangladesh.names;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final int BG=0xFFF9F8F2, INK=0xFF233C32, GREEN=0xFF24755C,
            MUTED=0xFF778176, LIGHT=0xFFE7EDE1, LINE=0xFFE3E6DB, WHITE=0xFFFFFEF9;
    private Catalog catalog;
    private NameAudioPlayer nameAudio;
    private SharedPreferences prefs;
    private Set<String> favorites,seen;
    private Typeface bangla,banglaBold;
    private LinearLayout root,body,results,nav;
    private String route="explore",category="",query="",detail="",previousRoute="explore";
    private final List<String> quizIds=new ArrayList<>(), optionIds=new ArrayList<>();
    private int question=0,score=0;
    private String selected="";
    private boolean quizActive=false,quizFinished=false;
    private int savedScroll=0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(android.os.Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        if(android.os.Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::handleBack);
        catalog=new Catalog(this);nameAudio=new NameAudioPlayer(this);
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        prefs=getSharedPreferences("collection",MODE_PRIVATE);
        favorites=new HashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));
        seen=new HashSet<>(prefs.getStringSet("seen",Collections.emptySet()));
        Set<String> entryIds=new HashSet<>(),categoryIds=new HashSet<>();
        for(Catalog.Entry entry:catalog.entries)entryIds.add(entry.id);
        for(Catalog.Category group:catalog.categories)categoryIds.add(group.id);
        boolean favoritesChanged=favorites.retainAll(entryIds),seenChanged=seen.retainAll(entryIds);
        if(favoritesChanged||seenChanged)prefs.edit().putStringSet("favorites",new HashSet<>(favorites)).putStringSet("seen",new HashSet<>(seen)).apply();
        bangla=Typeface.createFromAsset(getAssets(),"fonts/bangla.ttf");
        banglaBold=Typeface.createFromAsset(getAssets(),"fonts/bangla-bold.ttf");
        if(state!=null) {
            route=state.getString("route","explore");category=state.getString("category","");
            query=state.getString("query","");detail=state.getString("detail","");
            previousRoute=state.getString("previousRoute","explore");
            ArrayList<String> ids=state.getStringArrayList("quizIds");if(ids!=null)quizIds.addAll(ids);
            ids=state.getStringArrayList("optionIds");if(ids!=null)optionIds.addAll(ids);
            question=state.getInt("question");score=state.getInt("score");selected=state.getString("selected","");
            quizActive=state.getBoolean("quizActive");quizFinished=state.getBoolean("quizFinished");
        }
        // A content update can retire the category, detail or quiz saved by Android.
        if(!category.isEmpty()&&!categoryIds.contains(category)){category="";query="";}
        if(!detail.isEmpty()&&!entryIds.contains(detail)){detail="";route="explore";}
        if(!entryIds.containsAll(quizIds)||!entryIds.containsAll(optionIds)) {
            quizIds.clear();optionIds.clear();selected="";question=0;score=0;
            quizActive=false;quizFinished=false;
        }
        render();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("route",route);out.putString("category",category);out.putString("query",query);
        out.putString("detail",detail);out.putString("previousRoute",previousRoute);
        out.putStringArrayList("quizIds",new ArrayList<>(quizIds));out.putStringArrayList("optionIds",new ArrayList<>(optionIds));
        out.putInt("question",question);out.putInt("score",score);out.putString("selected",selected);
        out.putBoolean("quizActive",quizActive);out.putBoolean("quizFinished",quizFinished);
    }
    @Override public void onConfigurationChanged(Configuration c) {super.onConfigurationChanged(c);render();}
    @Override protected void onPause() {if(nameAudio!=null)nameAudio.stop();super.onPause();}
    @Override protected void onStop() {if(nameAudio!=null)nameAudio.stop();super.onStop();}
    @Override protected void onDestroy() {if(nameAudio!=null)nameAudio.stop();super.onDestroy();}
    int dp(float n) {return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,int radius) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private GradientDrawable outlined(int color,int radius,int stroke) {
        GradientDrawable d=shape(color,radius);d.setStroke(dp(1),stroke);return d;
    }
    private LinearLayout column() {LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row() {LinearLayout v=new LinearLayout(this);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView text(String value,int size,int color,boolean bold) {
        TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);
        v.setTypeface(containsBangla(value)?(bold?banglaBold:bangla):Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        v.setIncludeFontPadding(false);v.setLineSpacing(dp(3),1);return v;
    }
    private boolean containsBangla(String s) {for(char c:s.toCharArray())if(c>='\u0980'&&c<='\u09ff')return true;return false;}
    private TextView englishLabel(String value,int maxSp,int minSp) {
        TextView v=text(value,maxSp,0xFF58685D,false);
        v.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        // Max lines (rather than horizontal scrolling) lets platform auto-size fit the whole name.
        v.setMaxLines(1);v.setHorizontallyScrolling(false);
        v.setAutoSizeTextTypeUniformWithConfiguration(minSp,maxSp,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        v.setMinHeight(dp(maxSp+6));return v;
    }
    private void pad(View v,int l,int t,int r,int b) {v.setPadding(dp(l),dp(t),dp(r),dp(b));}
    private void add(LinearLayout p,View v,int width,int height) {p.addView(v,new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height)));}
    private void gap(LinearLayout p,int height) {add(p,new View(this),1,height);}
    private void weighted(LinearLayout p,View v) {p.addView(v,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));}
    private void clickable(View v,Runnable action) {
        v.setSoundEffectsEnabled(false);
        v.setClickable(true);v.setFocusable(true);
        android.util.TypedValue tv=new android.util.TypedValue();getTheme().resolveAttribute(android.R.attr.selectableItemBackground,tv,true);
        v.setForeground(getDrawable(tv.resourceId));v.setOnClickListener(w->action.run());
    }
    private View iconButton(String icon,String description,Runnable action) {
        FrameLayout holder=new FrameLayout(this);holder.setBackground(outlined(WHITE,16,LINE));
        IconView v=new IconView(this,icon,INK,false);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(21),dp(21),Gravity.CENTER);holder.addView(v,p);
        holder.setContentDescription(description);clickable(holder,action);return holder;
    }
    private TextView button(String label,boolean primary,Runnable action) {
        TextView v=text(label,16,primary?WHITE:GREEN,true);v.setGravity(Gravity.CENTER);pad(v,16,15,16,15);
        v.setMinHeight(dp(52));v.setBackground(primary?shape(GREEN,17):outlined(WHITE,17,LINE));clickable(v,action);return v;
    }
    private void hideKeyboard() {
        View focused=getCurrentFocus();if(focused!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focused.getWindowToken(),0);
    }
    private void go(String target) {
        hideKeyboard();route=target;category="";query="";savedScroll=0;render();
    }
    private void render() {
        if(nameAudio!=null)nameAudio.stop();
        root=column();root.setBackgroundColor(BG);root.setTag("root");
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
                v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);root.requestApplyInsets();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);scroll.setTag("scroll");
        body=column();pad(body,18,18,18,24);
        // Center the readable content on tablets while retaining a full-width navigation bar.
        FrameLayout frame=new FrameLayout(this);FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(Math.min(dp(700),getResources().getDisplayMetrics().widthPixels),-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        frame.addView(body,bp);scroll.addView(frame);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(route.equals("detail")) showDetail();
        else if(route.equals("quiz")) showQuiz();
        else showExplore();
        if(!route.equals("detail")) bottomNav();
        if(savedScroll>0&&!route.equals("detail")){int y=savedScroll;scroll.post(()->scroll.scrollTo(0,y));}
    }
    private void brandHeader() {
        LinearLayout header=row();
        FrameLayout logo=new FrameLayout(this);logo.setBackground(shape(GREEN,13));
        logo.addView(new IconView(this,"leaf",0xFFF1D49C,false),new FrameLayout.LayoutParams(dp(25),dp(25),Gravity.CENTER));
        add(header,logo,44,44);View space=new View(this);add(header,space,11,1);
        LinearLayout titles=column();add(titles,text(getString(R.string.app_name),19,INK,true),-1,-2);gap(titles,4);
        TextView eyebrow=englishLabel("A world to discover",13,11);add(titles,eyebrow,-1,-2);weighted(header,titles);
        add(body,header,-1,-2);gap(body,24);
    }
    private void showExplore() {
        brandHeader();
        boolean saved=route.equals("favorites");
        if(saved) {
            add(body,text("পছন্দের সংগ্রহ",30,INK,true),-1,-2);gap(body,7);
            add(body,englishLabel("Your favorite discoveries",16,14),-1,-2);gap(body,22);
        } else if(category.isEmpty()) {
            add(body,text("ছবিতে ছবিতে শিখি",28,INK,true),-1,-2);gap(body,7);
            add(body,text(bn(catalog.categories.size())+"টি বিভাগ · "+bn(catalog.entries.size())+"টি নাম · বাংলা ও English",13,MUTED,false),-1,-2);gap(body,20);
        } else {
            Catalog.Category cat=catalog.category(category);
            LinearLayout head=row();add(head,iconButton("back","সব বিভাগ · All categories",()->{category="";query="";savedScroll=0;render();}),44,44);add(head,new View(this),13,1);
            LinearLayout title=column();add(title,text(cat.bn,30,INK,true),-1,-2);gap(title,4);add(title,englishLabel(cat.en+" · "+catalog.filter(category,"").size()+" names",18,14),-1,-2);weighted(head,title);
            add(body,head,-1,-2);gap(body,23);
        }
        searchBox();gap(body,22);
        results=column();results.setTag("results");add(body,results,-1,-2);updateResults();
    }
    private void searchBox() {
        LinearLayout box=row();pad(box,15,0,6,0);box.setBackground(outlined(WHITE,17,LINE));
        add(box,new IconView(this,"search",MUTED,false),21,21);add(box,new View(this),9,1);
        EditText search=new EditText(this);search.setTag("search");search.setTextSize(14);search.setTextColor(INK);search.setHintTextColor(MUTED);
        search.setSingleLine();search.setHint("বাংলা বা English নাম খুঁজি…");search.setTypeface(bangla);search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(dp(3),0,dp(3),0);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);search.setContentDescription("বাংলা বা English নাম খুঁজুন · Search names");
        box.addView(search,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView clear=text("×",26,MUTED,false);clear.setGravity(Gravity.CENTER);clear.setContentDescription("খোঁজা মুছুন · Clear search");
        add(box,clear,44,48);clear.setVisibility(query.isEmpty()?View.GONE:View.VISIBLE);clickable(clear,()->search.setText(""));
        search.setText(query);search.setSelection(search.length());
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString();clear.setVisibility(query.isEmpty()?View.GONE:View.VISIBLE);updateResults();}
            public void afterTextChanged(Editable s){}
        });
        search.setOnEditorActionListener((v,action,event)->{hideKeyboard();return true;});
        add(body,box,-1,56);
    }
    private void updateResults() {
        if(nameAudio!=null)nameAudio.stop();
        if(results==null)return;results.removeAllViews();boolean saved=route.equals("favorites");
        if(category.isEmpty()&&query.trim().isEmpty()&&!saved) {homeContent();return;}
        List<Catalog.Entry> list=catalog.filter(category,query);
        if(saved)list.removeIf(e->!favorites.contains(e.id));
        LinearLayout count=row();weighted(count,text(saved?"সংরক্ষিত নাম":"ছবিতে পরিচয়",17,INK,true));add(count,text(bn(list.size())+"টি",12,MUTED,false),-2,-2);add(results,count,-1,-2);gap(results,16);
        if(list.isEmpty()) {
            LinearLayout empty=column();pad(empty,22,35,22,35);empty.setGravity(Gravity.CENTER);empty.setBackground(shape(LIGHT,23));
            add(empty,new IconView(this,saved&&query.isEmpty()?"heart":"search",GREEN,false),42,42);gap(empty,20);
            TextView title=text(saved&&query.isEmpty()?"পছন্দের নাম জমাই":"কোনো নাম পাওয়া যায়নি",21,INK,true);title.setGravity(Gravity.CENTER);add(empty,title,-1,-2);gap(empty,11);
            TextView note=text(saved&&query.isEmpty()?"নামের পাশে হৃদয় চিহ্নে চাপ দিয়ে\nতোমার সংগ্রহে যোগ করো।":"অন্য একটি বাংলা বা English নাম দিয়ে খোঁজো।",14,MUTED,false);note.setGravity(Gravity.CENTER);add(empty,note,-1,-2);gap(empty,22);
            add(empty,button(saved&&query.isEmpty()?"ছবি দেখি · Explore":"খোঁজা মুছি · Clear search",true,()->{if(saved&&query.isEmpty())go("explore");else{EditText search=root.findViewWithTag("search");search.setText("");}}),-1,-2);
            add(results,empty,-1,-2);
        } else entryGrid(results,list);
    }
    private int gridColWidthPx() {
        int bodyW=Math.min(dp(700),getResources().getDisplayMetrics().widthPixels)-dp(36);
        return Math.max(dp(120),(bodyW-dp(14))/2);
    }
    private int entryColumns() {
        android.graphics.Paint paint=new android.graphics.Paint();
        paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        paint.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,14,getResources().getDisplayMetrics()));
        float widest=0;for(Catalog.Entry e:catalog.entries)widest=Math.max(widest,paint.measureText(e.en));
        // Switch to a wider single column on compact screens or with larger accessibility text.
        return gridColWidthPx()>=Math.ceil(widest)+dp(30)?2:1;
    }
    private void homeContent() {
        LinearLayout section=row();weighted(section,text("কী চিনতে চাও?",21,INK,true));
        add(results,section,-1,-2);gap(results,15);
        int catArtH=Math.min(dp(180),Math.round(gridColWidthPx()*0.78f));
        for(int i=0;i<catalog.categories.size();i+=2) {
            LinearLayout line=row();line.setGravity(Gravity.TOP);
            for(int j=i;j<Math.min(i+2,catalog.categories.size());j++) {
                Catalog.Category cat=catalog.categories.get(j);LinearLayout card=column();card.setBackground(shape(WHITE,21));card.setClipToOutline(true);
                ImageView cover=image(catalog.entry(cat.cover));
                card.addView(cover,new LinearLayout.LayoutParams(-1,catArtH));
                LinearLayout labels=column();pad(labels,12,11,12,13);
                TextView name=text(cat.bn,20,INK,true);name.setGravity(Gravity.CENTER);name.setTag("category_name_"+cat.id);add(labels,name,-1,-2);gap(labels,5);
                TextView english=englishLabel(cat.en,16,14);english.setTag("english_category_"+cat.id);english.setGravity(Gravity.CENTER);add(labels,english,-1,-2);
                String count=bn(catalog.filter(cat.id,"").size())+"টি নাম";
                add(card,labels,-1,-2);
                card.setTag("category_"+cat.id);card.setContentDescription(cat.bn+", "+cat.en+", "+count);clickable(card,()->openCategory(cat.id));
                line.addView(card,new LinearLayout.LayoutParams(0,-2,1));if(j==i)add(line,new View(this),14,1);
            }
            if(i+1==catalog.categories.size())line.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
            add(results,line,-1,-2);gap(results,14);
        }
        gap(results,10);LinearLayout quiz=row();pad(quiz,17,19,17,19);quiz.setBackground(shape(0xFFF3E9D5,20));
        add(quiz,new IconView(this,"quiz",0xFFB18A3E,false),28,28);add(quiz,new View(this),13,1);
        LinearLayout qt=column();add(qt,text("কতগুলো চিনতে পারো?",17,INK,true),-1,-2);gap(qt,5);add(qt,englishLabel("Try a picture quiz",15,13),-1,-2);weighted(quiz,qt);add(quiz,new IconView(this,"next",INK,false),20,20);
        clickable(quiz,()->go("quiz"));add(results,quiz,-1,-2);gap(results,24);
        if(!seen.isEmpty()) {gap(results,8);TextView visited=text("দেখা হয়েছে "+bn(seen.size())+" / "+bn(catalog.entries.size())+"টি নাম",11,GREEN,false);visited.setGravity(Gravity.CENTER);add(results,visited,-1,-2);}
    }
    private void openCategory(String id) {hideKeyboard();category=id;query="";savedScroll=0;route="explore";render();}
    private ImageView image(Catalog.Entry entry) {
        ImageView v=new ImageView(this);v.setImageBitmap(catalog.picture(entry));v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setBackgroundColor(0xFFFFFAF0);
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return v;
    }
    private void entryGrid(LinearLayout parent,List<Catalog.Entry> entries) {
        int columns=entryColumns();
        android.graphics.Paint namePaint=new android.graphics.Paint();
        namePaint.setTypeface(banglaBold);
        namePaint.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,19,getResources().getDisplayMetrics()));
        float widestWord=0;
        for(Catalog.Entry entry:catalog.entries)for(String word:entry.bn.split("\\s+"))widestWord=Math.max(widestWord,namePaint.measureText(word));
        // Keep each Bangla word intact beside the 48dp button, including at larger text sizes.
        if(gridColWidthPx()<Math.ceil(widestWord)+dp(79))columns=1;
        int artSizePx=columns==1?dp(280):Math.min(dp(240),gridColWidthPx());
        for(int i=0;i<entries.size();i+=columns) {
            LinearLayout line=row();line.setGravity(Gravity.TOP);
            for(int j=i;j<Math.min(i+columns,entries.size());j++) {
                Catalog.Entry entry=entries.get(j);LinearLayout card=column();card.setBackground(outlined(WHITE,21,LINE));card.setClipToOutline(true);card.setTag("entry_"+entry.id);
                ImageView art=image(entry);art.setTag("picture_"+entry.id);
                card.addView(art,new LinearLayout.LayoutParams(-1,artSizePx));
                LinearLayout labels=column();pad(labels,13,2,10,12);
                LinearLayout nameRow=row();nameRow.setGravity(Gravity.BOTTOM);
                TextView name=text(entry.bn,19,INK,true);name.setTag("name_"+entry.id);weighted(nameRow,name);
                View heart=heart(entry,()->{if(route.equals("favorites"))updateResults();else{ /* replaced in place by heart() */ }});
                // Keep the generous touch area above the icon, not between the two names.
                heart.setPadding(0,dp(16),0,0);
                add(nameRow,new View(this),8,1);add(nameRow,heart,48,48);add(labels,nameRow,-1,-2);gap(labels,2);
                TextView english=englishLabel(entry.en,16,14);english.setTag("english_entry_"+entry.id);add(labels,english,-1,-2);
                gap(labels,8);add(labels,listenControls(entry),-1,-2);add(card,labels,-1,-2);
                card.setContentDescription(entry.bn+", "+entry.en);clickable(card,()->openDetail(entry.id));
                line.addView(card,new LinearLayout.LayoutParams(0,-2,1));if(columns==2&&j==i)add(line,new View(this),14,1);
            }
            if(columns==2&&i+1==entries.size()){View blank=new View(this);line.addView(blank,new LinearLayout.LayoutParams(0,1,1));}
            add(parent,line,-1,-2);gap(parent,14);
        }
    }
    private FrameLayout heart(Catalog.Entry entry,Runnable after) {
        FrameLayout frame=new FrameLayout(this);frame.setBackground(shape(0xF7FFFEF9,24));frame.setTag("favorite_"+entry.id);
        updateHeart(frame,entry);clickable(frame,()->{
            if(favorites.contains(entry.id))favorites.remove(entry.id);else favorites.add(entry.id);
            prefs.edit().putStringSet("favorites",new HashSet<>(favorites)).apply();updateHeart(frame,entry);after.run();
            frame.announceForAccessibility(favorites.contains(entry.id)?"পছন্দে যোগ হয়েছে":"পছন্দ থেকে সরানো হয়েছে");
        });return frame;
    }
    private View listenControls(Catalog.Entry entry) {
        LinearLayout controls=row();
        weighted(controls,listenButton(entry,"bn"));add(controls,new View(this),8,1);
        weighted(controls,listenButton(entry,"en"));return controls;
    }
    private View listenButton(Catalog.Entry entry,String language) {
        LinearLayout control=row();control.setGravity(Gravity.CENTER);pad(control,6,4,6,4);
        control.setMinimumHeight(dp(36));control.setBackground(shape(LIGHT,10));
        control.setTag((language.equals("bn")?"listen_":"listen_en_")+entry.id);
        Runnable reset=()->updateListenButton(control,entry,language,false);
        reset.run();clickable(control,()->{
            if(nameAudio.isActive(entry.id,language)){nameAudio.stop();return;}
            nameAudio.stop();updateListenButton(control,entry,language,true);
            nameAudio.play(entry.id,language,reset,()->android.widget.Toast.makeText(this,
                    language.equals("bn")?"এখন শোনা যাচ্ছে না। আবার চেষ্টা করো।":"Unable to play. Please try again.",
                    android.widget.Toast.LENGTH_SHORT).show());
        });return control;
    }
    private void updateListenButton(LinearLayout control,Catalog.Entry entry,String language,boolean playing) {
        boolean bangla=language.equals("bn");
        control.removeAllViews();control.setSelected(playing);
        add(control,new IconView(this,playing?"stop":"speaker",GREEN,false),18,18);add(control,new View(this),5,1);
        TextView label=text(bangla?(playing?"থামাই":"বাংলা"):(playing?"Stop":"English"),14,GREEN,true);
        label.setMaxLines(1);add(control,label,-2,-2);
        control.setContentDescription(bangla?
                (playing?"থামাই · Stop Bangla: ":"বাংলা নাম শুনি · Listen in Bangla: ")+entry.bn:
                (playing?"Stop English: ":"Listen in English: ")+entry.en);
    }
    private void updateHeart(FrameLayout frame,Catalog.Entry entry) {
        frame.removeAllViews();boolean saved=favorites.contains(entry.id);
        frame.addView(new IconView(this,"heart",saved?0xFFC16958:GREEN,saved),new FrameLayout.LayoutParams(dp(21),dp(21),Gravity.CENTER));
        frame.setContentDescription((saved?"পছন্দ থেকে সরাই · Remove favorite ":"পছন্দে রাখি · Save favorite ")+entry.bn);frame.setSelected(saved);
    }
    private void openDetail(String id) {
        hideKeyboard();previousRoute=route;ScrollView scroll=root.findViewWithTag("scroll");savedScroll=scroll.getScrollY();
        detail=id;route="detail";render();scroll=root.findViewWithTag("scroll");scroll.scrollTo(0,0);
    }
    private void showDetail() {
        Catalog.Entry e=catalog.entry(detail);Catalog.Category c=catalog.category(e.category);
        seen.add(e.id);prefs.edit().putStringSet("seen",new HashSet<>(seen)).apply();
        LinearLayout head=row();add(head,iconButton("back","ফিরে যাই · Back",this::leaveDetail),44,44);
        TextView categoryTitle=text(c.bn+" / "+c.en,14,GREEN,true);categoryTitle.setGravity(Gravity.CENTER);weighted(head,categoryTitle);
        add(head,heart(e,()->{}),44,44);add(body,head,-1,-2);gap(body,25);
        ImageView art=image(e);art.setBackground(shape(Color.parseColor(c.color),24));art.setClipToOutline(true);art.setContentDescription("ছবি: "+e.bn+" · "+e.en);art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        int widthPx=Math.min(getResources().getDisplayMetrics().widthPixels-dp(36),dp(520));
        int sizePx=Math.min(dp(440),widthPx);
        LinearLayout.LayoutParams artParams=new LinearLayout.LayoutParams(sizePx,sizePx);artParams.gravity=Gravity.CENTER_HORIZONTAL;
        body.addView(art,artParams);gap(body,26);
        TextView small=englishLabel("Picture dictionary",14,12);small.setTextColor(GREEN);small.setGravity(Gravity.CENTER);add(body,small,-1,-2);gap(body,13);
        TextView banglaName=text(e.bn,38,INK,true);banglaName.setGravity(Gravity.CENTER);banglaName.setTag("detail_bn");add(body,banglaName,-1,-2);gap(body,9);
        TextView englishName=englishLabel(e.en,28,18);englishName.setGravity(Gravity.CENTER);englishName.setTag("detail_en");add(body,englishName,-1,-2);gap(body,16);
        add(body,listenControls(e),-1,-2);gap(body,24);
        List<Catalog.Entry> siblings=catalog.filter(e.category,"");int index=siblings.indexOf(e);
        LinearLayout pager=row();TextView prev=button("←  আগেরটি",false,()->detailMove(siblings,index-1));prev.setTag("previous_entry");
        prev.setEnabled(index>0);prev.setAlpha(index>0?1:.4f);pager.addView(prev,new LinearLayout.LayoutParams(0,-2,1));add(pager,new View(this),14,1);
        TextView next=button("পরেরটি  →",true,()->detailMove(siblings,index+1));next.setTag("next_entry");next.setEnabled(index<siblings.size()-1);next.setAlpha(index<siblings.size()-1?1:.4f);
        pager.addView(next,new LinearLayout.LayoutParams(0,-2,1));add(body,pager,-1,-2);gap(body,17);
        TextView position=text(bn(index+1)+" / "+bn(siblings.size())+"  ·  "+c.bn,12,MUTED,false);position.setGravity(Gravity.CENTER);add(body,position,-1,-2);
        if(e.id.equals("boal")) {
            gap(body,24);
        TextView imageCredit=text("",12,MUTED,false);
        imageCredit.setText(android.text.Html.fromHtml("Boal image: AI redrawing based on <a href=\"https://commons.wikimedia.org/wiki/File:Wallago_attu_1.jpg\">Wallago attu 1.jpg</a> by Reezwan. Original and adapted image: <a href=\"https://creativecommons.org/licenses/by-sa/3.0/\">CC BY-SA 3.0</a>."));
        imageCredit.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        add(body,imageCredit,-1,-2);
        }
    }
    private void detailMove(List<Catalog.Entry> entries,int i) {
        if(i>=0&&i<entries.size()){detail=entries.get(i).id;int old=savedScroll;savedScroll=0;render();savedScroll=old;}
    }
    private void leaveDetail() {route=previousRoute;render();}
    private void bottomNav() {
        nav=row();nav.setBackgroundColor(BG);pad(nav,17,10,17,10);View divider=new View(this);divider.setBackgroundColor(LINE);add(root,divider,-1,1);
        String[][] tabs={{"explore","grid","ঘুরে দেখি","Explore"},{"favorites","heart","পছন্দের","Favorites"},{"quiz","quiz","খেলা করি","Quiz"}};
        for(String[] tab:tabs) {
            boolean active=route.equals(tab[0]);LinearLayout item=column();item.setGravity(Gravity.CENTER);pad(item,3,8,3,8);
            if(active)item.setBackground(shape(LIGHT,18));add(item,new IconView(this,tab[1],active?GREEN:MUTED,active&&tab[1].equals("heart")),22,22);gap(item,6);
            TextView label=text(tab[2],11,active?GREEN:MUTED,active);add(item,label,-2,-2);
            item.setTag("nav_"+tab[0]);item.setContentDescription(tab[2]+" · "+tab[3]);item.setSelected(active);clickable(item,()->go(tab[0]));
            item.setMinimumHeight(dp(66));nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        }
        add(root,nav,-1,-2);
    }
    private void showQuiz() {
        if(!quizActive||quizFinished) {
            brandHeader();add(body,text("ছবি দেখে বলি",30,INK,true),-1,-2);gap(body,8);
            add(body,englishLabel("How many can you recognize?",16,14),-1,-2);gap(body,25);
        } else {
            LinearLayout title=row();weighted(title,text("ছবি দেখে বলি",21,INK,true));TextView subtitle=englishLabel("Picture quiz",14,12);subtitle.setTextColor(GREEN);add(title,subtitle,-2,-2);
            add(body,title,-1,-2);gap(body,17);
        }
        if(quizFinished){quizResult();return;}
        if(!quizActive) {
            LinearLayout card=column();card.setGravity(Gravity.CENTER);pad(card,23,25,23,27);card.setBackground(shape(0xFFF3E9D5,25));
            ImageView art=image(catalog.entry("kingfisher"));art.setBackground(shape(0xFFE5EEDC,26));art.setClipToOutline(true);add(card,art,212,212);gap(card,22);
            TextView title=text("চেনা ছবির ছোট্ট খেলা",23,INK,true);title.setGravity(Gravity.CENTER);add(card,title,-1,-2);gap(card,12);
            TextView note=text("৫টি ছবি, ৪টি করে উত্তর।\nছবি দেখে সঠিক নামটি বেছে নাও।",15,0xFF7C795F,false);note.setGravity(Gravity.CENTER);add(card,note,-1,-2);gap(card,24);
            TextView start=button("শুরু করি · Let's play  →",true,this::startQuiz);start.setTag("start_quiz");add(card,start,-1,-2);add(body,card,-1,-2);gap(body,23);
            TextView best=text("সেরা স্কোর  "+bn(prefs.getInt("best",0))+" / ৫   ·   কোনো সময়সীমা নেই",12,MUTED,false);best.setGravity(Gravity.CENTER);add(body,best,-1,-2);return;
        }
        Catalog.Entry answer=catalog.entry(quizIds.get(question));
        LinearLayout status=row();weighted(status,text("প্রশ্ন "+bn(question+1)+" / ৫",13,GREEN,true));add(status,text("স্কোর "+bn(score),12,MUTED,false),-2,-2);add(body,status,-1,-2);gap(body,12);
        ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(5);progress.setProgress(question+1);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(LINE));add(body,progress,-1,5);gap(body,14);
        ImageView art=image(answer);art.setBackground(shape(Color.parseColor(catalog.category(answer.category).color),24));art.setClipToOutline(true);
        art.setContentDescription("প্রশ্নের ছবি · Identify the pictured "+catalog.category(answer.category).en.toLowerCase(java.util.Locale.ROOT));art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        int screenHeight=Math.round(getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density);
        int qSize=screenHeight<700?176:244;
        LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(dp(qSize),dp(qSize));qp.gravity=Gravity.CENTER_HORIZONTAL;
        body.addView(art,qp);gap(body,15);add(body,text("ছবিটির নাম কী?",20,INK,true),-1,-2);gap(body,13);
        LinearLayout choices=null;int columns=entryColumns();
        for(int i=0;i<optionIds.size();i++) {
            if(i%columns==0){choices=row();choices.setGravity(Gravity.TOP);add(body,choices,-1,-2);gap(body,10);}
            Catalog.Entry option=catalog.entry(optionIds.get(i));boolean correct=option.id.equals(answer.id),chosen=option.id.equals(selected);
            int bg=WHITE,border=LINE;
            if(!selected.isEmpty()&&correct){bg=0xFFE3EEDD;border=GREEN;}
            else if(chosen){bg=0xFFF9E6DE;border=0xFFC77F67;}
            LinearLayout choice=column();pad(choice,13,13,13,11);choice.setMinimumHeight(dp(90));choice.setBackground(outlined(bg,15,border));
            LinearLayout name=row();weighted(name,text(option.bn,17,INK,true));
            if(!selected.isEmpty()&&(correct||chosen))add(name,new IconView(this,correct?"check":"close",correct?GREEN:0xFFC77F67,false),18,18);
            add(choice,name,-1,-2);gap(choice,6);TextView english=englishLabel(option.en,16,14);english.setTag("english_option_"+i);add(choice,english,-1,-2);
            choice.setTag("answer_"+i);choice.setContentDescription(option.bn+", "+option.en);choice.setEnabled(selected.isEmpty());
            if(selected.isEmpty())clickable(choice,()->answer(option.id));choices.addView(choice,new LinearLayout.LayoutParams(0,-2,1));if(columns==2&&i%2==0)add(choices,new View(this),11,1);
        }
        if(!selected.isEmpty()) {
            gap(body,6);TextView feedback=text(selected.equals(answer.id)?"ঠিক বলেছ! খুব ভালো।":"সঠিক নাম: "+answer.bn+" · "+answer.en,14,selected.equals(answer.id)?GREEN:0xFF9E614C,true);
            feedback.setTag("quiz_feedback");feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);add(body,feedback,-1,-2);gap(body,17);
            TextView next=button(question==4?"ফলাফল দেখি  →":"পরের প্রশ্ন  →",true,this::nextQuestion);next.setTag("next_question");add(body,next,-1,-2);
        }
    }
    private void startQuiz() {
        List<Catalog.Entry> shuffled=new ArrayList<>(catalog.entries);Collections.shuffle(shuffled);quizIds.clear();
        for(int i=0;i<5;i++)quizIds.add(shuffled.get(i).id);question=0;score=0;selected="";quizActive=true;quizFinished=false;makeOptions();savedScroll=0;render();
    }
    private void makeOptions() {
        Catalog.Entry answer=catalog.entry(quizIds.get(question));List<Catalog.Entry> pool=catalog.filter(answer.category,"");pool.remove(answer);Collections.shuffle(pool);
        optionIds.clear();optionIds.add(answer.id);for(int i=0;i<3;i++)optionIds.add(pool.get(i).id);Collections.shuffle(optionIds);
    }
    private void answer(String id) {
        if(!selected.isEmpty())return;selected=id;if(id.equals(quizIds.get(question)))score++;
        savedScroll=0;render();ScrollView scroll=root.findViewWithTag("scroll");scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
    }
    private void nextQuestion() {
        if(question==4){quizFinished=true;quizActive=false;prefs.edit().putInt("best",Math.max(score,prefs.getInt("best",0))).apply();}
        else{question++;selected="";makeOptions();}savedScroll=0;render();
    }
    private void quizResult() {
        LinearLayout card=column();pad(card,23,32,23,30);card.setGravity(Gravity.CENTER);card.setBackground(shape(LIGHT,25));
        add(card,new IconView(this,"leaf",GREEN,false),58,58);gap(card,23);
        TextView scoreText=text(bn(score)+" / ৫",53,GREEN,true);scoreText.setTag("quiz_score");add(card,scoreText,-2,-2);gap(card,14);
        TextView heading=text(score>=4?"দারুণ চিনতে পেরেছ!":"শিখতে শিখতেই মজা!",23,INK,true);heading.setGravity(Gravity.CENTER);add(card,heading,-1,-2);gap(card,12);
        TextView note=text("আবার খেলো, আরও ভালো করে চেনো।",14,MUTED,false);note.setGravity(Gravity.CENTER);add(card,note,-1,-2);gap(card,25);
        TextView again=button("আবার খেলি · Play again",true,this::startQuiz);again.setTag("play_again");add(card,again,-1,-2);gap(card,11);
        add(card,button("ছবির খাতায় ফিরি",false,()->go("explore")),-1,-2);add(body,card,-1,-2);
    }
    private String bn(int n) {String s=Integer.toString(n);StringBuilder b=new StringBuilder();for(char c:s.toCharArray())b.append(c>='0'&&c<='9'?(char)('০'+c-'0'):c);return b.toString();}
    // API 33+ uses the native OnBackInvokedDispatcher registered in onCreate.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() {handleBack();}
    private void handleBack() {
        if(route.equals("detail")){leaveDetail();return;}
        if(!query.isEmpty()){EditText search=root.findViewWithTag("search");if(search!=null){search.setText("");hideKeyboard();return;}}
        if(!category.isEmpty()){openCategory("");return;}
        if(!route.equals("explore")){go("explore");return;}
        finish();
    }
}
