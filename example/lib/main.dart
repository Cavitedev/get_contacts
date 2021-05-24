import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get_contacts/get_contacts.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _contacts = 'Unknown';

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String contacts;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      contacts = await GetContacts.contactsJsonString;
    } on PlatformException catch (e) {
      contacts = 'Error code is ' + e.code;
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;
    final Map<String, dynamic> jsonMap =
    json.decode(contacts) as Map<String, dynamic>;

    List<dynamic> _contactsList = jsonMap["contacts"] as List<dynamic>;

    int times =
        _contactsList.where((e) => (e as Map<String, dynamic>)["name"] == "Ana 3C").length;
    setState(() {
      _contacts = times.toString();
    });
  }

  @override
  Widget build(BuildContext context) {



    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: SingleChildScrollView(
          child: Center(
            child: Text('Contains Ana 3C: $_contacts\n'),
          ),
        ),
      ),
      theme: ThemeData.dark(),
    );
  }
}
