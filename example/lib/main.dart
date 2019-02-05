import 'package:esptouch_flutter/esptouch_flutter.dart';
import 'package:flutter/material.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
  }

  void _init(WifiCred cred) async {
    EsptouchFlutter.subscribeToResults();
    EsptouchFlutter.initEspTouch(cred);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData.light(),
      home: Scaffold(
        appBar: AppBar(
          title: const Text('ESP Touch'),
        ),
        body: Container(
          child: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                Padding(
                  padding: const EdgeInsets.all(32.0),
                  child: WifiForm(
                    callback: _init,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class WifiForm extends StatefulWidget {
  final String ssid;
  final dynamic callback;

  WifiForm({this.ssid, @required this.callback});

  @override
  State<StatefulWidget> createState() => _WifiFormState();
}

class _WifiFormState extends State<WifiForm> {
  final _formKey = GlobalKey<FormState>();
  final _wifiCred = WifiCred();

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: Container(
        child: Column(
          children: <Widget>[
            TextFormField(
              decoration: InputDecoration(
                icon: Icon(
                  Icons.network_wifi,
                  size: 28.0,
                ),
              ),
              style: TextStyle(fontSize: 28.0, color: Colors.black),
              validator: (value) {
                if (value.isEmpty) {
                  return 'Please enter the SSID of the network';
                }
              },
              onSaved: (ssid) => _wifiCred.ssid = ssid,
            ),
            TextFormField(
              decoration: InputDecoration(
                  icon: Icon(
                Icons.wifi_lock,
                size: 28.0,
              )),
              style: TextStyle(fontSize: 28.0, color: Colors.black),
              validator: (value) {
                if (value.isEmpty) {
                  return 'Please enter the bssid';
                }
              },
              onSaved: (bssid) => _wifiCred.bssid = bssid,
            ),
            TextFormField(
              decoration: InputDecoration(
                icon: Icon(
                  Icons.lock,
                  size: 28.0,
                ),
              ),
              style: TextStyle(fontSize: 28.0, color: Colors.black),
              obscureText: true,
              validator: (value) {
                if (value.isEmpty || value.length < 8) {
                  return 'Please enter a valid password';
                }
              },
              onSaved: (pass) => _wifiCred.pass = pass,
            ),
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: RaisedButton(
                color: Colors.amber,
                onPressed: () {
                  if (_formKey.currentState.validate()) {
                    _formKey.currentState.save();
                    widget.callback(_wifiCred);
                    Scaffold.of(context).showSnackBar(
                      SnackBar(
                        content: Text('Connecting to ${_wifiCred.ssid}'),
                        action: SnackBarAction(
                          onPressed: () {},
                          label: 'Cancel',
                          textColor: Colors.amber,
                        ),
                        duration: Duration(seconds: 2),
                      ),
                    );
                  }
                },
                child: Padding(
                  padding: EdgeInsets.symmetric(horizontal: 20.0, vertical: 10.0),
                  child: Text('Touch', style: TextStyle(fontSize: 24.0),)
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
