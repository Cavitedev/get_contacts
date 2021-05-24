import 'dart:async';

import 'package:flutter/services.dart';

class GetContacts {
  static const MethodChannel _channel =
      const MethodChannel("com.cavitedev.get_contacts");

  static Future<String> get contactsJsonString async {
    String contactsJson;

    contactsJson = await _channel.invokeMethod('getContacts');

    return contactsJson;
  }
}
