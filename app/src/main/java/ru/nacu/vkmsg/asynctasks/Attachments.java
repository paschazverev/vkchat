package ru.nacu.vkmsg.asynctasks;

import com.perm.kate.api.Attachment;
import com.perm.kate.api.KException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.android.common.db.DatabaseTools;
import ru.nacu.vkmsg.VKMessenger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author quadro
 * @since 7/9/12 11:36 AM
 */
public final class Attachments {

    public final List<Photo> photos;
    public final List<Video> videos;
    public final List<Audio> audios;
    public final List<Forwarded> messages;
    public final List<Document> documents;
    public final Geo geo;


    public static Attachments parse(String s) {
        if (s == null) return null;

        try {
            return new Attachments(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Attachments(String s) throws JSONException {
        this(new JSONObject(s));
    }

    public Attachments(JSONObject o) {
        JSONArray photos = o.optJSONArray("photos");
        if (photos != null && photos.length() != 0) {
            this.photos = new ArrayList<Photo>();
            for (int i = 0; i < photos.length(); i++) {
                this.photos.add(new Photo(photos.optJSONObject(i)));
            }
        } else {
            this.photos = null;
        }

        JSONArray videos = o.optJSONArray("videos");
        if (videos != null && videos.length() != 0) {
            this.videos = new ArrayList<Video>();
            for (int i = 0; i < videos.length(); i++) {
                this.videos.add(new Video(videos.optJSONObject(i)));
            }
        } else {
            this.videos = null;
        }

        JSONArray audios = o.optJSONArray("audios");
        if (audios != null && audios.length() != 0) {
            this.audios = new ArrayList<Audio>();
            for (int i = 0; i < audios.length(); i++) {
                this.audios.add(new Audio(audios.optJSONObject(i)));
            }
        } else {
            this.audios = null;
        }

        JSONArray messages = o.optJSONArray("messages");
        if (messages != null && messages.length() != 0) {
            this.messages = new ArrayList<Forwarded>();
            for (int i = 0; i < messages.length(); i++) {
                this.messages.add(new Forwarded(messages.optJSONObject(i)));
            }
        } else {
            this.messages = null;
        }

        JSONArray documents = o.optJSONArray("documents");
        if (documents != null && documents.length() != 0) {
            this.documents = new ArrayList<Document>();
            for (int i = 0; i < documents.length(); i++) {
                this.documents.add(new Document(documents.optJSONObject(i)));
            }
        } else {
            this.documents = null;
        }

        JSONObject geo = o.optJSONObject("geo");
        if (geo != null) {
            this.geo = new Geo(geo);
        } else {
            this.geo = null;
        }
    }

    public Attachments(List<Photo> photos, List<Video> videos, List<Audio> audios, List<Forwarded> messages, List<Document> docs, Geo geo) {
        this.photos = photos;
        this.videos = videos;
        this.audios = audios;
        this.messages = messages;
        this.documents = docs;
        this.geo = geo;
    }

    public static class Geo {
        public final double lat;
        public final double lon;
        public final String title;

        public String getMapImage(int width, int height) {
            return "http://maps.google.com/maps/api/staticmap?center=" + lat + "," + lon + "&zoom=15&size=" + width + "x" + height + "&sensor=false&markers=color:blue%7Clabel:S%7C" + lat + "," + lon;
        }

        public Geo(double lat, double lon, String title) {
            this.lat = lat;
            this.lon = lon;
            this.title = title;
        }

        public Geo(JSONObject o) {
            lat = Double.parseDouble(o.optString("lat"));
            lon = Double.parseDouble(o.optString("lon"));
            title = o.optString("title", null);
        }

        public JSONObject serialize0() throws JSONException {
            final JSONObject r = new JSONObject();
            r.put("lat", lat + "");
            r.put("lon", lon + "");
            r.put("title", title);
            return r;
        }
    }

    public static class Forwarded {
        public final long id;
        public final long userId;
        public final long date;
        public final String body;
        public final Attachments attachments;

        public Forwarded(JSONObject obj) {
            userId = obj.optLong("userId");
            date = obj.optLong("date");
            body = obj.optString("body", null);
            id = obj.optLong("id", 0);
            if (obj.has("attachments")) {
                attachments = new Attachments(obj.optJSONObject("attachments"));
            } else {
                attachments = null;
            }
        }

        private JSONObject serialize0() throws JSONException {
            final JSONObject o = new JSONObject();
            o.put("userId", userId);
            o.put("date", date);
            o.put("body", body);
            o.put("id", id);
            if (attachments != null) {
                o.put("attachments", attachments.serialize0());
            }
            return o;
        }

        public Forwarded(long id, long userId, long date, String body, Attachments attachments) {
            this.id = id;
            this.userId = userId;
            this.date = date;
            this.body = body;
            this.attachments = attachments;
        }
    }

    public static class Audio {
        public final String artist;
        public final String track;
        public final String url;

        public Audio(String artist, String track, String url) {
            this.artist = artist;
            this.track = track;
            this.url = url;
        }

        public Audio(JSONObject o) {
            artist = o.optString("artist", null);
            track = o.optString("track", null);
            url = o.optString("url", null);
        }

        private JSONObject serialize0() throws JSONException {
            final JSONObject o = new JSONObject();
            o.put("artist", artist);
            o.put("track", track);
            o.put("url", url);
            return o;
        }
    }

    public static class Document {
        public final String title;
        public final String url;

        public Document(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public Document(JSONObject o) {
            title = o.optString("title", null);
            url = o.optString("url", null);
        }

        public JSONObject serialize0() throws JSONException {
            final JSONObject r = new JSONObject();
            r.put("title", title);
            r.put("url", url);
            return r;
        }
    }

    public static class Video {
        public final String title;
        public final String player;
        public final String image;

        public Video(String title, String player, String image) {
            this.title = title;
            this.player = player;
            this.image = image;
        }

        public Video(JSONObject o) {
            title = o.optString("title", null);
            image = o.optString("image", null);
            player = o.optString("player", null);
        }

        private JSONObject serialize0() throws JSONException {
            final JSONObject r = new JSONObject();
            r.put("title", title);
            r.put("image", image);
            r.put("player", player);
            return r;
        }
    }

    public static class Photo {
        public final String name;
        private final String photo;
        private final String photoBig;
        private final String photoXBig;
        private final String photoXXBig;
        private final String photoXXXBig;
        public final int width;
        public final int height;

        public Photo(String name, String photo, String photoBig, String photoXBig, String photoXXBig, String photoXXXBig, int width, int height) {
            this.name = name;
            this.photo = photo;
            this.photoBig = photoBig;
            this.photoXBig = photoXBig;
            this.photoXXBig = photoXXBig;
            this.photoXXXBig = photoXXXBig;
            this.width = width;
            this.height = height;
        }

        public Photo(JSONObject o) {
            this.name = checkEmpty(o.optString("name", null));
            this.photo = checkEmpty(o.optString("photo", null));
            this.photoBig = checkEmpty(o.optString("photoBig", null));
            this.photoXBig = checkEmpty(o.optString("photoXBig", null));
            this.photoXXBig = checkEmpty(o.optString("photoXXBig", null));
            this.photoXXXBig = checkEmpty(o.optString("photoXXXBig", null));
            this.width = o.optInt("width", 0);
            this.height = o.optInt("height", 0);
        }

        public String getBiggestPhoto() {
            if (photoXXXBig != null) return photoXXXBig;
            if (photoXXBig != null) return photoXXBig;
            if (photoXBig != null) return photoXBig;
            if (photoBig != null) return photoBig;
            return photo;
        }

        public String getThumbnailPhoto() {
            if (photoBig != null) return photoBig;
            return photo;
        }

        private JSONObject serialize0() throws JSONException {
            final JSONObject r = new JSONObject();
            r.put("name", name);
            r.put("photo", photo);
            r.put("photoBig", photoBig);
            r.put("photoXBig", photoXBig);
            r.put("photoXXBig", photoXXBig);
            r.put("photoXXXBig", photoXXXBig);
            r.put("width", width);
            r.put("height", height);
            return r;
        }
    }

    public static String loadAttachmentsInformationWithVideos(List<Attachment> attachments) throws IOException, KException, JSONException {
        if (attachments == null || attachments.size() == 0) return null;

        List<String> videos = null;
        List<Attachment> others = new ArrayList<Attachment>(attachments.size());
        for (Attachment a : attachments) {
            if ("video".equals(a.type)) {
                if (videos == null) {
                    videos = new ArrayList<String>();
                }

                videos.add(a.video.owner_id + "_" + a.video.vid);
            } else {
                others.add(a);
            }
        }

        if (videos != null) {
            final ArrayList<com.perm.kate.api.Video> vids =
                    VKMessenger.getApi().getVideo(DatabaseTools.idsToString(videos), null, "320", null, null);

            for (com.perm.kate.api.Video vid : vids) {
                final Attachment a = new Attachment();
                a.type = "video";
                a.video = vid;
                others.add(a);
            }
        }

        return loadAttachmentsInformation(others).serialize().toString();
    }

    public static Attachments loadAttachmentsInformation(List<Attachment> attachments) {
        if (attachments == null || attachments.size() == 0) return null;

        List<Photo> photos = new ArrayList<Photo>();
        List<Video> videos = new ArrayList<Video>();
        List<Audio> audios = new ArrayList<Audio>();
        List<Document> documents = new ArrayList<Document>();
        List<Forwarded> forwarded = new ArrayList<Forwarded>();
        Geo geo = null;

        //photo,posted_photo,video,audio,link,note,app,poll,doc
        for (Attachment a : attachments) {
            if ("video".equals(a.type)) {
                videos.add(new Video(a.video.title, a.video.player, a.video.image));
            } else if ("photo".equals(a.type)) {
                photos.add(new Photo(a.photo.phototext, a.photo.src, a.photo.src_big, a.photo.src_xbig, a.photo.src_xxbig, a.photo.src_xxxbig, a.photo.width, a.photo.height));
            } else if ("audio".equals(a.type)) {
                audios.add(new Audio(a.audio.artist, a.audio.title, a.audio.url));
            } else if ("geo".equals(a.type)) {
                geo = new Geo(Double.parseDouble(a.geo.lat), Double.parseDouble(a.geo.lon), a.geo.place != null ? a.geo.place.title : null);
            } else if ("fwd".equals(a.type)) {
                forwarded.add(new Forwarded(0, a.message.uid, Long.parseLong(a.message.date), a.message.body, loadAttachmentsInformation(a.message.attachments)));
            } else if ("doc".equals(a.type)) {
                documents.add(new Document(a.document.title, a.document.url));
            }
        }

        return new Attachments(photos, videos, audios, forwarded, documents, geo);
    }

    public JSONObject serialize() {
        try {
            return serialize0();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private JSONObject serialize0() throws JSONException {
        final JSONObject r = new JSONObject();

        if (photos != null) {
            final JSONArray a = new JSONArray();
            int i = 0;
            for (Photo photo : photos) {
                a.put(i, photo.serialize0());
                i++;
            }

            r.put("photos", a);
        }

        if (videos != null) {
            final JSONArray a = new JSONArray();
            int i = 0;
            for (Video video : videos) {
                a.put(i, video.serialize0());
                i++;
            }

            r.put("videos", a);
        }

        if (audios != null) {
            final JSONArray a = new JSONArray();
            int i = 0;
            for (Audio audio : audios) {
                a.put(i, audio.serialize0());
                i++;
            }

            r.put("audios", a);
        }

        if (messages != null) {
            final JSONArray a = new JSONArray();
            int i = 0;
            for (Forwarded m : messages) {
                a.put(i, m.serialize0());
                i++;
            }

            r.put("messages", a);
        }

        if (documents != null) {
            final JSONArray a = new JSONArray();
            int i = 0;
            for (Document d : documents) {
                a.put(i, d.serialize0());
                i++;
            }

            r.put("documents", a);
        }

        if (geo != null) {
            r.put("geo", geo.serialize0());
        }

        return r;
    }

    private static String checkEmpty(String s) {
        if (s == null || s.length() == 0) return null;
        return s;
    }
}
