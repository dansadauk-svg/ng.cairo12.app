package ng.cairo12.app;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;

/** All API calls use HTTPS and fail closed on a cross-host redirect. */
final class Api {
    interface Result { void done(Object value, String error); }
    final Vault vault;
    private final CatalogueCache catalogueCache;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    Api(Context context) { vault = new Vault(context); catalogueCache=new CatalogueCache(new File(context.getCacheDir(),"catalogue_cache")); }
    void request(String path, JSONObject body, Result callback) {
        worker.execute(() -> {
            try { Object value = send(path, body, null, null); main.post(() -> callback.done(value, null)); }
            catch (Exception error) { main.post(() -> callback.done(null, message(error))); }
        });
    }
    void receipt(Context context, long id, String kind, String paidAt, Uri uri, Result callback) {
        worker.execute(() -> {
            try {
                String mime = context.getContentResolver().getType(uri);
                if (mime == null || !(mime.equals("image/jpeg") || mime.equals("image/png") || mime.equals("image/webp"))) throw new IOException("Choose a JPG, PNG or WebP receipt.");
                byte[] bytes;
                try (InputStream input = context.getContentResolver().openInputStream(uri)) { bytes = read(input, 3 * 1024 * 1024); }
                JSONObject fields = new JSONObject().put("kind", kind).put("paid_at", paidAt).put("mime", mime);
                Object value = send("orders/" + id + "/receipt", fields, bytes, mime);
                main.post(() -> callback.done(value, null));
            } catch (Exception error) { main.post(() -> callback.done(null, message(error))); }
        });
    }
    private Object send(String path,JSONObject body,byte[] file,String mime)throws Exception {
        boolean force=path.endsWith("&_fresh=1");if(force)path=path.substring(0,path.length()-9);
        boolean cacheable=body==null&&file==null&&CatalogueCache.eligible(path);String key=BuildConfig.SITE_URL+path;
        CatalogueCache.Entry cached=cacheable?catalogueCache.get(key):null;long now=System.currentTimeMillis();
        if(cacheable&&!force&&cached!=null&&cached.fresh(now))return new JSONObject(cached.body.toString());
        try{Object result=sendNetwork(path,body,file,mime);if(cacheable){if(!(result instanceof JSONObject)||((JSONObject)result).optJSONArray("products")==null)throw new IOException("The catalogue response was unreadable.");catalogueCache.put(key,(JSONObject)result,now);}return result;}
        catch(Exception error){if(cacheable&&!force&&cached!=null&&cached.offline(System.currentTimeMillis())&&(!(error instanceof HttpFailure)||((HttpFailure)error).status>=500))return new JSONObject(cached.body.toString()).put("_offline",true);throw error;}
    }
    private static final class HttpFailure extends IOException {final int status;HttpFailure(int status,String message){super(message);this.status=status;}}
    private Object sendNetwork(String path, JSONObject body, byte[] file, String mime) throws Exception {
        URL url = new URL(BuildConfig.SITE_URL + "wp-json/cairo12/v1/" + path);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        connection.setRequestProperty("Cache-Control","no-cache");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(45000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Cairo12-Android/"+BuildConfig.VERSION_NAME);
        String token = vault.token();
        if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
        if (body != null) {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            if (file == null) {
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream out = connection.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            } else {
                String boundary = "C12" + UUID.randomUUID();
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                try (OutputStream out = connection.getOutputStream()) {
                    for (String field : new String[]{"kind", "paid_at"}) {
                        write(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field + "\"\r\n\r\n" + body.getString(field) + "\r\n");
                    }
                    write(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"receipt." + (mime.equals("image/jpeg")?"jpg":mime.substring(6)) + "\"\r\nContent-Type: " + mime + "\r\n\r\n");
                    out.write(file);
                    write(out, "\r\n--" + boundary + "--\r\n");
                }
            }
        }
        try {
            int status = connection.getResponseCode();
            if (status >= 300 && status < 400) throw new IOException("The test website redirected the connection. Please check its WordPress site address.");
            InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String text;
            try (InputStream stream = input) { text = new String(read(stream, 10 * 1024 * 1024), StandardCharsets.UTF_8); }
            Object value;
            try { value = new JSONTokener(text).nextValue(); }
            catch (JSONException error) { throw new IOException("The website API returned an unreadable response."); }
            if (status == 404 && path.startsWith("mobile/")) throw new IOException("App sign-in needs the latest Cairo12 plugin on this website. Please ask the administrator to install Cairo12 Core 0.2.1 or later.");
            if (status >= 400) {
                if (status == 401&&token.equals(vault.token())) vault.clear();
                String message = value instanceof JSONObject ? ((JSONObject)value).optString("message", "Request failed") : "Request failed";
                throw new HttpFailure(status,message);
            }
            if (!(value instanceof JSONObject) && !(value instanceof JSONArray)) throw new IOException("The website API returned an unexpected response.");
            return value;
        } finally { connection.disconnect(); }
    }
    static byte[] read(InputStream stream, int limit) throws IOException {
        if (stream == null) throw new IOException("No response received. Try again.");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = stream.read(buffer)) != -1) {
            if (output.size() + count > limit) throw new IOException("File or response is too large.");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
    private static void write(OutputStream out, String value) throws IOException { out.write(value.getBytes(StandardCharsets.UTF_8)); }
    private static String message(Exception e) {
        if(e instanceof UnknownHostException) return "We can’t connect to Cairo12 right now. Check your internet connection and try again shortly.";
        if(e instanceof SocketTimeoutException) return "Cairo12 is taking a little longer to respond. Please try again.";
        if(e instanceof ConnectException || e instanceof javax.net.ssl.SSLException) return "We couldn’t connect securely to Cairo12. Please try again shortly.";
        return e.getMessage() == null ? "Connection failed. Please retry." : e.getMessage(); }
    void close() { worker.shutdown(); }
}
