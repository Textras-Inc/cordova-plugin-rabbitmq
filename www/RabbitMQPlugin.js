var exec = require('cordova/exec');
var cordova = require('cordova');
var RabbitMQPlugin = function() {};

RabbitMQPlugin.prototype.isPlatformIOS = function() {
  return (
    device.platform === "iPhone" ||
    device.platform === "iPad" ||
    device.platform === "iPod touch" ||
    device.platform === "iOS"
  );
};

RabbitMQPlugin.prototype.errorCallback = function(msg) {
  console.log("RabbitMQPlugin Callback Error: " + msg);
};

RabbitMQPlugin.prototype.callNative = function(
  name,
  args,
  successCallback,
  errorCallback
) {
  if (errorCallback) {
    exec(successCallback, errorCallback, "RabbitMQPlugin", name, args);
  } else {
    exec(
      successCallback,
      this.errorCallback,
      "RabbitMQPlugin",
      name,
      args
    );
  }
};

RabbitMQPlugin.prototype.init = function (
    queueName,
    host,
    port,
    user,
    passwd,
    successCallback,
    errorCallback
) {
    if (this.isPlatformIOS()) {
        console.log('RabbitMQPlugin.prototype.initial');
        this.callNative(
            'initial',
            [queueName, host, port, user, passwd],
            successCallback,
            errorCallback
        );
    } else {
        console.log('RabbitMQPlugin.prototype.init');
        this.callNative(
            'init',
            [queueName, host, port, user, passwd],
            successCallback,
            errorCallback
        );
    }
};

// Common methods
RabbitMQPlugin.prototype.coolMethod = function() {
  if (this.isPlatformIOS()) {
    this.callNative("coolMethod", [], null);
  } else {
    this.callNative("coolMethod", [], null);
  }
};

// 向Webview发送接收到的数据
RabbitMQPlugin.prototype.receivedMessageInAndroidCallback = function(data) {
  console.log('=============== received rmq message:', data);
  if (device.platform === "Android") {
    data = JSON.stringify(data);
    var event = JSON.parse(data);
    cordova.fireDocumentEvent("rabbitmq.receivedMessage", event);
  } else {
    data = JSON.stringify(data);
    var event = JSON.parse(data);
    cordova.fireDocumentEvent("rabbitmq.receivedMessage", event);
  }
};

if (!window.plugins) {
  window.plugins = {};
}

if (!window.plugins.RabbitMQPlugin) {
  window.plugins.RabbitMQPlugin = new RabbitMQPlugin();
}

module.exports = new RabbitMQPlugin();
