package com.xiaoji.android.rabbitmq;

import android.app.Service;
import android.content.Intent;
import android.content.ComponentName;
import android.os.Binder;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;

import com.rabbitmq.client.*;
import org.json.JSONException;
import org.json.JSONObject;
import cn.jiguang.api.JCoreInterface;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 *
 * RabbitMQ AMQP协议客户端, 注册成Android服务
 * 在App运行期间持续接收阿里云数据中心的消息推送
 * 替代原有的Websocket形式的消息接收
 *
 **/
public class RabbitMQClientService extends Service {
    public String queue = "";
    public String routingkey = "";

    public String host = "localhost";
    public Integer port = 5672;
    public String user = "guest";
    public String passwd = "guest";

    public RabbitMQClientService() {
        Log.i("RabbitMQPlugin", "RabbitMQ, setting thread policy");
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("RabbitMQPlugin", "created service");
        JCoreInterface.asyncExecute(new MQ());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("RabbitMQPlugin", "Kathy onStartCommand - startId = " + startId + ", Thread ID = " + Thread.currentThread().getId());
        //return super.onStartCommand(intent, START_STICKY, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        Log.i("RabbitMQPlugin", "destroyed service");
        stopForeground(true);
        Intent intent = new Intent("com.xiaoji.rabbitmq.SERVICE_DESTROY");
        sendBroadcast(intent);
        super.onDestroy();
    }

    private MyBinder binder = new MyBinder();

    @Override
    public IBinder onBind(Intent intent) {
      String queueName = intent.getStringExtra("queueName");

      String host = intent.getStringExtra("host");
      String port = intent.getStringExtra("port");
      String user = intent.getStringExtra("user");
      String passwd = intent.getStringExtra("passwd");

      RabbitMQClientService.this.queue = queueName;

      RabbitMQClientService.this.host = host;
      RabbitMQClientService.this.port = Integer.valueOf(port);
      RabbitMQClientService.this.user = user;
      RabbitMQClientService.this.passwd = passwd;

      return binder;
    }

    public class MyBinder extends Binder {
      public Service getService() {
        return RabbitMQClientService.this;
      }
    }

    class MQ implements Runnable {
        private Connection conn = null;
        private Channel channel = null;

        public void run() {
            try {
                if (android.os.Build.VERSION.SDK_INT > 9) {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                }

                connect();
            } catch (Exception e) {
              Log.e("RabbitMQPlugin", "RabbitMQ connect error", e.getCause());
            }
        }

        private void connect() {
          if (this.conn != null) {
              if (this.conn.isOpen()) {
                Log.i("RabbitMQPlugin", "connection is already open, exiting");
                  return;
              } else {
                Log.i("RabbitMQPlugin", "connection is not open, continuing");
              }
          } else {
            Log.i("RabbitMQPlugin", "inside connect, no connection");
          }

          ConnectionFactory factory = new ConnectionFactory();
          try {
              factory.setHost(host);
              factory.setPort(port);
              factory.setUsername(user);
              factory.setPassword(passwd);
              factory.setVirtualHost("/");
              factory.setAutomaticRecoveryEnabled(true);
              factory.setShutdownTimeout(0);
              factory.setRequestedHeartbeat(5000);
              factory.setHandshakeTimeout(5000);
              factory.setNetworkRecoveryInterval(5000);

              this.conn = factory.newConnection();

              this.channel = this.conn.createChannel();

              Map<String, Object> args = new HashMap<String, Object>();
              args.put("x-message-ttl", 1000 * 60 * 60 * 24);	// 存放24小时

            // NOTE: we shouldn't need to declare these here
            //   this.channel.exchangeDeclare(exchange, "direct", true, false, null);
            //   this.channel.exchangeDeclare(announceexchange, "fanout", true, false, null);
            //   this.channel.queueDeclare(queue, true, false, false, null);
            //   this.channel.queueBind(queue, announceexchange, routingkey);
            //   this.channel.queueBind(queue, exchange, routingkeyAccount);
            //   this.channel.queueBind(queue, exchange, routingkeyDevice);

              this.channel.basicConsume(queue, false, new DefaultConsumer(channel) {

                  @Override
                  public void handleDelivery(String consumerTag,
                                             Envelope envelope,
                                             AMQP.BasicProperties properties,
                                             byte[] body)
                          throws IOException {
                      JSONObject payload = null;
                      long deliveryTag = envelope.getDeliveryTag();

                      try {
                          Log.i("RabbitMQPlugin", new String(body, "utf-8"));
                          Intent sendIntent = new Intent("com.xiaoji.cordova.plugin.rabbitmq.MESSAGE_RECEIVED");
                          // Android 8 later
                          //sendIntent.setComponent(new ComponentName("com.xiaoji.cordova.plugin.rabbitmq", "com.xiaoji.cordova.plugin.rabbitmq.RabbitMQReceiver"));
                          sendIntent.putExtra("body", new String(body, "utf-8"));
                          //sendIntent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
                          RabbitMQClientService.this.sendBroadcast(sendIntent);
                          Log.i("RabbitMQPlugin", "Broadcast Sent for consumed message.");

                          channel.basicAck(deliveryTag, false);
                          Log.i("RabbitMQPlugin", "Ack committed.");
                      } catch (Exception e) {
                          Log.e("RabbitMQPlugin", "JSON format error", e.getCause());
                      }
                  }
                });
            } catch (Exception e) {
              Log.e("RabbitMQPlugin", "RabbitMQ connection error", e.getCause());
            }
        }
    }
}
