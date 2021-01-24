
import 'dart:async';

import 'package:flutter/services.dart';

class GetContacts {
  static const MethodChannel _channel =
      const MethodChannel('get_contacts');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
