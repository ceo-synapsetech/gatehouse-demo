import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';

class DefaultFirebaseOptions {
  static FirebaseOptions get currentPlatform {
    if (kIsWeb || defaultTargetPlatform != TargetPlatform.android) {
      throw UnsupportedError(
        'Firebase is currently configured for the Gatehouse Android app.',
      );
    }

    return const FirebaseOptions(
      apiKey: 'AIzaSyCi5sfrdgjTc6aowfQDh2cAAEFNVBzJ4js',
      appId: '1:263748072043:android:97f93c9a98859007f4772e',
      messagingSenderId: '263748072043',
      projectId: 'gatehouse-res',
      storageBucket: 'gatehouse-res.firebasestorage.app',
    );
  }
}

