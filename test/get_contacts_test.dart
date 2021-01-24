import 'package:flutter_test/flutter_test.dart';
import 'package:get_contacts/get_contacts.dart';

void main() {

  TestWidgetsFlutterBinding.ensureInitialized();



  test('getPlatformVersion', () async {
    expect(await GetContacts.contactsJsonString, '42');
  });
}
