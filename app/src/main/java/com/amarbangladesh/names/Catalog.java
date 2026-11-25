package com.amarbangladesh.names;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** All content is loaded from the APK. This class has no network dependencies. */
public final class Catalog {
    public static final class Category {
        public final String id, bn, en, color, cover;
        Category(JSONObject o) throws Exception {
            id=o.getString("id"); bn=o.getString("bn"); en=o.getString("en");
            color=o.getString("color"); cover=o.getString("cover");
        }
    }
    public static final class Entry {
        public final String id, category, bn, en, picture;
        Entry(JSONObject o) throws Exception {
            id=o.getString("id"); category=o.getString("category"); bn=o.getString("bn");
            en=o.getString("en"); picture=o.getString("picture");
        }
    }
    public final List<Category> categories = new ArrayList<>();
    public final List<Entry> entries = new ArrayList<>();
    private final Context context;
    private final LruCache<String,Bitmap> pictures = new LruCache<String,Bitmap>(16*1024*1024) {
        @Override protected int sizeOf(String key,Bitmap value) { return value.getByteCount(); }
    };
    public Catalog(Context context) {
        this.context=context.getApplicationContext();
        try (InputStream in=context.getAssets().open("collection.json")) {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
            while((count=in.read(buffer))!=-1)bytes.write(buffer,0,count);
            JSONObject data=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            JSONArray cats=data.getJSONArray("categories"), items=data.getJSONArray("entries");
            for(int i=0;i<cats.length();i++) categories.add(new Category(cats.getJSONObject(i)));
            for(int i=0;i<items.length();i++) entries.add(new Entry(items.getJSONObject(i)));
        } catch(Exception e) { throw new IllegalStateException("Bundled collection could not be loaded",e); }
    }
    public Category category(String id) {
        for(Category c:categories) if(c.id.equals(id)) return c;
        throw new IllegalArgumentException("Unknown category: "+id);
    }
    public Entry entry(String id) {
        for(Entry e:entries) if(e.id.equals(id)) return e;
        throw new IllegalArgumentException("Unknown entry: "+id);
    }
    public List<Entry> filter(String category,String query) {
        List<Entry> result=new ArrayList<>();
        String q=query.trim().toLowerCase(Locale.ROOT);
        for(Entry e:entries) if((category.isEmpty()||e.category.equals(category))&&
                (e.bn.contains(q)||e.en.toLowerCase(Locale.ROOT).contains(q))) result.add(e);
        return result;
    }
    public Bitmap picture(Entry e) {
        Bitmap b=pictures.get(e.id);
        if(b==null) {
            try(InputStream in=context.getAssets().open(e.picture)) { b=BitmapFactory.decodeStream(in); }
            catch(Exception ex) { throw new IllegalStateException("Missing bundled picture: "+e.id,ex); }
            if(b==null) throw new IllegalStateException("Invalid picture: "+e.id);
            pictures.put(e.id,b);
        }
        return b;
    }
}
