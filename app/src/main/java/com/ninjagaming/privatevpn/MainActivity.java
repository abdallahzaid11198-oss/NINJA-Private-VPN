package com.ninjagaming.privatevpn;
import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;

public class MainActivity extends Activity {
  static final int PICK=8, API=9, SYSTEM=10, BG=0xff0c111b, PANEL=0xff182232, GOLD=0xfffac342, MUTED=0xffa4b1c4, GREEN=0xff49d89e;
  TextView status, hint, fileLabel, engine, ping;
  Button connect, disconnect;
  IOpenVPNAPIService vpn;
  String config;
  boolean bound, approved, connected, pending;
  final IOpenVPNStatusCallback cb=new IOpenVPNStatusCallback.Stub(){
    public void newStatus(String uuid,String state,String message,String level){
      runOnUiThread(()->{
        connected="CONNECTED".equalsIgnoreCase(state);
        int color=connected?GREEN:MUTED;
        if(connected)status.setText("متصل فعليًا ✓");
        else if(Arrays.asList("CONNECTING","WAIT","AUTH","GET_CONFIG","ASSIGN_IP","RECONNECTING").contains(state)) {
          status.setText("جاري الاتصال..."); color=GOLD;
        } else status.setText("الحالة: "+state);
        status.setTextColor(color);
        hint.setText(message==null?"":message);
        buttons();
      });
    }
  };
  final ServiceConnection service=new ServiceConnection(){
    public void onServiceConnected(ComponentName name,IBinder binder){
      vpn=IOpenVPNAPIService.Stub.asInterface(binder);
      engine.setText("OpenVPN: متاح، جارٍ طلب الإذن");
      try{
        Intent permission=vpn.prepare(getPackageName());
        if(permission==null)ready(); else startActivityForResult(permission,API);
      }catch(Exception e){engine.setText("OpenVPN: فشل طلب الإذن");}
      buttons();
    }
    public void onServiceDisconnected(ComponentName name){
      vpn=null;approved=false;connected=false;
      engine.setText("OpenVPN: غير متصل بالتطبيق");buttons();
    }
  };
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().setStatusBarColor(BG);
    getWindow().setNavigationBarColor(BG);
    draw();load();
  }
  @Override public void onStart(){
    super.onStart();
    if(!bound){
      Intent i=new Intent("de.blinkt.openvpn.api.IOpenVPNAPIService");
      i.setPackage("de.blinkt.openvpn");
      try{bound=bindService(i,service,BIND_AUTO_CREATE);}catch(Exception ignored){bound=false;}
      if(!bound)engine.setText("نزّل OpenVPN for Android (Arne Schwabe) الأول");
    }
    buttons();
  }
  @Override public void onDestroy(){
    if(vpn!=null&&approved)try{vpn.unregisterStatusCallback(cb);}catch(Exception ignored){}
    if(bound)try{unbindService(service);}catch(Exception ignored){}
    super.onDestroy();
  }
  void ready(){
    if(vpn==null)return;
    try{vpn.registerStatusCallback(cb);approved=true;engine.setText("محرك OpenVPN جاهز");}
    catch(Exception e){engine.setText("تعذّر تسجيل حالة المحرك");}
    buttons();
    if(pending){pending=false;connectVpn();}
  }
  void connectVpn(){
    if(config==null){msg("استورد ملف .ovpn أولًا");return;}
    if(vpn==null){msg("نزّل تطبيق OpenVPN for Android");return;}
    try{
      if(!approved){pending=true;Intent i=vpn.prepare(getPackageName());if(i!=null)startActivityForResult(i,API);else ready();return;}
      Intent i=vpn.prepareVPNService();
      if(i!=null){startActivityForResult(i,SYSTEM);return;}
      startVpn();
    }catch(Exception e){msg("تعذر طلب الاتصال: "+e.getClass().getSimpleName());}
  }
  void startVpn(){
    if(vpn==null||config==null)return;
    try{vpn.startVPN(config);status.setText("تم إرسال طلب الاتصال...");status.setTextColor(GOLD);}
    catch(Exception e){msg("فشل بدء الاتصال");}
  }
  void disconnectVpn(){
    if(vpn==null)return;
    try{vpn.disconnect();status.setText("تم إرسال طلب الفصل...");status.setTextColor(GOLD);}
    catch(Exception e){msg("فشل الفصل");}
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request==PICK&&result==RESULT_OK&&data!=null) {
      try{importFile(data.getData());}catch(Exception e){msg("ملف غير صالح: "+e.getMessage());}
    }else if(request==API) {
      if(result==RESULT_OK)ready();else{pending=false;engine.setText("اترفض إذن OpenVPN");}
    }else if(request==SYSTEM) {
      if(result==RESULT_OK)startVpn();else msg("اترفض إذن تشغيل VPN");
    }
  }
  void choose(){
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");
    startActivityForResult(i,PICK);
  }
  void importFile(Uri u)throws Exception{
    if(u==null)throw new IOException("Missing file");
    ByteArrayOutputStream out=new ByteArrayOutputStream();
    try(InputStream in=getContentResolver().openInputStream(u)){
      if(in==null)throw new IOException("Unreadable");
      byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){
        if(out.size()+n>262144)throw new IOException("Too large");
        out.write(b,0,n);
      }
    }
    String c=out.toString(StandardCharsets.UTF_8.name());
    if(!c.matches("(?s).*\\bclient\\b.*")||!c.matches("(?s).*\\bremote\\s+\\S+.*")||!c.contains("<ca>"))
      throw new IOException("لازم ملف client .ovpn فيه remote و ca مدمجة");
    try(FileOutputStream f=openFileOutput("client.ovpn",MODE_PRIVATE)){f.write(c.getBytes(StandardCharsets.UTF_8));}
    config=c;fileLabel.setText("السيرفر: "+remote(c));buttons();msg("تم استيراد الملف");
  }
  String remote(String c){
    for(String l:c.split("\\R"))if(l.trim().startsWith("remote "))return l.trim().substring(7).replaceAll("\\s+",":");
    return "unknown";
  }
  void load(){
    try(FileInputStream f=openFileInput("client.ovpn")){
      ByteArrayOutputStream out=new ByteArrayOutputStream();
      byte[] b=new byte[4096];int n;while((n=f.read(b))!=-1)out.write(b,0,n);
      config=out.toString(StandardCharsets.UTF_8.name());fileLabel.setText("السيرفر: "+remote(config));
    }catch(Exception ignored){}
    buttons();
  }
  void measure(){
    ping.setText("جاري اختبار الشبكة...");
    new Thread(()->{
      List<Double> vals=new ArrayList<>();
      for(int x=0;x<5;x++){
        try(Socket s=new Socket()){
          long t=System.nanoTime();s.connect(new InetSocketAddress("1.1.1.1",443),1500);
          vals.add((System.nanoTime()-t)/1000000.0);
        }catch(Exception ignored){}
      }
      String result="تعذر قياس الاتصال";
      if(vals.size()>1){
        double avg=0,jit=0;
        for(double n:vals)avg+=n;
        for(int j=1;j<vals.size();j++)jit+=Math.abs(vals.get(j)-vals.get(j-1));
        result=String.format(Locale.US,"TCP: %.0f ms   Jitter: %.0f ms",avg/vals.size(),jit/(vals.size()-1));
      }
      final String output=result;runOnUiThread(()->ping.setText(output));
    }).start();
  }
  void buttons(){
    if(connect!=null)connect.setEnabled(vpn!=null&&config!=null&&!connected);
    if(disconnect!=null)disconnect.setEnabled(vpn!=null);
  }
  void msg(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
  int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
  TextView text(String s,int sp,int color){
    TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(sp);
    t.setPadding(0,dp(5),0,dp(5));return t;
  }
  Button button(String s,int color){
    Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(color==GOLD?BG:Color.WHITE);
    b.setTextSize(15);b.setTypeface(null,Typeface.BOLD);
    GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));
    b.setBackground(d);b.setMinHeight(dp(52));return b;
  }
  LinearLayout panel(){
    LinearLayout p=new LinearLayout(this);p.setOrientation(1);p.setPadding(dp(19),dp(16),dp(19),dp(16));
    GradientDrawable d=new GradientDrawable();d.setColor(PANEL);d.setCornerRadius(dp(18));p.setBackground(d);return p;
  }
  void add(LinearLayout p,View v,int gap){
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(gap);p.addView(v,lp);
  }
  void draw(){
    ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(BG);
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);
    root.setPadding(dp(22),dp(28),dp(22),dp(28));scroll.addView(root);setContentView(scroll);
    add(root,text("NINJA // PRIVATE NETWORK",12,GOLD),0);
    TextView head=text("NINJA VPN",38,Color.WHITE);head.setTypeface(null,Typeface.BOLD);add(root,head,0);
    add(root,text("eFootball connection controller",14,MUTED),0);
    LinearLayout p=panel();add(root,p,22);
    add(p,text("LIVE VPN STATUS",12,GOLD),0);
    status=text("غير متصل",24,Color.WHITE);status.setTypeface(null,Typeface.BOLD);add(p,status,10);
    hint=text("البرنامج محتاج سيرفر VPN فعلي",13,MUTED);add(p,hint,6);
    connect=button("اتصال بالسيرفر",GOLD);add(root,connect,16);connect.setOnClickListener(v->connectVpn());
    disconnect=button("فصل الاتصال",PANEL);add(root,disconnect,9);disconnect.setOnClickListener(v->disconnectVpn());
    p=panel();add(root,p,20);
    add(p,text("SERVER CONFIGURATION",12,GOLD),0);
    fileLabel=text("مفيش ملف VPN مستورد",14,Color.WHITE);add(p,fileLabel,9);
    Button imp=button("استيراد ملف .ovpn",0xff384c68);add(p,imp,10);imp.setOnClickListener(v->choose());
    engine=text("جاري البحث عن OpenVPN...",12,MUTED);add(p,engine,9);
    p=panel();add(root,p,15);
    add(p,text("NETWORK QUALITY",12,GOLD),0);
    ping=text("TCP: —    Jitter: —",18,Color.WHITE);add(p,ping,12);
    add(p,text("القياس لـ Cloudflare 1.1.1.1 فقط، مش لسيرفر اللعبة",12,MUTED),2);
    Button probe=button("اختبار جودة الشبكة",0xff384c68);add(p,probe,10);probe.setOnClickListener(v->measure());
    add(root,text("يحتاج OpenVPN for Android وسيرفر خاص. مش مضمون يقلل تأخير eFootball أو يحدد خصومك.",13,MUTED),18);
    buttons();
  }
}
