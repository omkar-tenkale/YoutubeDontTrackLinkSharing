package omkartenkale.privacy.youtube.link.unshorten.expand.share;


import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

public class MainActivity extends Activity {

    String intentSubject ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent receivedIntent = getIntent();
        String receivedType = receivedIntent.getType();
        Log.e("log","intent="+receivedIntent.getAction());
        findViewById(R.id.rootlayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            finish();
            }
        });
        if(receivedIntent.getAction().equals(Intent.ACTION_SEND)) { //content is being shared }else if(receivedAction.equals(Intent.ACTION_MAIN)){ //app has been launched directly, not from share list }
            if(receivedType.startsWith("text/")) {
                String shortLink = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
                if (shortLink != null)
                { //set the text
                 //   txtView.setText(receivedText);
                    Log.e("log","received="+shortLink);
                    ((TextView)  findViewById(R.id.shortened_link)).setText(shortLink);
                    intentSubject = receivedIntent.getStringExtra(Intent.EXTRA_SUBJECT);
                    ((TextView)  findViewById(R.id.video_title)).setText(receivedIntent.getStringExtra(Intent.EXTRA_SUBJECT));

                    new UrlExpander().execute(shortLink);
                    //Second approach via Webview
                    //Both ways work
//                    final WebView webView;
//                    webView =findViewById(R.id.webview);
//                    webView.loadUrl(shortLink);
//                    webView.setWebViewClient(new WebViewClient() {
//                        public void onPageFinished(WebView view, String url) {
//                            Log.e("log","webviewurl="+webView.getUrl());
//
//                        }
//                    });
                }


            }
        }else{
            //opened from launcher
                Log.e("log","opened from launcher=");
            TextView link;
            link = (TextView) findViewById(R.id.welcome_text);
            link.setMovementMethod(LinkMovementMethod.getInstance());
            link.setText(Html.fromHtml("When sharing videos from youtube select this app\n It will retrieve the original URL which cannot be tracked back to you when someone clicks it.\n <a href=https://github.com/omkar-tenkale/YoutubeDontTrackLinkSharing >Github</a>"));
            findViewById(R.id.info_display).setVisibility(View.VISIBLE);


        }

    }


    class UrlExpander extends AsyncTask<String, Void, String> {

        private Exception exception;

        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                Log.e("log","getting url="+url.toString());


                // open connection
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);

                // stop following browser redirect
                httpURLConnection.setInstanceFollowRedirects(false);
                httpURLConnection.getRequestMethod();
                // extract location header containing the actual destination URL
                String expandedURL = httpURLConnection.getHeaderField("Location");
                Log.e("log","returning expanded url="+expandedURL+"\n"+httpURLConnection.toString());

                httpURLConnection.disconnect();

                return expandedURL;
            } catch (Exception e) {
                Log.e("log","error in network call=");

                e.printStackTrace();
                return null;
            } finally {

            }
        }

        protected void onPostExecute(String expandedUrl) {

            Log.e("log","expanded="+expandedUrl);

            if(expandedUrl!=null) {
                if(!expandedUrl.contains("youtube")){
                    findViewById(R.id.error_display).setVisibility(View.VISIBLE);
                    return;
                }
                if (expandedUrl.contains("&fea")) {
                    Log.e("log", "filtered=" + expandedUrl.substring(0, expandedUrl.indexOf("&feature")));
                    expandedUrl = expandedUrl.substring(0, expandedUrl.indexOf("&feature"));
                }
                ((TextView) findViewById(R.id.video_link)).setText(expandedUrl);
                findViewById(R.id.success_display).setVisibility(View.VISIBLE);
                final String finalExpandedUrl1 = expandedUrl;
                Button sharebtn = findViewById(R.id.share);
                sharebtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, intentSubject);
                        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, finalExpandedUrl1);
                        startActivity(sharingIntent);
                    }
                });
                sharebtn.callOnClick();
                getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                final String finalExpandedUrl = expandedUrl;
                ((Button) findViewById(R.id.copy)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Youtube video link", finalExpandedUrl);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, "Copied \n"+ finalExpandedUrl, Toast.LENGTH_SHORT).show();
                    }
                });

            }else {
                findViewById(R.id.error_display).setVisibility(View.VISIBLE);
            }
        }
    }

}
