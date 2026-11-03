package com.brothers.stickers;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Recibe el archivo (.zip / .txt) que WhatsApp comparte con "Exportar chat",
 * lo copia a la caché como "shared_chat.dat" y avisa a la página web.
 */
public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            handleShareIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShareIntent(intent);
    }

    @SuppressWarnings("deprecation")
    private Uri getSharedUri(Intent intent) {
        if (Build.VERSION.SDK_INT >= 33) {
            return intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
        }
        return intent.getParcelableExtra(Intent.EXTRA_STREAM);
    }

    private void handleShareIntent(final Intent intent) {
        if (intent == null) return;
        if (!Intent.ACTION_SEND.equals(intent.getAction())) return;
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return;

        final Uri uri = getSharedUri(intent);
        if (uri == null) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                try {
                    File out = new File(getCacheDir(), "shared_chat.dat");
                    if (out.exists()) out.delete();

                    InputStream in = getContentResolver().openInputStream(uri);
                    if (in == null) throw new Exception("No se pudo abrir el archivo compartido");
                    OutputStream os = new FileOutputStream(out);
                    try {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = in.read(buf)) != -1) {
                            os.write(buf, 0, n);
                        }
                        os.flush();
                        ok = true;
                    } finally {
                        try { os.close(); } catch (Exception ignored) {}
                        try { in.close(); } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (ok) notifyWeb();
            }
        }).start();
    }

    private void notifyWeb() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (getBridge() != null && getBridge().getWebView() != null) {
                        getBridge().getWebView().evaluateJavascript(
                            "window.dispatchEvent(new Event('sharedFileReady'));", null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
