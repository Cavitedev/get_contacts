package com.cavitedev.get_contacts.contacts


import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.cavitedev.get_contacts.core.StringManipulator.toJoinedPhoneString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object ContactsService {




    suspend fun fetchContacts(context: Context): List<Contact> {
        val contentRes: ContentResolver = context.contentResolver
        var contacts: List<Contact>
        coroutineScope {
            val contactsListAsync = async { getPhoneContacts(contentRes) }
            val contactNumbersAsync = async { getContactNumbers(contentRes) }
            val contactEmailAsync = async { getContactEmails(contentRes) }
            val contactCompanyAsync = async { getContactCompanies(contentRes) }

            contacts = contactsListAsync.await()
            val contactNumbers = contactNumbersAsync.await()
            val contactEmails = contactEmailAsync.await()
            val contactCompanies = contactCompanyAsync.await()

            contacts.forEach {
                contactNumbers[it.id]?.let { numbers ->
                    it.numbers = numbers
                }
                contactEmails[it.id]?.let { emails ->
                    it.emails = emails
                }
                contactCompanies[it.id]?.let { companies ->
                    it.companies = companies
                }

            }
        }
        return contacts
    }

    private fun getPhoneContacts(contentRes: ContentResolver): List<Contact> {
        val contactsList = ArrayList<Contact>()
        val contactsCursor = contentRes.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID,ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                null,
                null,
                null)
        if (contactsCursor != null && contactsCursor.count > 0) {

            val idIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            while (contactsCursor.moveToNext()) {
                val id = contactsCursor.getString(idIndex)
                val name = contactsCursor.getString(nameIndex)
                val contact = Contact(id, name)
                contactsList.add(contact)

            }
            contactsCursor.close()
        }
        return contactsList
    }

    private fun getContactNumbers(contentRes: ContentResolver): Map<String, List<String>> {
        val contactsNumberMap = HashMap<String, ArrayList<String>>()
        val phoneCursor: Cursor? = contentRes.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
        )
        if (phoneCursor != null && phoneCursor.count > 0) {
            val contactIdIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (phoneCursor.moveToNext()) {
                val contactId = phoneCursor.getString(contactIdIndex)
                val number: String = toJoinedPhoneString(phoneCursor.getString(numberIndex))
                //check if the map contains key or not, if not then create a new array list with number
                if (contactsNumberMap.containsKey(contactId)) {
                    if (!contactsNumberMap[contactId]!!.contains(number)) {
                        contactsNumberMap[contactId]!!.add(number)
                    }
                } else {
                    contactsNumberMap[contactId] = arrayListOf(number)
                }
            }
            //contact contains all the number of a particular contact
            phoneCursor.close()
        }
        return contactsNumberMap
    }

    private fun getContactEmails(contentRes: ContentResolver): Map<String, List<String>> {
        val contactsEmailMap = HashMap<String, ArrayList<String>>()
        val emailCursor = contentRes.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.CONTACT_ID, ContactsContract.CommonDataKinds.Email.ADDRESS),
                null,
                null,
                null)
        if (emailCursor != null && emailCursor.count > 0) {
            val contactIdIndex = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val emailIndex = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (emailCursor.moveToNext()) {
                val contactId = emailCursor.getString(contactIdIndex)
                val email = emailCursor.getString(emailIndex)
                //check if the map contains key or not, if not then create a new array list with email
                if (contactsEmailMap.containsKey(contactId)) {
                    contactsEmailMap[contactId]?.add(email)
                } else {
                    contactsEmailMap[contactId] = arrayListOf(email)
                }
            }
            //contact contains all the emails of a particular contact
            emailCursor.close()
        }
        return contactsEmailMap
    }

    private fun getContactCompanies(contentRes: ContentResolver): Map<String, List<Company>> {
        val contactsCompanyMap = HashMap<String, ArrayList<Company>>()
        val where = ContactsContract.Data.MIMETYPE + " = ?"
        val params = arrayOf(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
        val companyCursor = contentRes.query(ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Organization.CONTACT_ID, ContactsContract.CommonDataKinds.Organization.COMPANY,
                        ContactsContract.CommonDataKinds.Organization.TITLE),
                where,
                params,
                null)

        if (companyCursor != null && companyCursor.count > 0) {
            val contactIdIndex = companyCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.CONTACT_ID)
            val companyIndex = companyCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
            val titleIndex = companyCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)
            while (companyCursor.moveToNext()) {
                val contactId = companyCursor.getString(contactIdIndex)
                val companyName = companyCursor.getString(companyIndex)
                val companyTitle = companyCursor.getString(titleIndex)
                //check if the map contains key or not, if not then create a new array list with email
                val company = Company(companyName, companyTitle)
                if (contactsCompanyMap.containsKey(contactId)) {
                    contactsCompanyMap[contactId]?.add(company)
                } else {
                    contactsCompanyMap[contactId] = arrayListOf(company)
                }
            }
            //contact contains all the emails of a particular contact
            companyCursor.close()
        }
        return contactsCompanyMap
    }

}